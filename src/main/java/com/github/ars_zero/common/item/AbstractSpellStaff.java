package com.github.ars_zero.common.item;

import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}


