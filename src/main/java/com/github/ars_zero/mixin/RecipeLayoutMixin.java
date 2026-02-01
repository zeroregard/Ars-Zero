package com.github.ars_zero.mixin;

import com.hollingsworth.arsnouveau.client.jei.EnchantingApparatusRecipeCategory;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.gui.recipes.RecipeLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RecipeLayout.class, remap = false)
public class RecipeLayoutMixin {

    @Redirect(
        method = "drawRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lmezz/jei/api/recipe/category/IRecipeCategory;draw(Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V"
        ),
        remap = false
    )
    @SuppressWarnings("unchecked")
    private void arsZero$skipDrawWhenNotRecipeHolder(IRecipeCategory category, Object recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (category instanceof EnchantingApparatusRecipeCategory && !(recipe instanceof RecipeHolder)) {
            return;
        }
        category.draw(recipe, slotsView, guiGraphics, mouseX, mouseY);
    }
}
