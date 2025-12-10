package com.github.ars_zero.common.block;

import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.github.ars_zero.registry.ModBlockEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import com.hollingsworth.arsnouveau.common.block.tile.BasicSpellTurretTile;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class MultiphaseSpellTurretTile extends BasicSpellTurretTile {

    private static final String BEGIN_TAG = "begin_spell";
    private static final String TICK_TAG = "tick_spell";
    private static final String END_TAG = "end_spell";
    private static final int HISTORY_LIMIT = 32;

    private Spell beginSpell = new Spell();
    private Spell tickSpell = new Spell();
    private Spell endSpell = new Spell();

    private boolean wasPowered;
    private boolean casting;
    private int tickCooldown;
    private int tickIntervalCounter;
    private int tickDelayOffset = 0;

    private MultiPhaseCastContext castContext;
    private UUID ownerUUID;

    private final Deque<PhaseExecution> phaseHistory = new ArrayDeque<>();

    private enum AnimationState {
        IDLE,
        BEGIN,
        TICK,
        END
    }

    private AnimationState currentAnimationState = AnimationState.IDLE;
    private AnimationController<MultiphaseSpellTurretTile> animationController;
    private long animationStartTime = 0;
    
    private static final double BEGIN_ANIMATION_DURATION = 0.16;
    private static final double END_ANIMATION_DURATION = 0.28;

    public MultiphaseSpellTurretTile(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIPHASE_SPELL_TURRET.get(), pos, state);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean powered = hasRedstoneSignal();
        if (powered && !wasPowered) {
            if (!casting) {
                startCasting();
            }
        } else if (powered && wasPowered) {
            if (casting) {
                handleTickCasting();
            }
        } else if (!powered && wasPowered) {
            if (casting) {
                finishCasting();
            }
        }
        wasPowered = powered;
    }

    public void configureSpells(Spell begin, Spell tick, Spell end, UUID owner, int tickDelayOffset) {
        beginSpell = sanitizeSpell(begin);
        tickSpell = sanitizeSpell(tick);
        endSpell = sanitizeSpell(end);
        casting = false;
        wasPowered = false;
        tickIntervalCounter = 0;
        tickCooldown = calculateTickCooldown(tickSpell);
        phaseHistory.clear();
        clearCastContext();
        ownerUUID = owner;
        setPlayer(owner);
        this.tickDelayOffset = tickDelayOffset;
        currentAnimationState = AnimationState.IDLE;
        animationStartTime = 0;
        setChanged();
        updateBlock();
    }

    public List<PhaseExecution> getPhaseHistory() {
        return List.copyOf(phaseHistory);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        saveSpell(tag, BEGIN_TAG, beginSpell);
        saveSpell(tag, TICK_TAG, tickSpell);
        saveSpell(tag, END_TAG, endSpell);
        tag.putBoolean("was_powered", wasPowered);
        if (ownerUUID != null) {
            tag.putUUID("owner_uuid", ownerUUID);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        beginSpell = loadSpell(tag, BEGIN_TAG);
        tickSpell = loadSpell(tag, TICK_TAG);
        endSpell = loadSpell(tag, END_TAG);
        wasPowered = tag.getBoolean("was_powered");
        if (tag.contains("owner_uuid")) {
            ownerUUID = tag.getUUID("owner_uuid");
        } else {
            ownerUUID = null;
        }
        casting = false;
        tickIntervalCounter = 0;
        tickCooldown = calculateTickCooldown(tickSpell);
        phaseHistory.clear();
        clearCastContext();
    }

    private void startCasting() {
        casting = true;
        tickIntervalCounter = 0;
        tickCooldown = calculateTickCooldown(tickSpell);
        initializeCastContext();
        castPhase(SpellPhase.BEGIN);
    }

    private void handleTickCasting() {
        if (tickSpell == null || tickSpell.isEmpty()) {
            return;
        }
        if (tickIntervalCounter > 0) {
            tickIntervalCounter--;
            return;
        }
        castPhase(SpellPhase.TICK);
        tickIntervalCounter = tickCooldown;
    }

    private void finishCasting() {
        casting = false;
        tickIntervalCounter = 0;
        castPhase(SpellPhase.END);
        clearCastContext();
    }

    private void castPhase(SpellPhase phase) {
        Spell spell = switch (phase) {
            case BEGIN -> beginSpell;
            case TICK -> tickSpell;
            case END -> endSpell;
        };
        if (spell == null || spell.isEmpty()) {
            return;
        }
        spellCaster = (SpellCaster) spellCaster.setSpell(spell, 0);
        
        if (castContext != null && phase == SpellPhase.BEGIN) {
            castContext.currentPhase = SpellPhase.BEGIN;
            castContext.isCasting = true;
        }
        
        super.shootSpell();
        recordPhase(phase, spell);
    }

    @Override
    public void shootSpell() {
        if (level == null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            super.shootSpell();
            return;
        }
        
        if (spellCaster.getSpell().isEmpty()) {
            return;
        }
        
        BlockPos pos = this.getBlockPos();
        int manaCost = getManaCost();
        if (manaCost > 0 && com.hollingsworth.arsnouveau.api.util.SourceUtil.takeSourceMultipleWithParticles(pos, serverLevel, 10, manaCost) == null) {
            return;
        }
        
        SpellPhase currentPhase = castContext != null ? castContext.currentPhase : SpellPhase.BEGIN;
        int animationArg = switch (currentPhase) {
            case BEGIN -> 0;
            case TICK -> 1;
            case END -> 2;
        };
        com.hollingsworth.arsnouveau.common.network.Networking.sendToNearbyClient(serverLevel, pos, new com.hollingsworth.arsnouveau.common.network.PacketOneShotAnimation(pos, animationArg));
        net.minecraft.core.Position iposition = com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.getDispensePosition(pos, serverLevel.getBlockState(pos).getValue(com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.FACING));
        net.minecraft.core.Direction direction = serverLevel.getBlockState(pos).getValue(com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.FACING);
        
        net.neoforged.neoforge.common.util.FakePlayer fakePlayer = ownerUUID != null
                ? net.neoforged.neoforge.common.util.FakePlayerFactory.get(serverLevel, new com.mojang.authlib.GameProfile(ownerUUID, ""))
                : com.hollingsworth.arsnouveau.api.ANFakePlayer.getPlayer(serverLevel);
        fakePlayer.setPos(pos.getX(), pos.getY(), pos.getZ());
        SpellPhase spellPhase = switch (currentPhase) {
            case BEGIN -> SpellPhase.BEGIN;
            case TICK -> SpellPhase.TICK;
            case END -> SpellPhase.END;
        };
        
        SpellContext spellContext = new SpellContext(serverLevel, spellCaster.getSpell(), fakePlayer, new TileCaster(this, SpellContext.CasterType.TURRET));
        com.hollingsworth.arsnouveau.api.spell.SpellResolver resolver = new com.hollingsworth.arsnouveau.api.spell.EntitySpellResolver(spellContext);
        
        if (castContext != null && ownerUUID != null) {
            resolver = new WrappedSpellResolver((com.hollingsworth.arsnouveau.api.spell.EntitySpellResolver) resolver, ownerUUID, spellPhase, true);
        }
        
        if (resolver.castType != null && com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.TURRET_BEHAVIOR_MAP.containsKey(resolver.castType)) {
            com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.TURRET_BEHAVIOR_MAP.get(resolver.castType).onCast(resolver, serverLevel, pos, fakePlayer, iposition, direction);
        }
        
        if (castContext != null) {
            updateCastContextPhase(currentPhase);
        }
    }

    private void initializeCastContext() {
        if (ownerUUID == null) {
            return;
        }
        castContext = new MultiPhaseCastContext(ownerUUID, MultiPhaseCastContext.CastSource.TURRET);
        castContext.currentPhase = SpellPhase.BEGIN;
        castContext.isCasting = true;
        castContext.beginResults.clear();
        castContext.tickResults.clear();
        castContext.endResults.clear();
        castContext.createdAt = System.currentTimeMillis();
    }

    private void updateCastContextPhase(SpellPhase phase) {
        if (castContext == null) {
            return;
        }
        castContext.currentPhase = phase;
        if (phase == SpellPhase.TICK) {
            castContext.tickCount++;
            castContext.sequenceTick++;
        }
    }

    private void clearCastContext() {
        castContext = null;
    }

    public MultiPhaseCastContext getCastContext() {
        return castContext;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    public void getTooltip(List<Component> tooltip) {
        if (!beginSpell.isEmpty()) {
            tooltip.add(Component.literal("Begin: " + beginSpell.getDisplayString()));
        }
        if (!tickSpell.isEmpty()) {
            tooltip.add(Component.literal("Tick: " + tickSpell.getDisplayString()));
        }
        if (!endSpell.isEmpty()) {
            tooltip.add(Component.literal("End: " + endSpell.getDisplayString()));
        }
    }

    private void recordPhase(SpellPhase phase, Spell spell) {
        if (phaseHistory.size() >= HISTORY_LIMIT) {
            phaseHistory.removeFirst();
        }
        long gameTime = level != null ? level.getGameTime() : 0L;
        String name = spell.name();
        if (name == null || name.isBlank()) {
            name = phase.name();
        }
        phaseHistory.addLast(new PhaseExecution(phase, name, gameTime));
    }

    private boolean hasRedstoneSignal() {
        if (level == null) {
            return false;
        }
        BlockPos above = worldPosition.above();
        return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(above);
    }

    private static Spell sanitizeSpell(Spell spell) {
        if (spell == null || spell.isEmpty()) {
            return new Spell();
        }
        String json = spell.toJson();
        return json.isEmpty() ? new Spell() : Spell.fromJson(json);
    }

    private static void saveSpell(CompoundTag tag, String key, Spell spell) {
        if (spell == null || spell.isEmpty()) {
            tag.remove(key);
            return;
        }
        Tag data = ANCodecs.encode(Spell.CODEC.codec(), spell);
        if (data != null) {
            tag.put(key, data);
        }
    }

    private static Spell loadSpell(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return new Spell();
        }
        Tag data = tag.get(key);
        Spell loaded = (Spell) ANCodecs.decode(Spell.CODEC.codec(), data);
        return loaded == null ? new Spell() : loaded;
    }

    private static int calculateTickCooldown(Spell spell) {
        if (spell == null || spell.isEmpty()) {
            return 0;
        }
        List<AbstractSpellPart> recipe = new ArrayList<>();
        for (AbstractSpellPart part : spell.recipe()) {
            recipe.add(part);
        }
        if (recipe.isEmpty()) {
            return 0;
        }
        int totalCooldown = 0;
        int index = recipe.size() - 1;
        ResourceLocation delay = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_delay");
        ResourceLocation extend = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_extend_time");
        ResourceLocation durationDown = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_duration_down");
        while (index >= 0) {
            AbstractSpellPart part = recipe.get(index);
            ResourceLocation id = part.getRegistryName();
            if (delay.equals(id)) {
                int extendCount = 0;
                int durationDownCount = 0;
                int j = index + 1;
                while (j < recipe.size()) {
                    AbstractSpellPart augment = recipe.get(j);
                    ResourceLocation augmentId = augment.getRegistryName();
                    if (extend.equals(augmentId)) {
                        extendCount++;
                        j++;
                    } else if (durationDown.equals(augmentId)) {
                        durationDownCount++;
                        j++;
                    } else {
                        break;
                    }
                }
                int delayCooldown = Math.max(0, 1 + extendCount - durationDownCount);
                totalCooldown += delayCooldown;
                index--;
            } else if (extend.equals(id) || durationDown.equals(id)) {
                index--;
            } else {
                break;
            }
        }
        return Math.max(0, totalCooldown);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        animationController = new AnimationController<>(this, "multiphaseController", 0, this::animationPredicate);
        data.add(animationController);
        super.registerControllers(data);
    }

    private PlayState animationPredicate(software.bernie.geckolib.animation.AnimationState<?> event) {
        if (level == null) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
            return PlayState.CONTINUE;
        }
        
        boolean powered = hasRedstoneSignal();
        
        switch (currentAnimationState) {
            case IDLE -> {
                if (powered) {
                    currentAnimationState = AnimationState.BEGIN;
                    animationStartTime = System.currentTimeMillis();
                    event.getController().forceAnimationReset();
                    event.getController().setAnimation(RawAnimation.begin().thenPlay("begin"));
                    return PlayState.CONTINUE;
                } else {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                    return PlayState.CONTINUE;
                }
            }
            case BEGIN -> {
                if (!powered) {
                    currentAnimationState = AnimationState.IDLE;
                    event.getController().forceAnimationReset();
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                    return PlayState.CONTINUE;
                }
                double elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0;
                if (elapsed >= BEGIN_ANIMATION_DURATION) {
                    currentAnimationState = AnimationState.TICK;
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("tick"));
                    return PlayState.CONTINUE;
                }
                event.getController().setAnimation(RawAnimation.begin().thenPlay("begin"));
                return PlayState.CONTINUE;
            }
            case TICK -> {
                if (!powered) {
                    currentAnimationState = AnimationState.END;
                    animationStartTime = System.currentTimeMillis();
                    event.getController().forceAnimationReset();
                    event.getController().setAnimation(RawAnimation.begin().thenPlay("end"));
                    return PlayState.CONTINUE;
                }
                event.getController().setAnimation(RawAnimation.begin().thenLoop("tick"));
                return PlayState.CONTINUE;
            }
            case END -> {
                double elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0;
                if (elapsed >= END_ANIMATION_DURATION) {
                    currentAnimationState = AnimationState.IDLE;
                    event.getController().forceAnimationReset();
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                    return PlayState.CONTINUE;
                }
                if (powered) {
                    currentAnimationState = AnimationState.BEGIN;
                    animationStartTime = System.currentTimeMillis();
                    event.getController().forceAnimationReset();
                    event.getController().setAnimation(RawAnimation.begin().thenPlay("begin"));
                    return PlayState.CONTINUE;
                }
                event.getController().setAnimation(RawAnimation.begin().thenPlay("end"));
                return PlayState.CONTINUE;
            }
        }
        
        return PlayState.STOP;
    }

    @Override
    public void startAnimation(int arg) {
    }

    public record PhaseExecution(SpellPhase phase, String spellName, long gameTime) {
    }
}
