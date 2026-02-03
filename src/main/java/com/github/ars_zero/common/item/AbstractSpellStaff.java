package com.github.ars_zero.common.item;

import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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
            IMultiPhaseCaster caster = AbstractMultiPhaseCastDevice.asMultiPhaseCaster(player, stack);
            MultiPhaseCastContext context = caster != null ? caster.getCastContext() : null;
            if (context == null || !context.isCasting) {
                return;
            }
            
            tickPhase(player, stack);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ScribesTile && !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && level.getBlockEntity(hitResult.getBlockPos()) instanceof ScribesTile
                && !player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            IMultiPhaseCaster caster = AbstractMultiPhaseCastDevice.asMultiPhaseCaster(player, stack);
            MultiPhaseCastContext context = caster != null ? caster.getCastContext() : null;
            boolean isCasting = context != null && context.isCasting;
            if (isCasting) {
                return InteractionResultHolder.pass(stack);
            }
            beginPhase(player, stack, MultiPhaseCastContext.CastSource.ITEM);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        IMultiPhaseCaster caster = AbstractMultiPhaseCastDevice.asMultiPhaseCaster(player, stack);
        MultiPhaseCastContext context = caster != null ? caster.getCastContext() : null;
        if (context == null || !context.isCasting) {
            return;
        }
        
        if (context.sequenceTick == 0) {
            tickPhase(player, stack);
        }
        
        endPhase(player, stack);
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
}


