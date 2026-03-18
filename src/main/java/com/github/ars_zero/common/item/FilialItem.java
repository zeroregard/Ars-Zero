package com.github.ars_zero.common.item;

import com.github.ars_zero.client.renderer.model.AnimatedFilialGeoModel;
import com.github.ars_zero.client.renderer.model.StaticFilialGeoModel;
import com.github.ars_zero.common.config.ServerConfig;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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
public class FilialItem extends Item implements GeoItem {

    /** CUSTOM_DATA CompoundTag key used to store the embedded filial school on a staff. */
    public static final String TAG_KEY = "ars_zero_filial_school";

    /** Maps school ID → power attribute Holder, populated by each FilialItem constructor. */
    private static final Map<String, Holder<Attribute>> SCHOOL_POWER_MAP = new HashMap<>();

    /** Maps school ID → FilialItem instance, populated by each FilialItem constructor. */
    private static final Map<String, FilialItem> SCHOOL_ITEM_MAP = new HashMap<>();

    private final String schoolId;
    private final Holder<Attribute> powerAttribute;
    /** Looping animation name, or {@code null} for static filials. */
    @Nullable private final String animationName;
    private boolean spinOnStaff = true;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FilialItem(String schoolId, Holder<Attribute> powerAttribute, @Nullable String animationName) {
        super(new Properties().stacksTo(1));
        this.schoolId = schoolId;
        this.powerAttribute = powerAttribute;
        this.animationName = animationName;
        SCHOOL_POWER_MAP.put(schoolId, powerAttribute);
        SCHOOL_ITEM_MAP.put(schoolId, this);
    }

    public FilialItem noSpin() {
        this.spinOnStaff = false;
        return this;
    }

    public boolean shouldSpinOnStaff() {
        return spinOnStaff;
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
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers base = super.getDefaultAttributeModifiers(stack);
        int bonus = ServerConfig.FILIAL_POWER_BONUS.get();
        if (bonus > 0) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ars_zero", "filial_offhand_" + schoolId);
            base = base.withModifierAdded(
                powerAttribute,
                new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.OFFHAND
            );
        }
        return base;
    }

    // -------------------------------------------------------------------------
    // GeoItem — animation + rendering
    // -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (animationName != null) {
            controllers.add(new AnimationController<>(this, "spin", 0, state ->
                    state.setAndContinue(RawAnimation.begin().thenLoop(animationName))));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer =
                    new com.github.ars_zero.client.renderer.item.FilialItemRenderer(
                            animationName != null ? new AnimatedFilialGeoModel() : new StaticFilialGeoModel());

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
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

    /**
     * Returns the {@link FilialItem} instance for a given school ID, or {@code null} if unknown.
     * Populated when {@link FilialItem} instances are constructed during item registration.
     */
    @Nullable
    public static FilialItem getItemForSchool(String schoolId) {
        return SCHOOL_ITEM_MAP.get(schoolId);
    }
}
