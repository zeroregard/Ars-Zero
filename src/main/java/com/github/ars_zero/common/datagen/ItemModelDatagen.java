package com.github.ars_zero.common.datagen;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ItemModelDatagen extends ItemModelProvider {

    public ItemModelDatagen(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ArsZero.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent("blighted_soil", ArsZero.prefix("block/blighted_soil"));
        for (String name : ModBlocks.CORRUPTED_BASE_NAMES) {
            withExistingParent(name, ArsZero.prefix("block/" + name));
            withExistingParent(name + "_stairs", ArsZero.prefix("block/" + name + "_stairs"));
            withExistingParent(name + "_slab", ArsZero.prefix("block/" + name + "_slab"));
        }
    }
}
