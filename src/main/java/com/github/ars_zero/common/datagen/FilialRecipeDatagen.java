package com.github.ars_zero.common.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates:
 * <ul>
 *   <li>8 filial crafting recipes (type {@code ars_nouveau:enchanting_apparatus}) — one per school</li>
 *   <li>8 staff-filial embedding recipes (type {@code ars_zero:staff_filial}) — one per school</li>
 * </ul>
 * Reagent (centre block) is always {@code ars_nouveau:source_gem}.
 * Each recipe has exactly 4 pedestal items.
 */
public class FilialRecipeDatagen extends SimpleDataProvider {

    private final List<FileObj> files = new ArrayList<>();

    public FilialRecipeDatagen(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    public void collectJsons(CachedOutput pOutput) {
        // Filial creation recipes (ars_nouveau:enchanting_apparatus)
        // Pedestal order matches FILIALS.md ingredients.
        addFilialCreationRecipe("air_filial",
                "ars_nouveau:air_essence",
                "minecraft:phantom_membrane",
                "minecraft:feather",
                "minecraft:breeze_rod");

        addFilialCreationRecipe("earth_filial",
                "ars_nouveau:earth_essence",
                "minecraft:oak_log",
                "minecraft:emerald",
                "minecraft:moss_block");

        addFilialCreationRecipe("fire_filial",
                "ars_nouveau:fire_essence",
                "minecraft:lava_bucket",
                "minecraft:blaze_rod",
                "minecraft:netherite_ingot");

        addFilialCreationRecipe("water_filial",
                "ars_nouveau:water_essence",
                "minecraft:heart_of_the_sea",
                "minecraft:water_bucket",
                "minecraft:prismarine_shard");

        addFilialCreationRecipe("abjuration_filial",
                "ars_nouveau:abjuration_essence",
                "minecraft:quartz",
                "minecraft:shield",
                "minecraft:amethyst_shard");

        addFilialCreationRecipe("conjuration_filial",
                "ars_nouveau:conjuration_essence",
                "minecraft:diamond",
                "minecraft:totem_of_undying",
                "minecraft:egg");

        addFilialCreationRecipe("manipulation_filial",
                "ars_nouveau:manipulation_essence",
                "minecraft:redstone",
                "minecraft:ender_pearl",
                "minecraft:string");

        addFilialCreationRecipe("necromancy_filial",
                "ars_elemental:anima_essence",
                "minecraft:wither_skeleton_skull",
                "minecraft:bone",
                "minecraft:echo_shard");

        // Staff embedding recipes (ars_zero:staff_filial)
        addStaffFilialRecipe("air_filial");
        addStaffFilialRecipe("earth_filial");
        addStaffFilialRecipe("fire_filial");
        addStaffFilialRecipe("water_filial");
        addStaffFilialRecipe("abjuration_filial");
        addStaffFilialRecipe("conjuration_filial");
        addStaffFilialRecipe("manipulation_filial");
        addStaffFilialRecipe("necromancy_filial");

        for (FileObj fileObj : files) {
            saveStable(pOutput, fileObj.element(), fileObj.path());
        }
    }

    /**
     * Generates an {@code ars_nouveau:enchanting_apparatus} recipe that crafts the named filial.
     * The reagent (centre) is always a Source Gem; the four pedestal items are as specified.
     */
    private void addFilialCreationRecipe(String filialId,
                                         String pedestal1, String pedestal2,
                                         String pedestal3, String pedestal4) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:enchanting_apparatus");

        json.add("reagent", item("ars_nouveau:source_gem"));

        JsonArray pedestalItems = new JsonArray();
        pedestalItems.add(item(pedestal1));
        pedestalItems.add(item(pedestal2));
        pedestalItems.add(item(pedestal3));
        pedestalItems.add(item(pedestal4));
        json.add("pedestalItems", pedestalItems);

        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", "ars_zero:" + filialId);
        json.add("result", result);

        json.addProperty("sourceCost", 1000);
        json.addProperty("keepNbtOfReagent", false);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/create_" + filialId + ".json"), json));
    }

    /**
     * Generates an {@code ars_zero:staff_filial} recipe that embeds a filial into any staff.
     * The reagent (centre) is any item tagged {@code ars_zero:filial_staff_input}; the single
     * pedestal item is the filial itself.
     */
    private void addStaffFilialRecipe(String filialId) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_zero:staff_filial");

        json.add("reagent", tag("ars_zero:filial_staff_input"));

        JsonArray pedestalItems = new JsonArray();
        pedestalItems.add(item("ars_zero:" + filialId));
        json.add("pedestalItems", pedestalItems);

        json.addProperty("sourceCost", 500);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/embed_" + filialId + ".json"), json));
    }

    private static JsonObject item(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("item", id);
        return o;
    }

    private static JsonObject tag(String tagId) {
        JsonObject o = new JsonObject();
        o.addProperty("tag", tagId);
        return o;
    }

    @Override
    public @NotNull String getName() {
        return "ArsZero: Filial Recipe Datagen";
    }

    Path resolvePath(String path) {
        return this.generator.getPackOutput().getOutputFolder().resolve(path);
    }

    public record FileObj(Path path, com.google.gson.JsonElement element) {}
}
