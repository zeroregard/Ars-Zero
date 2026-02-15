package com.github.ars_zero.common.block;

import com.github.ars_zero.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class StaffDisplayBlockEntity extends BlockEntity {

    private static final String ITEM_TAG = "itemStack";

    private ItemStack stack = ItemStack.EMPTY;

    public StaffDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAFF_DISPLAY.get(), pos, state);
    }

    @NotNull
    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack newStack) {
        this.stack = newStack;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!stack.isEmpty()) {
            Tag stackTag = stack.save(registries);
            tag.put(ITEM_TAG, stackTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stack = ItemStack.parseOptional(registries, tag.getCompound(ITEM_TAG));
    }
}
