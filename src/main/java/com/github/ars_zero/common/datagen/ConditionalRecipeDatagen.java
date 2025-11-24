package com.github.ars_zero.common.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConditionalRecipeDatagen extends SimpleDataProvider {
    List<FileObj> files = new ArrayList<>();

    public ConditionalRecipeDatagen(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    public void collectJsons(CachedOutput pOutput) {
        addFusionMarkRecipe();

        for (FileObj fileObj : files) {
            saveStable(pOutput, fileObj.element, fileObj.path);
        }
    }

    private void addFusionMarkRecipe() {
        JsonObject json = new JsonObject();
        
        JsonArray conditions = new JsonArray();
        
        JsonObject condition1 = new JsonObject();
        condition1.addProperty("type", "neoforge:mod_loaded");
        condition1.addProperty("modid", "ars_technica");
        conditions.add(condition1);
        
        JsonObject condition2 = new JsonObject();
        condition2.addProperty("type", "neoforge:mod_loaded");
        condition2.addProperty("modid", "ars_affinity");
        conditions.add(condition2);
        
        json.add("neoforge:conditions", conditions);
        
        json.addProperty("type", "ars_nouveau:imbuement");

        JsonObject input = new JsonObject();
        input.addProperty("item", "ars_technica:blank_disc");
        json.add("input", input);

        JsonArray pedestalItems = new JsonArray();
        pedestalItems.add(item("ars_technica:calibrated_precision_mechanism"));
        pedestalItems.add(item("ars_affinity:ritual_amnesia"));
        pedestalItems.add(item("ars_zero:archmage_spell_staff"));
        json.add("pedestalItems", pedestalItems);

        JsonObject output = new JsonObject();
        output.addProperty("count", 1);
        output.addProperty("id", "ars_zero:fusion_record");
        json.add("output", output);

        json.addProperty("source", 10000);

        files.add(new FileObj(resolvePath("data/ars_zero/recipe/fusion_record.json"), json));
    }

    private static JsonObject item(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("item", id);
        return o;
    }

    @Override
    public @NotNull String getName() {
        return "ArsZero: Conditional Recipe Datagen";
    }

    Path resolvePath(String path) {
        return this.generator.getPackOutput().getOutputFolder().resolve(path);
    }

    public record FileObj(Path path, com.google.gson.JsonElement element) { }
}

