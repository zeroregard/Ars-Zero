package com.github.ars_zero.common.item;

import com.github.ars_zero.common.config.ServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A filial item for a specific school of magic. When held in the offhand slot, grants a
 * configurable spell power bonus (from {@link ServerConfig#FILIAL_POWER_BONUS}) to the
 * corresponding SauceLib power attribute.
 *
 * <p>Filials can also be embedded into a staff via the Enchanting Apparatus
 * ({@code ars_zero:staff_filial} recipe), granting the same bonus when the staff is in the
 * mainhand. Both bonuses stack.
 */
public class FilialItem extends Item {

    /** CUSTOM_DATA CompoundTag key used to store the embedded filial school on a staff. */
    public static final String TAG_KEY = "ars_zero_filial_school";

    /** Maps school ID → power attribute Holder, populated by each FilialItem constructor. */
    private static final Map<String, Holder<Attribute>> SCHOOL_POWER_MAP = new HashMap<>();

    private final String schoolId;
    private final Holder<Attribute> powerAttribute;

    public FilialItem(String schoolId, Holder<Attribute> powerAttribute) {
        super(new Properties());
        this.schoolId = schoolId;
        this.powerAttribute = powerAttribute;
        SCHOOL_POWER_MAP.put(schoolId, powerAttribute);
    }

    public String getSchoolId() {
        return schoolId;
    }

    public Holder<Attribute> getPowerAttribute() {
        return powerAttribute;
    }

    /**
     * Grants power bonus when held in the offhand slot.
     */
    @Override
    public void getAttributeModifiers(ItemStack stack, Consumer<ItemAttributeModifiers.Entry> consumer) {
        int bonus = ServerConfig.FILIAL_POWER_BONUS.get();
        if (bonus > 0) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ars_zero", "filial_offhand_" + schoolId);
            consumer.accept(new ItemAttributeModifiers.Entry(
                powerAttribute,
                new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.OFFHAND
            ));
        }
    }

    // -------------------------------------------------------------------------
    // Static helpers for reading/writing the embedded filial school on a staff
    // -------------------------------------------------------------------------

    /**
     * Returns the school ID of the filial embedded in this staff stack, or {@code null} if none.
     */
    @Nullable
    public static String getStaffFilialSchool(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains(TAG_KEY) ? tag.getString(TAG_KEY) : null;
    }

    /**
     * Sets (or clears) the embedded filial school on a staff stack.
     */
    public static void setStaffFilialSchool(ItemStack stack, @Nullable String schoolId) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        if (schoolId == null) {
            tag.remove(TAG_KEY);
        } else {
            tag.putString(TAG_KEY, schoolId);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Returns the power attribute Holder for a given school ID, or {@code null} if unknown.
     * Populated when {@link FilialItem} instances are constructed during item registration.
     */
    @Nullable
    public static Holder<Attribute> getPowerForSchool(String schoolId) {
        return SCHOOL_POWER_MAP.get(schoolId);
    }
}
