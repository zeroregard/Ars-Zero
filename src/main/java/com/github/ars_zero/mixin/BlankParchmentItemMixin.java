package com.github.ars_zero.mixin;

import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.multi.helpers.MultiPhaseParchmentHelper;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(com.hollingsworth.arsnouveau.common.items.BlankParchmentItem.class)
public class BlankParchmentItemMixin {

    /**
     * When blank parchment is on the Scribes table and the player shift+right-clicks with a
     * multiphase device (staff or circlet) in hand, produce a multiphase spell parchment
     * instead of the default single-spell parchment (which only works with a spell book in hand).
     */
    @Inject(method = "onScribe", at = @At("HEAD"), cancellable = true)
    private void arsZero$inscribeFromMultiphaseDevice(Level world, BlockPos pos, Player player,
            InteractionHand handIn, ItemStack thisStack, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClientSide()) {
            return;
        }
        ItemStack heldStack = player.getItemInHand(handIn);
        if (heldStack.isEmpty() || !(heldStack.getItem() instanceof AbstractMultiPhaseCastDevice)) {
            return;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ScribesTile scribesTile)) {
            return;
        }
        Optional<ItemStack> multiphaseOpt = MultiPhaseParchmentHelper.createMultiphaseParchmentFromDevice(heldStack);
        if (multiphaseOpt.isEmpty()) {
            return;
        }
        scribesTile.setStack(multiphaseOpt.get());
        world.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 2);
        cir.setReturnValue(true);
    }
}
