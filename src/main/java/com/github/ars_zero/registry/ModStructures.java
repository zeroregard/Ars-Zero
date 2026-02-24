package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.world.structure.BlightDungeon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, ArsZero.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<BlightDungeon>> BLIGHT_DUNGEON =
        STRUCTURES.register("blight_dungeon", () -> () -> BlightDungeon.CODEC);
}
