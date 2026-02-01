package com.github.ars_zero.common.crafting.recipes;

import alexthw.ars_elemental.common.components.ElementProtectionFlag;
import com.github.ars_zero.registry.ModRecipes;
import alexthw.ars_elemental.registry.ModRegistry;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProtectionUpgradeRecipe extends EnchantingApparatusRecipe {

    public ProtectionUpgradeRecipe(Ingredient reagent, List<Ingredient> stacks, int cost) {
        super(reagent, ItemStack.EMPTY, stacks, cost, true);
    }

    @Override
    public boolean matches(ApparatusRecipeInput input, Level level) {
        ElementProtectionFlag flag = input.catalyst().get(ModRegistry.P4E);
        return super.matches(input, level) && (flag == null || !flag.flag());
    }

    @Override
    public @NotNull ItemStack assemble(ApparatusRecipeInput input, HolderLookup.@NotNull Provider lookup) {
        ItemStack temp = input.catalyst().copy();
        temp.set(ModRegistry.P4E, new ElementProtectionFlag(true));
        return temp;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.PROTECTION_UPGRADE_TYPE.get();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.PROTECTION_UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<ProtectionUpgradeRecipe> {

        public static final MapCodec<ProtectionUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("reagent").forGetter(ProtectionUpgradeRecipe::reagent),
                Ingredient.CODEC.listOf().fieldOf("pedestalItems").forGetter(ProtectionUpgradeRecipe::pedestalItems),
                Codec.INT.fieldOf("sourceCost").forGetter(ProtectionUpgradeRecipe::sourceCost)
        ).apply(instance, ProtectionUpgradeRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ProtectionUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                ProtectionUpgradeRecipe::reagent,
                ANCodecs.INGREDIENT_LIST_STREAM,
                ProtectionUpgradeRecipe::pedestalItems,
                ByteBufCodecs.VAR_INT,
                ProtectionUpgradeRecipe::sourceCost,
                ProtectionUpgradeRecipe::new
        );

        @Override
        public @NotNull MapCodec<ProtectionUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ProtectionUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
