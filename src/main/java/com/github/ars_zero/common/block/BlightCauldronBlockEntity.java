package com.github.ars_zero.common.block;

import com.github.ars_zero.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlightCauldronBlockEntity extends BlockEntity {
    
    private static final String CONVERSION_LEVEL_TAG = "ConversionLevel";
    
    private float conversionLevel = 0.0f;
    
    public BlightCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLIGHT_CAULDRON.get(), pos, state);
    }
    
    public float getConversionLevel() {
        return conversionLevel;
    }
    
    public void setConversionLevel(float conversionLevel) {
        float clamped = Math.max(0.0f, Math.min(1.0f, conversionLevel));
        if (this.conversionLevel == clamped) {
            return;
        }
        this.conversionLevel = clamped;
        setChanged();
    }
    
    public void addConversionLevel(float delta) {
        setConversionLevel(this.conversionLevel + delta);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(CONVERSION_LEVEL_TAG, conversionLevel);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(CONVERSION_LEVEL_TAG)) {
            setConversionLevel(tag.getFloat(CONVERSION_LEVEL_TAG));
        } else {
            setConversionLevel(0.0f);
        }
    }
}

