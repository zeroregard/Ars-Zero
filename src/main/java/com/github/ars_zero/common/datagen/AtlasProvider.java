package com.github.ars_zero.common.datagen;

import com.github.ars_zero.ArsZero;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AtlasProvider extends SpriteSourceProvider {
    
    public AtlasProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper helper) {
        super(output, provider, ArsZero.MOD_ID, helper);
    }

    @Override
    protected void gather() {
        ResourceLocation compositeHighlight = ResourceLocation.fromNamespaceAndPath(ArsZero.MOD_ID, "gui/composite_effect_highlight");
        // Register in GUI atlas for GUI textures (not blocks atlas)
        // GUI atlas location: minecraft:textures/atlas/gui.png
        ResourceLocation guiAtlasLocation = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/gui.png");
        this.atlas(guiAtlasLocation).addSource(new SingleFile(compositeHighlight, Optional.empty()));
    }
}

