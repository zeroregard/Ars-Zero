package com.github.ars_zero.common.item;

import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class AbstractSpellStaff extends AbstractMultiPhaseCastDevice implements GeoItem {

    public enum StaffPhase {
        BEGIN,
        TICK,
        END
    }

    public AbstractSpellStaff(SpellTier tier) {
        super(tier, new Properties());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected AbstractMultiPhaseCastDeviceScreen createDeviceScreen(ItemStack stack, InteractionHand hand) {
        return new ArsZeroStaffGUI(stack, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player && !level.isClientSide) {
            int totalDuration = getUseDuration(stack, livingEntity);
            boolean isFirstTick = remainingUseDuration == totalDuration - 1;
            MultiPhaseCastContext context = getCastContext(player, MultiPhaseCastContext.CastSource.ITEM);
            boolean alreadyCasting = context != null && context.isCasting;
            if (isFirstTick && !alreadyCasting) {
                beginPhase(player, stack, MultiPhaseCastContext.CastSource.ITEM);
            } else if (!isFirstTick && alreadyCasting) {
                tickPhase(player, stack);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MultiPhaseCastContext context = getCastContext(player, MultiPhaseCastContext.CastSource.ITEM);
            boolean isCasting = context != null && context.isCasting;
            if (isCasting) {
                return InteractionResultHolder.pass(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        MultiPhaseCastContext context = getCastContext(player, MultiPhaseCastContext.CastSource.ITEM);
        if (context == null || !context.isCasting) {
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
    public int getUseDuration(ItemStack stack, LivingEntity livingEntity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 20, this::idlePredicate));
    }

    private <P extends AbstractSpellStaff & GeoItem> PlayState idlePredicate(AnimationState<P> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                boolean isUsing = mc.player.isUsingItem() && 
                    mc.player.getUseItem().getItem() instanceof AbstractSpellStaff;
                
                if (isUsing) {
                    event.getController().setAnimationSpeed(4.0);
                } else {
                    event.getController().setAnimationSpeed(1.0);
                }
            }
        }
        
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    public static void setFinial(ItemStack stack, String finialType) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        if (finialType != null && !finialType.isEmpty()) {
            tag.putString("finial", finialType);
        } else {
            tag.remove("finial");
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    public static String getFinial(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        if (!tag.contains("finial")) return null;
        String finial = tag.getString("finial");
        return finial.isEmpty() ? null : finial;
    }
    
    public static boolean hasFinial(ItemStack stack) {
        return getFinial(stack) != null;
    }
}


