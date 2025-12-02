package com.github.ars_zero.common.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.RadialMenuTracker;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketSetMultiPhaseSpellCastingSlot;
import com.github.ars_zero.common.network.PacketStaffSpellFired;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.MultiPhaseCastContextMap;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.github.ars_zero.registry.ModAttachments;
import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.sound.ConfiguredSpellSound;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenuSlot;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.SecondaryIconPosition;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractMultiPhaseCastDevice extends Item implements ICasterTool, IRadialProvider {

    public static class ArsZeroSpellContext extends SpellContext {
        public final SpellPhase phase;

        public ArsZeroSpellContext(Level world, Spell spell, LivingEntity caster, SpellPhase phase, ItemStack casterTool) {
            super(world, spell, caster, LivingCaster.from(caster), casterTool);
            this.phase = phase;
        }
    }

    private final SpellTier tier;
    private static final String SLOT_TICK_DELAY_KEY = "ars_zero_tick_delays";
    private static final int SLOT_COUNT = 10;
    private static final int DEFAULT_TICK_DELAY = 1;
    private static final int MAX_TICK_DELAY = 20;

    protected AbstractMultiPhaseCastDevice(SpellTier tier, Properties properties) {
        super(properties
            .stacksTo(1)
            .component(DataComponents.BASE_COLOR, DyeColor.PURPLE)
            .component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(30)));
        this.tier = tier;
    }

    public SpellTier getTier() {
        return tier;
    }

    protected static MultiPhaseCastContext getOrCreateContext(Player player, MultiPhaseCastContext.CastSource source) {
        MultiPhaseCastContextMap contextMap = player.getData(ModAttachments.CAST_CONTEXTS);
        if (contextMap == null) {
            contextMap = new MultiPhaseCastContextMap(player.getUUID());
            player.setData(ModAttachments.CAST_CONTEXTS, contextMap);
        }
        return contextMap.getOrCreate(source);
    }

    public static MultiPhaseCastContext getCastContext(Player player, MultiPhaseCastContext.CastSource source) {
        MultiPhaseCastContextMap contextMap = player.getData(ModAttachments.CAST_CONTEXTS);
        if (contextMap == null) {
            return null;
        }
        return contextMap.get(source);
    }

    public static MultiPhaseCastContext findContextByStack(Player player, ItemStack stack) {
        MultiPhaseCastContextMap contextMap = player.getData(ModAttachments.CAST_CONTEXTS);
        if (contextMap == null || stack.isEmpty()) {
            return null;
        }
        for (MultiPhaseCastContext context : contextMap.getAll().values()) {
            if (ItemStack.isSameItem(context.castingStack, stack)) {
                return context;
            }
        }
        return null;
    }

    public static void clearContext(Player player, MultiPhaseCastContext.CastSource source) {
        MultiPhaseCastContextMap contextMap = player.getData(ModAttachments.CAST_CONTEXTS);
        if (contextMap != null) {
            contextMap.remove(source);
            if (contextMap.isEmpty()) {
                player.removeData(ModAttachments.CAST_CONTEXTS);
            }
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster != null) {
            caster.setCurrentSlot(0);
            caster.saveToStack(stack);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract AbstractMultiPhaseCastDeviceScreen createDeviceScreen(ItemStack stack, InteractionHand hand);

    @OnlyIn(Dist.CLIENT)
    protected void openDeviceGUI(ItemStack stack, Player player, InteractionHand hand) {
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            player.sendSystemMessage(Component.literal("§cError: Device has no spell data! Try crafting a new one."));
            return;
        }
        Minecraft.getInstance().setScreen(createDeviceScreen(stack, hand));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onOpenBookMenuKeyPressed(ItemStack stack, Player player) {
        InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND) == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        openDeviceGUI(stack, player, hand);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onRadialKeyPressed(ItemStack stack, Player player) {
        RadialMenuTracker.activate(stack);
        if (Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new GuiRadialMenu<>(getRadialMenuProviderForSpellpart(stack)));
        }
    }

    public RadialMenu<AbstractSpellPart> getRadialMenuProviderForSpellpart(ItemStack stack) {
        boolean isCirclet = stack.getItem() instanceof SpellcastingCirclet;
        return new RadialMenu<>(
            logicalSlot -> Networking.sendToServer(new PacketSetMultiPhaseSpellCastingSlot(logicalSlot, isCirclet)),
            getRadialMenuSlotsForSpellpart(stack),
            SecondaryIconPosition.NORTH,
            RenderUtils::drawSpellPart,
            0
        );
    }

    public List<RadialMenuSlot<AbstractSpellPart>> getRadialMenuSlotsForSpellpart(ItemStack stack) {
        AbstractCaster<?> spellCaster = SpellCasterRegistry.from(stack);
        List<RadialMenuSlot<AbstractSpellPart>> radialMenuSlots = new ArrayList<>();

        for (int logicalSlot = 0; logicalSlot < 10; logicalSlot++) {
            int tickPhysicalSlot = logicalSlot * 3 + SpellPhase.TICK.ordinal();
            Spell spell = spellCaster.getSpell(tickPhysicalSlot);

            if (!spell.isEmpty()) {
                AbstractSpellPart iconGlyph = null;
                for (AbstractSpellPart part : spell.recipe()) {
                    if (!(part instanceof AbstractCastMethod)) {
                        iconGlyph = part;
                        break;
                    }
                }
                if (iconGlyph == null && spell.recipe().iterator().hasNext()) {
                    iconGlyph = spell.recipe().iterator().next();
                }
                radialMenuSlots.add(new RadialMenuSlot<>(spellCaster.getSpellName(tickPhysicalSlot), iconGlyph));
            } else {
                radialMenuSlots.add(new RadialMenuSlot<>("Empty", null));
            }
        }
        return radialMenuSlots;
    }

    protected void beginPhase(Player player, ItemStack stack, MultiPhaseCastContext.CastSource source) {
        MultiPhaseCastContext context = getOrCreateContext(player, source);

        context.currentPhase = SpellPhase.BEGIN;
        context.isCasting = true;
        context.tickCount = 0;
        context.sequenceTick = 0;
        context.outOfMana = false;
        context.createdAt = System.currentTimeMillis();
        context.beginResults.clear();
        context.tickResults.clear();
        context.endResults.clear();
        context.source = source;
        context.castingStack = stack;

        executeSpell(player, stack, SpellPhase.BEGIN);

        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, SpellPhase.BEGIN, source);
        }
    }

    public void tickPhase(Player player, ItemStack stack) {
        ItemStack castingStack = resolveCastingStack(player, stack);
        if (castingStack.isEmpty()) {
            MultiPhaseCastContext context = findContextByStack(player, stack);
            if (context != null) {
                clearContext(player, context.source);
            }
            return;
        }

        MultiPhaseCastContext context = findContextByStack(player, castingStack);
        if (context == null || !context.isCasting) {
            return;
        }

        context.currentPhase = SpellPhase.TICK;
        context.tickCount++;
        context.sequenceTick++;

        AbstractCaster<?> caster = SpellCasterRegistry.from(castingStack);
        if (caster == null) {
            return;
        }
        int currentLogicalSlot = caster.getCurrentSlot();
        if (currentLogicalSlot >= 0 && currentLogicalSlot < 10) {
            int physicalSlot = currentLogicalSlot * 3 + SpellPhase.TICK.ordinal();
            Spell spell = caster.getSpell(physicalSlot);

            if (context.tickCount == 1) {
                int sliderDelay = getSlotTickDelayOffset(castingStack, currentLogicalSlot);
                context.tickCooldown = calculateTickCooldown(spell) + sliderDelay;
            }
        }

        if (context.tickCooldown > 0 && (context.tickCount - 1) % (context.tickCooldown + 1) != 0) {
            return;
        }

        executeSpell(player, castingStack, SpellPhase.TICK);

        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, SpellPhase.TICK, context.source);
        }
    }

    public void endPhase(Player player, ItemStack stack) {
        ItemStack castingStack = resolveCastingStack(player, stack);
        MultiPhaseCastContext context = findContextByStack(player, castingStack);
        if (castingStack.isEmpty()) {
            if (context != null) {
                clearContext(player, context.source);
            }
            return;
        }
        if (context == null || !context.isCasting) {
            return;
        }

        context.currentPhase = SpellPhase.END;

        AnchorEffect.restoreEntityPhysics(context);

        executeSpell(player, castingStack, SpellPhase.END);

        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, SpellPhase.END, context.source);
        }

        clearContext(player, context.source);
    }

    private ItemStack resolveCastingStack(Player player, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            return stack;
        }
        MultiPhaseCastContextMap contextMap = player.getData(ModAttachments.CAST_CONTEXTS);
        if (contextMap != null) {
            for (MultiPhaseCastContext context : contextMap.getAll().values()) {
                if (!context.castingStack.isEmpty()) {
                    return context.castingStack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void executeSpell(Player player, ItemStack stack, SpellPhase phase) {
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            return;
        }

        int currentLogicalSlot = caster.getCurrentSlot();
        if (currentLogicalSlot < 0 || currentLogicalSlot >= 10) {
            currentLogicalSlot = 0;
        }

        boolean hasSpells = false;
        for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
            int testSlot = currentLogicalSlot * 3 + phaseIndex;
            if (!caster.getSpell(testSlot).isEmpty()) {
                hasSpells = true;
                break;
            }
        }

        if (!hasSpells) {
            for (int testLogicalSlot = 0; testLogicalSlot < 10; testLogicalSlot++) {
                for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
                    int testSlot = testLogicalSlot * 3 + phaseIndex;
                    if (!caster.getSpell(testSlot).isEmpty()) {
                        currentLogicalSlot = testLogicalSlot;
                        hasSpells = true;
                        break;
                    }
                }
                if (hasSpells) break;
            }
        }

        int physicalSlot = currentLogicalSlot * 3 + phase.ordinal();
        Spell spell = caster.getSpell(physicalSlot);

        if (spell.isEmpty()) {
            return;
        }

        if (isTemporalContextFormSpell(spell)) {
        }

        checkManaAndCast(player, stack, spell, phase);
    }

    private boolean checkManaAndCast(Player player, ItemStack stack, Spell spell, SpellPhase phase) {
        ArsZeroSpellContext context = new ArsZeroSpellContext(player.level(), spell, player, phase, stack);
        SpellResolver resolver = new SpellResolver(context);

        if (phase == SpellPhase.BEGIN) {
            MultiPhaseCastContext castContext = findContextByStack(player, stack);
            if (castContext != null) {
                resolver = new WrappedSpellResolver(resolver, player.getUUID(), SpellPhase.BEGIN, true);
            }
        }

        boolean canCast = resolver.canCast(player);

        if (canCast) {
            try {
                MultiPhaseCastContext castContext = findContextByStack(player, stack);
                InteractionHand hand = InteractionHand.MAIN_HAND;
                if (castContext != null && castContext.source == MultiPhaseCastContext.CastSource.CURIO) {
                    if (player.getMainHandItem().isEmpty()) {
                        hand = InteractionHand.MAIN_HAND;
                    } else if (player.getOffhandItem().isEmpty()) {
                        hand = InteractionHand.OFF_HAND;
                    } else {
                        hand = InteractionHand.MAIN_HAND;
                    }
                }
                
                AugmentSensitive sensitiveAugment = AugmentSensitive.INSTANCE;
                boolean isSensitive = resolver.spell.getBuffsAtIndex(0, player, sensitiveAugment) > 0;

                HitResult hitResult = SpellUtil.rayTrace(
                    player,
                    0.5 + player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue(),
                    1,
                    isSensitive
                );

                boolean castSuccess = false;

                if (hitResult instanceof EntityHitResult entityHitResult &&
                    entityHitResult.getEntity() instanceof LivingEntity) {
                    resolver.onCastOnEntity(stack, entityHitResult.getEntity(), hand);
                    castSuccess = true;
                }
                else if (hitResult instanceof BlockHitResult blockHitResult &&
                        (hitResult.getType() == HitResult.Type.BLOCK || isSensitive)) {
                    UseOnContext useContext = new UseOnContext(
                        player,
                        hand,
                        blockHitResult
                    );
                    resolver.onCastOnBlock(useContext);
                    castSuccess = true;
                }
                else {
                    castSuccess = resolver.onCast(stack, player.level());
                }

                if (!castSuccess) {
                    return false;
                }
            } catch (Exception e) {
                ArsZero.LOGGER.error("Exception during cast: ", e);
                return false;
            }
        }

        return canCast;
    }

    public static void setStaffSounds(ItemStack stack, ConfiguredSpellSound beginSound, ConfiguredSpellSound tickSound, ConfiguredSpellSound endSound, ResourceLocation tickLoopingSound) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();

        tag.putString("beginSound", beginSound.getSound() != null ? beginSound.getSound().getId().toString() : "");
        tag.putFloat("beginVolume", beginSound.getVolume());
        tag.putFloat("beginPitch", beginSound.getPitch());
        tag.putString("tickSound", tickSound.getSound() != null ? tickSound.getSound().getId().toString() : "");
        tag.putFloat("tickVolume", tickSound.getVolume());
        tag.putFloat("tickPitch", tickSound.getPitch());
        tag.putString("endSound", endSound.getSound() != null ? endSound.getSound().getId().toString() : "");
        tag.putFloat("endVolume", endSound.getVolume());
        tag.putFloat("endPitch", endSound.getPitch());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ConfiguredSpellSound getBeginSound(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ConfiguredSpellSound.EMPTY;
        CompoundTag tag = data.copyTag();
        if (!tag.contains("beginSound")) return ConfiguredSpellSound.EMPTY;

        String soundId = tag.getString("beginSound");
        if (soundId.isEmpty()) return ConfiguredSpellSound.EMPTY;

        var sound = com.hollingsworth.arsnouveau.api.registry.SpellSoundRegistry.get(ResourceLocation.parse(soundId));
        if (sound == null) return ConfiguredSpellSound.EMPTY;

        return new ConfiguredSpellSound(sound, tag.getFloat("beginVolume"), tag.getFloat("beginPitch"));
    }

    public static ConfiguredSpellSound getTickSound(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ConfiguredSpellSound.EMPTY;
        CompoundTag tag = data.copyTag();
        if (!tag.contains("tickSound")) return ConfiguredSpellSound.EMPTY;

        String soundId = tag.getString("tickSound");
        if (soundId.isEmpty()) return ConfiguredSpellSound.EMPTY;

        var sound = com.hollingsworth.arsnouveau.api.registry.SpellSoundRegistry.get(ResourceLocation.parse(soundId));
        if (sound == null) return ConfiguredSpellSound.EMPTY;

        return new ConfiguredSpellSound(sound, tag.getFloat("tickVolume"), tag.getFloat("tickPitch"));
    }

    public static ConfiguredSpellSound getEndSound(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ConfiguredSpellSound.EMPTY;
        CompoundTag tag = data.copyTag();
        if (!tag.contains("endSound")) return ConfiguredSpellSound.EMPTY;

        String soundId = tag.getString("endSound");
        if (soundId.isEmpty()) return ConfiguredSpellSound.EMPTY;

        var sound = com.hollingsworth.arsnouveau.api.registry.SpellSoundRegistry.get(ResourceLocation.parse(soundId));
        if (sound == null) return ConfiguredSpellSound.EMPTY;

        return new ConfiguredSpellSound(sound, tag.getFloat("endVolume"), tag.getFloat("endPitch"));
    }

    public static ResourceLocation getTickLoopingSound(ItemStack stack) {
        return null;
    }

    public static int[] getSlotTickDelays(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return createDefaultDelayArray();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : null;
        return readDelayArray(tag);
    }

    public static int getSlotTickDelay(ItemStack stack, int logicalSlot) {
        int index = Mth.clamp(logicalSlot, 0, SLOT_COUNT - 1);
        return getSlotTickDelays(stack)[index];
    }

    public static int getSlotTickDelayOffset(ItemStack stack, int logicalSlot) {
        return Math.max(0, getSlotTickDelay(stack, logicalSlot) - DEFAULT_TICK_DELAY);
    }

    public static void setSlotTickDelay(ItemStack stack, int logicalSlot, int delay) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int index = Mth.clamp(logicalSlot, 0, SLOT_COUNT - 1);
        int clampedDelay = Mth.clamp(delay, DEFAULT_TICK_DELAY, MAX_TICK_DELAY);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        int[] delays = readDelayArray(tag);
        delays[index] = clampedDelay;
        tag.putIntArray(SLOT_TICK_DELAY_KEY, delays);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static int[] readDelayArray(CompoundTag tag) {
        int[] delays = createDefaultDelayArray();
        if (tag == null || !tag.contains(SLOT_TICK_DELAY_KEY, Tag.TAG_INT_ARRAY)) {
            return delays;
        }
        int[] stored = tag.getIntArray(SLOT_TICK_DELAY_KEY);
        for (int i = 0; i < delays.length; i++) {
            int value = i < stored.length ? stored[i] : DEFAULT_TICK_DELAY;
            delays[i] = Mth.clamp(value, DEFAULT_TICK_DELAY, MAX_TICK_DELAY);
        }
        return delays;
    }

    private static int[] createDefaultDelayArray() {
        int[] delays = new int[SLOT_COUNT];
        Arrays.fill(delays, DEFAULT_TICK_DELAY);
        return delays;
    }

    private boolean isTemporalContextFormSpell(Spell spell) {
        if (spell.isEmpty()) return false;

        return spell.recipe().iterator().next() instanceof TemporalContextForm;
    }

    private int calculateTickCooldown(Spell spell) {
        if (spell.isEmpty()) return 0;

        List<AbstractSpellPart> recipe = new ArrayList<>();
        for (AbstractSpellPart part : spell.recipe()) {
            recipe.add(part);
        }

        if (recipe.isEmpty()) return 0;

        int totalCooldown = 0;
        int i = recipe.size() - 1;

        while (i >= 0) {
            AbstractSpellPart part = recipe.get(i);
            ResourceLocation partId = part.getRegistryName();

            if (partId.equals(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_delay"))) {
                int baseDelay = 1;
                int extendCount = 0;
                int durationDownCount = 0;

                int j = i + 1;
                while (j < recipe.size()) {
                    AbstractSpellPart augment = recipe.get(j);
                    ResourceLocation augmentId = augment.getRegistryName();

                    if (augmentId.equals(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_extend_time"))) {
                        extendCount++;
                        j++;
                    } else if (augmentId.equals(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_duration_down"))) {
                        durationDownCount++;
                        j++;
                    } else {
                        break;
                    }
                }

                int delayCooldown = baseDelay + extendCount - durationDownCount;
                totalCooldown += Math.max(0, delayCooldown);

                i--;
            } else if (partId.equals(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_extend_time")) ||
                       partId.equals(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_duration_down"))) {
                i--;
            } else {
                break;
            }
        }

        return totalCooldown;
    }

    protected void sendSpellFiredPacket(ServerPlayer player, SpellPhase phase, MultiPhaseCastContext.CastSource source) {
        boolean isMainHand = player.getUsedItemHand() == InteractionHand.MAIN_HAND;

        int tickCount = 0;
        MultiPhaseCastContext context = getCastContext(player, source);
        if (context != null) {
            tickCount = context.tickCount;
            if (context.source == MultiPhaseCastContext.CastSource.CURIO) {
                isMainHand = false;
            }
        }

        boolean isCurio = source == MultiPhaseCastContext.CastSource.CURIO;
        PacketStaffSpellFired packet = new PacketStaffSpellFired(phase.ordinal(), isMainHand, tickCount, isCurio);
        PacketDistributor.sendToPlayer(player, packet);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        return stack;
    }
}

