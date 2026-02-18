package com.github.ars_zero.common.crafting.recipes;

import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

/**
 * Shapeless recipe that retains staff data (spell clipboard, casting style, etc.) when upgrading
 * from one staff tier to the next, similar to how Ars Nouveau's BookUpgradeRecipe retains
 * spell book data.
 */
public class StaffUpgradeRecipe extends ShapelessRecipe {

    public StaffUpgradeRecipe(String group, CraftingBookCategory category, ItemStack recipeOutput, NonNullList<Ingredient> ingredients) {
        super(group, category, recipeOutput, ingredients);
    }

    @Override
    public @NotNull ItemStack assemble(final @NotNull CraftingInput inv, HolderLookup.@NotNull Provider registries) {
        ItemStack output = super.assemble(inv, registries);

        if (!output.isEmpty()) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack ingredient = inv.getItem(i);
                if (!ingredient.isEmpty() && ingredient.getItem() instanceof AbstractSpellStaff) {
                    output.applyComponents(ingredient.getComponentsPatch());
                    break;
                }
            }
        }

        return output;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.STAFF_UPGRADE_RECIPE.get();
    }
}
