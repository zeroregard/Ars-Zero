package com.github.ars_zero.common.datagen;

import com.github.ars_zero.registry.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.core.registries.BuiltInRegistries.ITEM;

public class FinialRecipeDatagen extends SimpleDataProvider {
    List<FileObj> files = new ArrayList<>();

    public FinialRecipeDatagen(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    public void collectJsons(CachedOutput pOutput) {
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_EARTH.get(), "earth");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_EARTH.get(), "earth");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_EARTH.get(), "earth");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_EARTH.get(), "earth");
        
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_AIR.get(), "air");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_AIR.get(), "air");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_AIR.get(), "air");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_AIR.get(), "air");
        
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_FIRE.get(), "fire");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_FIRE.get(), "fire");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_FIRE.get(), "fire");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_FIRE.get(), "fire");
        
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_WATER.get(), "water");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_WATER.get(), "water");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_WATER.get(), "water");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_WATER.get(), "water");
        
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_CONJURATION.get(), "conjuration");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_CONJURATION.get(), "conjuration");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_CONJURATION.get(), "conjuration");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_CONJURATION.get(), "conjuration");
        
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_ABJURATION.get(), "abjuration");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_ABJURATION.get(), "abjuration");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_ABJURATION.get(), "abjuration");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_ABJURATION.get(), "abjuration");
        
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_MANIPULATION.get(), "manipulation");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_MANIPULATION.get(), "manipulation");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_MANIPULATION.get(), "manipulation");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_MANIPULATION.get(), "manipulation");
        
        addFinialRecipe(ModItems.NOVICE_SPELL_STAFF.get(), ModItems.FINIAL_NECROMANCY.get(), "necromancy");
        addFinialRecipe(ModItems.MAGE_SPELL_STAFF.get(), ModItems.FINIAL_NECROMANCY.get(), "necromancy");
        addFinialRecipe(ModItems.ARCHMAGE_SPELL_STAFF.get(), ModItems.FINIAL_NECROMANCY.get(), "necromancy");
        addFinialRecipe(ModItems.CREATIVE_SPELL_STAFF.get(), ModItems.FINIAL_NECROMANCY.get(), "necromancy");

        for (FileObj fileObj : files) {
            saveStable(pOutput, fileObj.element, fileObj.path);
        }
    }

    private void addFinialRecipe(net.minecraft.world.level.ItemLike staff, net.minecraft.world.level.ItemLike finial, String finialType) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:enchantment_apparatus");
        
        JsonArray reagents = new JsonArray();
        reagents.add(item(id(staff.asItem())));
        reagents.add(item(id(finial.asItem())));
        json.add("reagent", reagents);
        
        JsonArray pedestalItems = new JsonArray();
        json.add("pedestalItems", pedestalItems);
        
        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", id(staff.asItem()));
        json.add("result", result);
        
        String staffName = ITEM.getKey(staff.asItem()).getPath();
        String finialName = ITEM.getKey(finial.asItem()).getPath();
        files.add(new FileObj(resolvePath("data/ars_zero/recipe/enchantment_apparatus/" + staffName + "_" + finialName + ".json"), json));
    }

    private static JsonObject item(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("item", id);
        return o;
    }

    private static String id(net.minecraft.world.item.Item item) {
        return ITEM.getKey(item).toString();
    }

    @Override
    public @NotNull String getName() {
        return "ArsZero: Finial Recipe Datagen";
    }

    Path resolvePath(String path) {
        return this.generator.getPackOutput().getOutputFolder().resolve(path);
    }

    public record FileObj(Path path, com.google.gson.JsonElement element) { }
}
