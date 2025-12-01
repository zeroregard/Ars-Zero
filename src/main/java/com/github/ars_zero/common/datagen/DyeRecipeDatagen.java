package com.github.ars_zero.common.datagen;

import com.github.ars_zero.common.crafting.recipes.DyeRecipe;
import com.github.ars_zero.registry.ModItems;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.NonNullList;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.core.registries.BuiltInRegistries.ITEM;

public class DyeRecipeDatagen extends SimpleDataProvider {
    List<FileObj> files = new ArrayList<>();

    public DyeRecipeDatagen(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    public void collectJsons(CachedOutput pOutput) {
        addDyeRecipe(ModItems.NOVICE_SPELL_STAFF.get());
        addDyeRecipe(ModItems.MAGE_SPELL_STAFF.get());
        addDyeRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get());
        addDyeRecipe(ModItems.CREATIVE_SPELL_STAFF.get());

        for (FileObj fileObj : files) {
            saveStable(pOutput, fileObj.element, fileObj.path);
        }
    }

    public void add(FileObj fileObj) {
        files.add(fileObj);
    }

    public void addDyeRecipe(ItemLike inputItem) {
        var dyeRecipe = new DyeRecipe("", CraftingBookCategory.MISC, inputItem.asItem().getDefaultInstance(), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Tags.Items.DYES), Ingredient.of(inputItem)));
        String itemName = ITEM.getKey(inputItem.asItem()).getPath();
        JsonElement recipeJson = Recipe.CODEC.encodeStart(JsonOps.INSTANCE, dyeRecipe).getOrThrow();
        files.add(new FileObj(resolvePath("data/ars_zero/recipe/dye_" + itemName + ".json"), recipeJson));
    }

    @Override
    public @NotNull String getName() {
        return "ArsZero: Dye Recipe Datagen";
    }

    Path resolvePath(String path) {
        return this.generator.getPackOutput().getOutputFolder().resolve(path);
    }

    public record FileObj(Path path, JsonElement element) {

    }
}

