package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.BlightLiquidBlock;
import com.github.ars_zero.common.fluid.BlightFluid;
import com.github.ars_zero.common.fluid.BlightFluidType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModFluids {
    
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, ArsZero.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, ArsZero.MOD_ID);
    
    public static final DeferredHolder<FluidType, BlightFluidType> BLIGHT_FLUID_TYPE = FLUID_TYPES.register(
        "blight_fluid",
        () -> new BlightFluidType(FluidType.Properties.create()
            .density(1000)
            .viscosity(3500)
            .temperature(300)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );
    
    public static final DeferredHolder<Fluid, BlightFluid.Source> BLIGHT_FLUID = FLUIDS.register(
        "blight_fluid",
        () -> new BlightFluid.Source(createProperties())
    );
    
    public static final DeferredHolder<Fluid, BlightFluid.Flowing> BLIGHT_FLUID_FLOWING = FLUIDS.register(
        "blight_fluid_flowing",
        () -> new BlightFluid.Flowing(createProperties())
    );
    
    public static final DeferredHolder<Block, BlightLiquidBlock> BLIGHT_FLUID_BLOCK = ModBlocks.BLOCKS.register(
        "blight_fluid",
        () -> new BlightLiquidBlock(() -> BLIGHT_FLUID_FLOWING.get(), BlockBehaviour.Properties.of()
            .strength(100.0f)
            .noLootTable()
            .replaceable()
            .liquid())
    );
    
    public static final DeferredHolder<Item, BucketItem> BLIGHT_FLUID_BUCKET = ModItems.ITEMS.register(
        "blight_fluid_bucket",
        () -> new BucketItem(BLIGHT_FLUID.get(), new Item.Properties()
            .craftRemainder(Items.BUCKET)
            .stacksTo(1))
    );
    
    private static BaseFlowingFluid.Properties createProperties() {
        return new BaseFlowingFluid.Properties(
            () -> BLIGHT_FLUID_TYPE.get(),
            () -> BLIGHT_FLUID.get(),
            () -> BLIGHT_FLUID_FLOWING.get()
        )
        .block(() -> BLIGHT_FLUID_BLOCK.get())
        .bucket(() -> BLIGHT_FLUID_BUCKET.get())
        .tickRate(30)
        .slopeFindDistance(4)
        .levelDecreasePerBlock(2);
    }
}
