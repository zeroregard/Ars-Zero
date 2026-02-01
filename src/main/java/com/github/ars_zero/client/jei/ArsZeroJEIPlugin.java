package com.github.ars_zero.client.jei;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.crafting.recipes.DyeRecipe;
import com.github.ars_zero.common.crafting.recipes.ProtectionUpgradeRecipe;
import com.github.ars_zero.registry.ModRecipes;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static mezz.jei.api.recipe.RecipeType.createFromDeferredVanilla;

@JeiPlugin
public class ArsZeroJEIPlugin implements IModPlugin {

    public static final Supplier<RecipeType<RecipeHolder<ProtectionUpgradeRecipe>>> PROTECTION_UPGRADE_TYPE =
            createFromDeferredVanilla(ModRecipes.PROTECTION_UPGRADE_TYPE);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ArsZero.prefix("jei_plugin");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(DyeRecipe.class, new DyeRecipeCategory());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ProtectionUpgradeRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RecipeManager manager = level.getRecipeManager();
        var recipes = manager.getAllRecipesFor(ModRecipes.PROTECTION_UPGRADE_TYPE.get());
        registration.addRecipes(PROTECTION_UPGRADE_TYPE.get(), recipes.stream().toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(BlockRegistry.ENCHANTING_APP_BLOCK.get()), PROTECTION_UPGRADE_TYPE.get());
    }
}





