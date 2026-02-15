package com.github.ars_zero.mixin;

import com.github.ars_zero.common.item.AbstractStaff;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketOpenGlyphCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(com.hollingsworth.arsnouveau.common.block.ScribesBlock.class)
public class ScribesBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void arsZero$allowStaffForGlyphCraft(ItemStack heldStack, BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand handIn, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (world.isClientSide || handIn != InteractionHand.MAIN_HAND || !(world.getBlockEntity(pos) instanceof ScribesTile)) {
            return;
        }
        if (player.getItemInHand(handIn).getItem() instanceof AbstractStaff && !player.isShiftKeyDown()) {
            Networking.sendToPlayerClient(new PacketOpenGlyphCraft(pos), (ServerPlayer) player);
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
        }
    }
}
