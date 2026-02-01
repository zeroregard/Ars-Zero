package com.github.ars_zero.mixin;

import com.hollingsworth.arsnouveau.client.jei.EnchantingApparatusRecipeCategory;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnchantingApparatusRecipeCategory.class, remap = false)
public class EnchantingApparatusRecipeCategoryMixin {

    @Inject(
        method = "draw",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void arsZero$guardDraw(RecipeHolder<?> recipeHolder, IRecipeSlotsView slotsView, GuiGraphics guiGraphics, double mouseX, double mouseY, CallbackInfo ci) {
        if (!(recipeHolder instanceof RecipeHolder)) {
            ci.cancel();
        }
    }
}
