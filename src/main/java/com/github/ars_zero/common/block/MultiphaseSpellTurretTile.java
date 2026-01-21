package com.github.ars_zero.common.block;

import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.MultiPhaseCastContextRegistry;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.registry.ModBlockEntities;
import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.EntitySpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import com.hollingsworth.arsnouveau.common.block.tile.BasicSpellTurretTile;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketOneShotAnimation;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.UUID;

public class MultiphaseSpellTurretTile extends BasicSpellTurretTile implements IMultiPhaseCaster {

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
    private int tickCount = 0;
    private int tickDelayOffset = 0;

    private MultiPhaseCastContext castContext;
    private UUID ownerUUID;
    private SpellPhase currentCastingPhase = SpellPhase.BEGIN;

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
    
    private static final double BASE_BEGIN_ANIMATION_DURATION = 0.16;
    private static final double BASE_END_ANIMATION_DURATION = 0.28;
    
    private double getBeginAnimationDuration() {
        double speedMultiplier = getAnimationSpeedMultiplier();
        return BASE_BEGIN_ANIMATION_DURATION / speedMultiplier;
    }
    
    private double getEndAnimationDuration() {
        double speedMultiplier = getAnimationSpeedMultiplier();
        return BASE_END_ANIMATION_DURATION / speedMultiplier;
    }
    
    private double getAnimationSpeedMultiplier() {
        int delay = tickCooldown;
        if (delay <= 0) {
            return 1.0;
        }
        return 1.0 / Math.max(1.0, delay);
    }

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
        tickCount = 0;
        this.tickDelayOffset = tickDelayOffset;
        tickCooldown = calculateTickCooldown(tickSpell) + tickDelayOffset;
        phaseHistory.clear();
        clearCastContext();
        currentCastingPhase = SpellPhase.BEGIN;
        ownerUUID = owner;
        setPlayer(owner);
        currentAnimationState = AnimationState.IDLE;
        animationStartTime = 0;
        setChanged();
        updateBlock();
    }

    public void configureSpells(Spell begin, Spell tick, Spell end, UUID owner) {
        configureSpells(begin, tick, end, owner, calculateTickCooldown(tick));
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
        tag.putInt("tick_delay_offset", tickDelayOffset);
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
        tickDelayOffset = tag.contains("tick_delay_offset") ? tag.getInt("tick_delay_offset") : 0;
        if (tag.contains("owner_uuid")) {
            ownerUUID = tag.getUUID("owner_uuid");
        } else {
            ownerUUID = null;
        }
        casting = false;
        tickCount = 0;
        tickCooldown = calculateTickCooldown(tickSpell) + tickDelayOffset;
        phaseHistory.clear();
        clearCastContext();
        currentCastingPhase = SpellPhase.BEGIN;
    }

    private void startCasting() {
        casting = true;
        tickCount = 0;
        tickCooldown = calculateTickCooldown(tickSpell) + tickDelayOffset;
        initializeCastContext();
        castPhase(SpellPhase.BEGIN);
    }

    private void handleTickCasting() {
        if (tickSpell == null || tickSpell.isEmpty()) {
            return;
        }
        
        if (currentCastingPhase == SpellPhase.BEGIN && castContext != null && !castContext.beginFinished) {
            tickCount++;
            return;
        }
        
        tickCount++;
        
        if (tickCooldown > 0 && (tickCount - 1) % tickCooldown != 0) {
            return;
        }
        
        castPhase(SpellPhase.TICK);
    }

    private void finishCasting() {
        casting = false;
        tickCount = 0;
        castPhase(SpellPhase.END);
    }

    private void castPhase(SpellPhase phase) {
        currentCastingPhase = phase;
        
        if (castContext != null) {
            updateContextPhase(phase);
            if (phase == SpellPhase.BEGIN) {
                castContext.isCasting = true;
            }
        }
        
        Spell spell = switch (phase) {
            case BEGIN -> beginSpell;
            case TICK -> tickSpell;
            case END -> endSpell;
        };
        
        if (spell == null || spell.isEmpty()) {
            if (phase == SpellPhase.BEGIN && castContext != null) {
                castContext.beginFinished = true;
            }
            return;
        }
        
        spellCaster = (SpellCaster) spellCaster.setSpell(spell, 0);
        this.shootSpell();
        recordPhase(phase, spell);
    }

    @Override
    public void shootSpell() {
        if (level == null || !(level instanceof ServerLevel serverLevel)) {
            super.shootSpell();
            return;
        }
        
        if (spellCaster.getSpell().isEmpty()) {
            return;
        }
        
        BlockPos pos = this.getBlockPos();
        int manaCost = getManaCost();
        
        if (manaCost > 0 && SourceUtil.takeSourceMultipleWithParticles(pos, serverLevel, 10, manaCost) == null) {
            return;
        }
        
        SpellPhase spellPhase = currentCastingPhase;
        
        int animationArg = switch (spellPhase) {
            case BEGIN -> 0;
            case TICK -> 1;
            case END -> 2;
        };
        Networking.sendToNearbyClient(serverLevel, pos, new PacketOneShotAnimation(pos, animationArg));
        Position iposition = BasicSpellTurret.getDispensePosition(pos, serverLevel.getBlockState(pos).getValue(BasicSpellTurret.FACING));
        
        FakePlayer fakePlayer = ownerUUID != null
                ? FakePlayerFactory.get(serverLevel, new GameProfile(ownerUUID, ""))
                : ANFakePlayer.getPlayer(serverLevel);
        fakePlayer.setPos(pos.getX(), pos.getY(), pos.getZ());
        
        SpellContext spellContext = new SpellContext(serverLevel, spellCaster.getSpell(), fakePlayer, new TileCaster(this, SpellContext.CasterType.TURRET));
        spellContext.tag.putLong("ars_zero:turret_pos", pos.asLong());
        if (castContext != null) {
            spellContext.tag.putUUID("ars_zero:cast_context_id", castContext.castId);
        }
        SpellResolver resolver = new EntitySpellResolver(spellContext);
        
        resolver = wrapResolverForPhase(resolver, spellPhase);
        
        Direction direction = serverLevel.getBlockState(pos).getValue(BasicSpellTurret.FACING);
        
        if (resolver.castType != null && BasicSpellTurret.TURRET_BEHAVIOR_MAP.containsKey(resolver.castType)) {
            fakePlayer.setPos(iposition.x(), iposition.y(), iposition.z());
            BasicSpellTurret.TURRET_BEHAVIOR_MAP.get(resolver.castType).onCast(resolver, serverLevel, pos, fakePlayer, iposition, direction);
        } else {
            fakePlayer.setPos(iposition.x(), iposition.y(), iposition.z());
            boolean canCast = resolver.canCast(fakePlayer);
            if (canCast) {
                resolver.onCast(ItemStack.EMPTY, serverLevel);
            }
        }
    }

    private void initializeCastContext() {
        if (ownerUUID == null) {
            return;
        }
        castContext = new MultiPhaseCastContext(ownerUUID, MultiPhaseCastContext.CastSource.TURRET);
        castContext.isCasting = true;
        updateContextPhase(SpellPhase.BEGIN);
        castContext.beginResults.clear();
        castContext.tickResults.clear();
        castContext.endResults.clear();
        castContext.createdAt = System.currentTimeMillis();
        MultiPhaseCastContextRegistry.register(castContext);
    }

    private void clearCastContext() {
        if (castContext != null) {
            MultiPhaseCastContextRegistry.remove(castContext.castId);
        }
        castContext = null;
    }

    @Override
    public MultiPhaseCastContext getCastContext() {
        return castContext;
    }

    @Override
    public UUID getPlayerId() {
        return ownerUUID;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public int getTickCooldown() {
        return tickCooldown;
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
                event.getController().setAnimationSpeed(1.0);
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
                    event.getController().setAnimationSpeed(1.0);
                    return PlayState.CONTINUE;
                }
                double animationSpeed = getAnimationSpeedMultiplier();
                event.getController().setAnimationSpeed(animationSpeed);
                double elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0;
                if (elapsed >= getBeginAnimationDuration()) {
                    currentAnimationState = AnimationState.TICK;
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("tick"));
                    event.getController().setAnimationSpeed(animationSpeed);
                    return PlayState.CONTINUE;
                }
                event.getController().setAnimation(RawAnimation.begin().thenPlay("begin"));
                return PlayState.CONTINUE;
            }
            case TICK -> {
                double animationSpeed = getAnimationSpeedMultiplier();
                event.getController().setAnimationSpeed(animationSpeed);
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
                double animationSpeed = getAnimationSpeedMultiplier();
                event.getController().setAnimationSpeed(animationSpeed);
                double elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0;
                if (elapsed >= getEndAnimationDuration()) {
                    currentAnimationState = AnimationState.IDLE;
                    event.getController().forceAnimationReset();
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                    event.getController().setAnimationSpeed(1.0);
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
