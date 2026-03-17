package com.github.ars_zero.common.crafting.recipes;

import com.github.ars_zero.common.item.FilialItem;
import com.github.ars_zero.registry.ModRecipes;
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

/**
 * Enchanting Apparatus recipe for embedding a filial into a staff.
 *
 * <ul>
 *   <li>Catalyst (centre): any item tagged {@code ars_zero:filial_staff_input} (staves/circlet)</li>
 *   <li>Pedestal items: the specific {@link FilialItem} to embed</li>
 *   <li>Output: the staff with {@link FilialItem#TAG_KEY} set to the filial's school ID</li>
 * </ul>
 *
 * If the staff already has a filial, it is replaced.
 */
public class StaffFilialRecipe extends EnchantingApparatusRecipe {

    public StaffFilialRecipe(Ingredient reagent, List<Ingredient> pedestalItems, int sourceCost) {
        super(reagent, ItemStack.EMPTY, pedestalItems, sourceCost, true);
    }

    @Override
    public boolean matches(ApparatusRecipeInput input, Level level) {
        if (!super.matches(input, level)) return false;
        // Ensure at least one pedestal item is a FilialItem (safety check)
        return input.pedestals().stream().anyMatch(s -> s.getItem() instanceof FilialItem);
    }

    @Override
    public @NotNull ItemStack assemble(ApparatusRecipeInput input, HolderLookup.@NotNull Provider lookup) {
        ItemStack staffCopy = input.catalyst().copy();
        // Find the FilialItem in the pedestal items and embed its school
        for (ItemStack pedestal : input.pedestals()) {
            if (pedestal.getItem() instanceof FilialItem filial) {
                FilialItem.setStaffFilialSchool(staffCopy, filial.getSchoolId());
                break;
            }
        }
        return staffCopy;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.STAFF_FILIAL_TYPE.get();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.STAFF_FILIAL_SERIALIZER.get();
    }

    // -------------------------------------------------------------------------
    // Serializer (same pattern as ProtectionUpgradeRecipe)
    // -------------------------------------------------------------------------

    public static class Serializer implements RecipeSerializer<StaffFilialRecipe> {

        public static final MapCodec<StaffFilialRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Ingredient.CODEC.fieldOf("reagent").forGetter(StaffFilialRecipe::reagent),
                Ingredient.CODEC.listOf().fieldOf("pedestalItems").forGetter(StaffFilialRecipe::pedestalItems),
                Codec.INT.fieldOf("sourceCost").forGetter(StaffFilialRecipe::sourceCost)
            ).apply(instance, StaffFilialRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, StaffFilialRecipe> STREAM_CODEC =
            StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                StaffFilialRecipe::reagent,
                ANCodecs.INGREDIENT_LIST_STREAM,
                StaffFilialRecipe::pedestalItems,
                ByteBufCodecs.VAR_INT,
                StaffFilialRecipe::sourceCost,
                StaffFilialRecipe::new
            );

        @Override
        public @NotNull MapCodec<StaffFilialRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, StaffFilialRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
