package com.github.ars_zero.client.jei;

import alexthw.ars_elemental.common.components.ElementProtectionFlag;
import alexthw.ars_elemental.registry.ModRegistry;
import com.github.ars_zero.common.crafting.recipes.ProtectionUpgradeRecipe;
import com.hollingsworth.arsnouveau.client.jei.EnchantingApparatusRecipeCategory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProtectionUpgradeRecipeCategory extends EnchantingApparatusRecipeCategory<ProtectionUpgradeRecipe> {

    public ProtectionUpgradeRecipeCategory(IGuiHelper helper) {
        super(helper);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<ProtectionUpgradeRecipe>> getRecipeType() {
        return ArsZeroJEIPlugin.PROTECTION_UPGRADE_TYPE.get();
    }

    @Override
    public Component getTitle() {
        return Component.translatable("tooltip.ars_nouveau.blessed");
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProtectionUpgradeRecipe> holder, IFocusGroup focuses) {
        ProtectionUpgradeRecipe recipe = holder.value();
        List<Ingredient> inputs = recipe.pedestalItems();
        double angleBetweenEach = 360.0 / inputs.size();
        point = new Vec2(48, 13);
        List<ItemStack> stacks = List.of(recipe.reagent().getItems());
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 45).addItemStacks(stacks);
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack input : stacks) {
            ItemStack temp = input.copy();
            temp.set(ModRegistry.P4E, new ElementProtectionFlag(true));
            outputs.add(temp);
        }
        for (Ingredient input : inputs) {
            builder.addSlot(RecipeIngredientRole.INPUT, (int) point.x, (int) point.y)
                    .addIngredients(input);
            point = rotatePointAbout(point, center, angleBetweenEach);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 10).addIngredients(VanillaTypes.ITEM_STACK, outputs);
    }
}
