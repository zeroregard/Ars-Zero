package com.github.ars_zero.common.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GlyphRecipeDatagen extends SimpleDataProvider {
    List<FileObj> files = new ArrayList<>();

    public GlyphRecipeDatagen(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    public void collectJsons(CachedOutput pOutput) {
        addSelectRecipe();
        addZeroGravityRecipe();
        addConjureVoxelRecipe();
        addAnchorRecipe();
        addEnlargeRecipe();
        addTemporalContextRecipe();
        addNearRecipe();
        addPushRecipe();

        for (FileObj fileObj : files) {
            saveStable(pOutput, fileObj.element, fileObj.path);
        }
    }

    private void addSelectRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 27);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("ars_nouveau:magebloom_fiber"));
        inputsArray.add(item("ars_zero:archwood_rod"));
        inputsArray.add(item("minecraft:amethyst_shard"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:select_effect");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_select_effect.json"), json));
    }

    private void addZeroGravityRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 55);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("minecraft:phantom_membrane"));
        inputsArray.add(item("minecraft:feather"));
        inputsArray.add(item("ars_nouveau:air_essence"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:zero_gravity_effect");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_zero_gravity_effect.json"), json));
    }

    private void addConjureVoxelRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 55);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("minecraft:clay"));
        inputsArray.add(item("minecraft:quartz"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:conjure_voxel_effect");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_conjure_voxel_effect.json"), json));
    }

    private void addAnchorRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 55);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("minecraft:compass"));
        inputsArray.add(item("minecraft:lodestone"));
        inputsArray.add(item("minecraft:ender_pearl"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:anchor_effect");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_anchor_effect.json"), json));
    }

    private void addEnlargeRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 27);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("minecraft:glow_ink_sac"));
        inputsArray.add(item("minecraft:golden_apple"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:enlarge_effect");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_enlarge_effect.json"), json));
    }

    private void addTemporalContextRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 27);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("minecraft:clock"));
        inputsArray.add(item("minecraft:redstone"));
        inputsArray.add(item("minecraft:repeater"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:temporal_context_form");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_temporal_context_form.json"), json));
    }

    private void addNearRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 27);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("minecraft:ender_pearl"));
        inputsArray.add(item("minecraft:sugar"));
        inputsArray.add(item("ars_nouveau:air_essence"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:near_form");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_near_form.json"), json));
    }

    private void addPushRecipe() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ars_nouveau:glyph");
        json.addProperty("exp", 27);

        JsonArray inputsArray = new JsonArray();
        inputsArray.add(item("ars_nouveau:air_essence"));
        inputsArray.add(item("minecraft:piston"));
        inputsArray.add(item("minecraft:redstone"));
        inputsArray.add(item("ars_nouveau:source_gem"));
        json.add("inputs", inputsArray);

        JsonObject outputObj = new JsonObject();
        outputObj.addProperty("count", 1);
        outputObj.addProperty("id", "ars_zero:push_effect");
        json.add("output", outputObj);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/glyph_push_effect.json"), json));
    }

    private static JsonObject item(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("item", id);
        return o;
    }

    @Override
    public @NotNull String getName() {
        return "ArsZero: Glyph Recipe Datagen";
    }

    Path resolvePath(String path) {
        return this.generator.getPackOutput().getOutputFolder().resolve(path);
    }

    public record FileObj(Path path, com.google.gson.JsonElement element) { }
}

