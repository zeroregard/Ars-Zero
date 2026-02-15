package com.github.ars_zero.common.block;

import com.github.ars_zero.common.item.AbstractStaff;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.common.block.ArcanePlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class StaffDisplayBlock extends ArcanePlatform {

    public StaffDisplayBlock() {
        super();
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new StaffDisplayBlockEntity(pos, state);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof StaffDisplayBlockEntity tile)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack held = player.getItemInHand(hand);

        if (tile.getStack().isEmpty()) {
            if (held.getItem() instanceof AbstractStaff || held.getItem() instanceof SpellBook) {
                tile.setStack(held.copyWithCount(1));
                held.shrink(1);
                level.sendBlockUpdated(pos, state, state, 2);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (held.isEmpty()) {
            ItemEntity dropped = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), tile.getStack());
            level.addFreshEntity(dropped);
            tile.setStack(ItemStack.EMPTY);
            level.sendBlockUpdated(pos, state, state, 2);
            return ItemInteractionResult.SUCCESS;
        }

        if (held.getItem() instanceof AbstractStaff || held.getItem() instanceof SpellBook) {
            ItemEntity dropped = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), tile.getStack());
            level.addFreshEntity(dropped);
            tile.setStack(held.copyWithCount(1));
            held.shrink(1);
            level.sendBlockUpdated(pos, state, state, 2);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
        if (level.getBlockEntity(pos) instanceof StaffDisplayBlockEntity tile && !tile.getStack().isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), tile.getStack()));
        }
        return state;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StaffDisplayBlockEntity tile && !tile.getStack().isEmpty()) {
            return 15;
        }
        return 0;
    }
}
