package com.github.ars_zero.common.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.network.PacketStaffSpellFired;
import com.github.ars_zero.common.network.PacketSetStaffSlot;
import com.github.ars_zero.common.spell.CastPhase;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.github.ars_zero.registry.ModAttachments;
import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenuSlot;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.SecondaryIconPosition;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.api.sound.ConfiguredSpellSound;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;

public class ArsZeroStaff extends Item implements ICasterTool, IRadialProvider, GeoItem {
    
    public enum StaffPhase {
        BEGIN,
        TICK,
        END
    }
    
    private static StaffCastContext getOrCreateContext(Player player) {
        StaffCastContext context = player.getData(ModAttachments.STAFF_CONTEXT);
        if (context == null) {
            context = new StaffCastContext(player.getUUID());
            player.setData(ModAttachments.STAFF_CONTEXT, context);
        }
        return context;
    }
    
    private static StaffCastContext getContext(Player player) {
        return player.getData(ModAttachments.STAFF_CONTEXT);
    }
    
    private static void clearContext(Player player) {
        player.removeData(ModAttachments.STAFF_CONTEXT);
    }
    
    public static class ArsZeroSpellContext extends SpellContext {
        public final StaffPhase phase;
        
        public ArsZeroSpellContext(Level world, Spell spell, LivingEntity caster, StaffPhase phase) {
            super(world, spell, caster, LivingCaster.from(caster), caster.getMainHandItem());
            this.phase = phase;
        }
    }
    
    // GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArsZeroStaff() {
        super(new Item.Properties()
            .stacksTo(1)
            .component(DataComponents.BASE_COLOR, DyeColor.PURPLE)
            .component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(30)));
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

    
    @Override
    public void onUseTick(Level level, net.minecraft.world.entity.LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player && !level.isClientSide) {
            int totalDuration = getUseDuration(stack, livingEntity);
            boolean isFirstTick = remainingUseDuration == totalDuration - 1;
            StaffCastContext context = getContext(player);
            boolean alreadyHolding = context != null && context.isHoldingStaff;
            
            if (isFirstTick && !alreadyHolding) {
                beginPhase(player, stack);
            } else if (!isFirstTick && alreadyHolding) {
                tickPhase(player, stack);
            }
        }
    }
    

    @OnlyIn(Dist.CLIENT)
    private void openStaffGUI(Player player) {
        Minecraft.getInstance().setScreen(new ArsZeroStaffGUI());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onOpenBookMenuKeyPressed(ItemStack stack, Player player) {
        openStaffGUI(player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onRadialKeyPressed(ItemStack stack, Player player) {
        if (Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new GuiRadialMenu<>(getRadialMenuProviderForSpellpart(stack)));
        }
    }

    public RadialMenu<AbstractSpellPart> getRadialMenuProviderForSpellpart(ItemStack itemStack) {
        return new RadialMenu<>((int logicalSlot) -> {
            com.github.ars_zero.common.network.Networking.sendToServer(new PacketSetStaffSlot(logicalSlot));
        },
                getRadialMenuSlotsForSpellpart(itemStack),
                SecondaryIconPosition.NORTH,
                RenderUtils::drawSpellPart,
                0);
    }

    public List<RadialMenuSlot<AbstractSpellPart>> getRadialMenuSlotsForSpellpart(ItemStack itemStack) {
        AbstractCaster<?> spellCaster = SpellCasterRegistry.from(itemStack);
        List<RadialMenuSlot<AbstractSpellPart>> radialMenuSlots = new ArrayList<>();
        
        for (int logicalSlot = 0; logicalSlot < 10; logicalSlot++) {
            int tickPhysicalSlot = logicalSlot * 3 + StaffPhase.TICK.ordinal();
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
                radialMenuSlots.add(new RadialMenuSlot<AbstractSpellPart>(spellCaster.getSpellName(tickPhysicalSlot), iconGlyph));
            } else {
                radialMenuSlots.add(new RadialMenuSlot<AbstractSpellPart>("Empty", null));
            }
        }
        return radialMenuSlots;
    }


    private void beginPhase(Player player, ItemStack stack) {
        StaffCastContext context = getOrCreateContext(player);
        
        context.currentPhase = StaffPhase.BEGIN;
        context.isHoldingStaff = true;
        context.tickCount = 0;
        context.sequenceTick = 0;
        context.outOfMana = false;
        context.createdAt = System.currentTimeMillis();
        context.beginResults.clear();
        context.tickResults.clear();
        context.endResults.clear();
        
        // playPhaseSound(player, stack, StaffPhase.BEGIN);
        executeSpell(player, stack, StaffPhase.BEGIN);
        
        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, StaffPhase.BEGIN);
        }
    }

    public void tickPhase(Player player, ItemStack stack) {
        StaffCastContext context = getContext(player);
        if (context == null || !context.isHoldingStaff) {
            return;
        }
        
        context.currentPhase = StaffPhase.TICK;
        context.tickCount++;
        context.sequenceTick++;
        
        executeSpell(player, stack, StaffPhase.TICK);
        
        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, StaffPhase.TICK);
        }
    }
    

    public void endPhase(Player player, ItemStack stack) {
        StaffCastContext context = getContext(player);
        if (context == null || !context.isHoldingStaff) {
            return;
        }
        
        context.currentPhase = StaffPhase.END;
        
        com.github.ars_zero.common.glyph.TranslateEffect.restoreEntityPhysics(context);
        
        // playPhaseSound(player, stack, StaffPhase.END);
        executeSpell(player, stack, StaffPhase.END);
        
        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, StaffPhase.END);
        }
        
        clearContext(player);
    }

    private void executeSpell(Player player, ItemStack stack, StaffPhase phase) {
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
            // Temporal context is now handled via events
        }
        
        checkManaAndCast(player, stack, spell, phase);
    }
    
    private boolean checkManaAndCast(Player player, ItemStack stack, Spell spell, StaffPhase phase) {
        ArsZeroSpellContext context = new ArsZeroSpellContext(player.level(), spell, player, phase);
        SpellResolver resolver = new SpellResolver(context);
        
        if (phase == StaffPhase.BEGIN) {
            StaffCastContext staffContext = getContext(player);
            if (staffContext != null) {
                resolver = new WrappedSpellResolver(resolver, player.getUUID(), CastPhase.BEGIN, true);
            }
        }
        
        boolean canCast = resolver.canCast(player);
        
        if (canCast) {
            try {
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
                    resolver.onCastOnEntity(stack, entityHitResult.getEntity(), InteractionHand.MAIN_HAND);
                    castSuccess = true;
                }
                else if (hitResult instanceof BlockHitResult blockHitResult && 
                         (hitResult.getType() == HitResult.Type.BLOCK || isSensitive)) {
                    UseOnContext useContext = new UseOnContext(
                        player, 
                        InteractionHand.MAIN_HAND, 
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

    public static StaffCastContext getStaffContext(Player player) {
        return player.getData(ModAttachments.STAFF_CONTEXT);
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
        // if (tickLoopingSound != null) {
        //     tag.putString("tickLooping", tickLoopingSound.toString());
        // }
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
        // CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        // if (data == null) return null;
        // CompoundTag tag = data.copyTag();
        // if (!tag.contains("tickLooping")) return null;
        // return ResourceLocation.parse(tag.getString("tickLooping"));
        return null;
    }
    
    
    /**
     * Check if a spell starts with TemporalContextForm
     */
    private boolean isTemporalContextFormSpell(Spell spell) {
        if (spell.isEmpty()) return false;
        
        // Check if the first glyph in the spell is TemporalContextForm
        return spell.recipe().iterator().next() instanceof TemporalContextForm;
    }
    

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        
        StaffCastContext context = getContext(player);
        boolean isHolding = context != null && context.isHoldingStaff;
        
        if (isHolding) {
            return InteractionResultHolder.pass(stack);
        }
        
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        
        StaffCastContext context = getContext(player);
        if (context == null || !context.isHoldingStaff) {
            return;
        }
        
        if (context.sequenceTick == 0) {
            tickPhase(player, stack);
            endPhase(player, stack);
        } else {
            endPhase(player, stack);
        }
    }
    
    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity livingEntity) {
        return 72000; // 1 hour - effectively infinite for our purposes
    }
    
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Use bow animation for staff
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity livingEntity) {
        return stack;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("item.ars_zero.ars_zero_staff.desc");
    }
    
    private void sendSpellFiredPacket(ServerPlayer player, StaffPhase phase) {
        PacketStaffSpellFired packet = new PacketStaffSpellFired(phase.ordinal());
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    // private void playPhaseSound(Player player, ItemStack stack, StaffPhase phase) {
    //     switch (phase) {
    //         case BEGIN -> {
    //             player.level().playSound(null, player.blockPosition(), 
    //                 net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 
    //                 SoundSource.PLAYERS, 0.5f, 0.8f);
    //         }
    //         case END -> {
    //             player.level().playSound(null, player.blockPosition(), 
    //                 net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 
    //                 SoundSource.PLAYERS, 0.5f, 1.2f);
    //         }
    //         case TICK -> {
    //         }
    //     }
    // }

    // GeckoLib implementation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "idle_controller", 20, this::idlePredicate));
    }

    private <P extends Item & GeoAnimatable> PlayState idlePredicate(AnimationState<P> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new com.github.ars_zero.client.renderer.item.CreativeSpellStaffRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }


}
