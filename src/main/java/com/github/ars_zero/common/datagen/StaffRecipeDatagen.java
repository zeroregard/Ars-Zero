package com.github.ars_zero.common.datagen;

import com.github.ars_zero.registry.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.core.registries.BuiltInRegistries.ITEM;

public class StaffRecipeDatagen extends SimpleDataProvider {
    List<FileObj> files = new ArrayList<>();

    public StaffRecipeDatagen(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    public void collectJsons(CachedOutput pOutput) {
        addArchwoodRodRecipe();
        addNoviceStaffRecipe();
        addMageStaffRecipe();
        addArchmageStaffRecipe();

        for (FileObj fileObj : files) {
            saveStable(pOutput, fileObj.element, fileObj.path);
        }
    }

    private void addArchwoodRodRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        json.addProperty("category", "misc");

        JsonArray pattern = new JsonArray();
        pattern.add("  A");
        pattern.add(" A ");
        pattern.add("A  ");
        json.add("pattern", pattern);

        JsonObject key = new JsonObject();
        JsonObject a = new JsonObject();
        a.addProperty("tag", "c:logs/archwood");
        key.add("A", a);
        json.add("key", key);

        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", "ars_zero:archwood_rod");
        json.add("result", result);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/archwood_rod.json"), json));
    }

    private void addNoviceStaffRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");
        json.addProperty("category", "misc");

        JsonArray ingredients = new JsonArray();
        ingredients.add(item("ars_zero:archwood_rod"));
        ingredients.add(item(id(Items.IRON_SHOVEL)));
        ingredients.add(item(id(Items.IRON_PICKAXE)));
        ingredients.add(item(id(Items.IRON_AXE)));
        ingredients.add(item(id(Items.IRON_SWORD)));
        json.add("ingredients", ingredients);

        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", id(ModItems.NOVICE_SPELL_STAFF.get().asItem()));
        json.add("result", result);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/novice_spell_staff.json"), json));
    }

    private void addMageStaffRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");
        json.addProperty("category", "misc");

        JsonArray ingredients = new JsonArray();
        ingredients.add(item(id(ModItems.NOVICE_SPELL_STAFF.get().asItem())));
        ingredients.add(tag("c:obsidians"));
        ingredients.add(tag("c:gems/diamond"));
        ingredients.add(tag("c:gems/diamond"));
        ingredients.add(tag("c:gems/diamond"));
        ingredients.add(item(id(Items.QUARTZ_BLOCK)));
        ingredients.add(item(id(Items.QUARTZ_BLOCK)));
        ingredients.add(tag("c:rods/blaze"));
        ingredients.add(tag("c:rods/blaze"));
        json.add("ingredients", ingredients);

        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", id(ModItems.MAGE_SPELL_STAFF.get().asItem()));
        json.add("result", result);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/mage_spell_staff.json"), json));
    }

    private void addArchmageStaffRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");
        json.addProperty("category", "misc");

        JsonArray ingredients = new JsonArray();
        ingredients.add(item(id(ModItems.MAGE_SPELL_STAFF.get().asItem())));
        ingredients.add(tag("c:ender_pearls"));
        ingredients.add(tag("c:ender_pearls"));
        ingredients.add(tag("c:ender_pearls"));
        ingredients.add(tag("c:gems/emerald"));
        ingredients.add(tag("c:gems/emerald"));
        ingredients.add(item(id(Items.TOTEM_OF_UNDYING)));
        ingredients.add(item(id(Items.NETHER_STAR)));
        ingredients.add(item("ars_nouveau:wilden_tribute"));
        json.add("ingredients", ingredients);

        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", id(ModItems.ARCHMAGE_SPELL_STAFF.get().asItem()));
        json.add("result", result);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/archmage_spell_staff.json"), json));
    }

    private static JsonObject item(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("item", id);
        return o;
    }

    private static JsonObject item(net.minecraft.resources.ResourceLocation id) {
        return item(id.toString());
    }

    private static JsonObject tag(String tag) {
        JsonObject o = new JsonObject();
        o.addProperty("tag", tag);
        return o;
    }

    private static String id(net.minecraft.world.item.Item item) {
        return ITEM.getKey(item).toString();
    }

    @Override
    public @NotNull String getName() {
        return "ArsZero: Staff Recipe Datagen";
    }

    Path resolvePath(String path) {
        return this.generator.getPackOutput().getOutputFolder().resolve(path);
    }

    public record FileObj(Path path, com.google.gson.JsonElement element) { }
}


