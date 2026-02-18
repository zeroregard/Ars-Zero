package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.crafting.recipes.DyeRecipe;
import com.github.ars_zero.common.crafting.recipes.ExtendableShapelessSerializer;
import com.github.ars_zero.common.crafting.recipes.ProtectionUpgradeRecipe;
import com.github.ars_zero.common.crafting.recipes.StaffUpgradeRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ArsZero.MOD_ID);
    
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, ArsZero.MOD_ID);

    public static final String DYE_RECIPE_ID = "dye";

    public static final DeferredHolder<RecipeType<?>, ModRecipeType<DyeRecipe>> DYE_TYPE = 
        RECIPE_TYPES.register(DYE_RECIPE_ID, () -> new ModRecipeType<>());
    
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DyeRecipe>> DYE_RECIPE =
        RECIPE_SERIALIZERS.register(DYE_RECIPE_ID, () -> ExtendableShapelessSerializer.create(DyeRecipe::new));

    public static final String STAFF_UPGRADE_ID = "staff_upgrade";
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StaffUpgradeRecipe>> STAFF_UPGRADE_RECIPE =
        RECIPE_SERIALIZERS.register(STAFF_UPGRADE_ID, () -> ExtendableShapelessSerializer.create(StaffUpgradeRecipe::new));

    public static final String PROTECTION_UPGRADE_ID = "protection_upgrade";
    public static final DeferredHolder<RecipeType<?>, RecipeType<ProtectionUpgradeRecipe>> PROTECTION_UPGRADE_TYPE =
        RECIPE_TYPES.register(PROTECTION_UPGRADE_ID, () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(ArsZero.MOD_ID, PROTECTION_UPGRADE_ID)));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProtectionUpgradeRecipe>> PROTECTION_UPGRADE_SERIALIZER =
        RECIPE_SERIALIZERS.register(PROTECTION_UPGRADE_ID, ProtectionUpgradeRecipe.Serializer::new);

    public static class ModRecipeType<T extends Recipe<?>> implements RecipeType<T> {
        @Override
        public String toString() {
            return BuiltInRegistries.RECIPE_TYPE.getKey(this).toString();
        }
    }
}
