package com.github.ars_zero.common.crafting.recipes;

import com.github.ars_zero.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

public class DyeRecipe extends ShapelessRecipe {

    public static final MapCodec<DyeRecipe> CODEC = ExtendableShapelessSerializer.createMap(DyeRecipe::new);

    public DyeRecipe(String groupIn, CraftingBookCategory category, ItemStack recipeOutputIn, NonNullList<Ingredient> recipeItemsIn) {
        super(groupIn, CraftingBookCategory.MISC, recipeOutputIn, recipeItemsIn);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput inv, HolderLookup.@NotNull Provider p_266797_) {
        ItemStack output = super.assemble(inv, p_266797_);
        if (!output.isEmpty()) {
            for (int i = 0; i < inv.size(); i++) {
                final ItemStack ingredient = inv.getItem(i);
                if (!ingredient.isEmpty() && ingredient.is(output.getItem())) {
                    output.applyComponents(ingredient.getComponentsPatch());
                }
            }
            for (int i = 0; i < inv.size(); i++) {
                final ItemStack ingredient = inv.getItem(i);
                DyeColor color = DyeColor.getColor(ingredient);
                if (!ingredient.isEmpty() && color != null) {
                    output.set(DataComponents.BASE_COLOR, color);
                }
            }
        }
        return output;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYE_RECIPE.get();
    }
    
    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.DYE_TYPE.get();
    }

}

