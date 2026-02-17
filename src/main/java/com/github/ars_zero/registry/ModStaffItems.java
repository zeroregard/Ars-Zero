package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.TelekinesisStaffRenderer;
import com.github.ars_zero.common.item.AbstractStaff;
import com.github.ars_zero.common.item.ArchmageSpellStaff;
import com.github.ars_zero.common.item.CreativeSpellStaff;
import com.github.ars_zero.common.item.MageSpellStaff;
import com.github.ars_zero.common.item.NoviceSpellStaff;
import com.github.ars_zero.common.item.StaticStaff;
import com.github.ars_zero.common.item.StaticStaffConfig;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemRegistryWrapper;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registration for spell staffs and static staffs. Call {@link #register(DeferredRegister)}
 * from ModItems to register all staff items.
 */
public final class ModStaffItems {

    // Spell staffs
    public static ItemRegistryWrapper<NoviceSpellStaff> NOVICE_SPELL_STAFF;
    public static ItemRegistryWrapper<MageSpellStaff> MAGE_SPELL_STAFF;
    public static ItemRegistryWrapper<ArchmageSpellStaff> ARCHMAGE_SPELL_STAFF;
    public static ItemRegistryWrapper<CreativeSpellStaff> CREATIVE_SPELL_STAFF;
    public static ItemRegistryWrapper<SpellcastingCirclet> SPELLCASTING_CIRCLET;

    // Static staffs (TELEKINESIS always; others devOnly)
    public static ItemRegistryWrapper<StaticStaff> STAFF_TELEKINESIS;
    /** All static staffs that are registered (TELEKINESIS + dev staffs when !production). */
    private static final List<ItemRegistryWrapper<StaticStaff>> REGISTERED_STATIC_STAFFS = new ArrayList<>();

    private ModStaffItems() {
    }

    public static void register(DeferredRegister<Item> items) {
        NOVICE_SPELL_STAFF = wrap(items.register("novice_spell_staff", NoviceSpellStaff::new));
        MAGE_SPELL_STAFF = wrap(items.register("mage_spell_staff", MageSpellStaff::new));
        ARCHMAGE_SPELL_STAFF = wrap(items.register("archmage_spell_staff", ArchmageSpellStaff::new));
        CREATIVE_SPELL_STAFF = wrap(items.register("creative_spell_staff", CreativeSpellStaff::new));
        SPELLCASTING_CIRCLET = wrap(items.register("spellcasting_circlet", SpellcastingCirclet::new));

        STAFF_TELEKINESIS = registerStaticStaff(items, "staff_telekinesis", staffTelekinesisConfig());
        registerStaticStaff(items, "staff_demonbane", staffDemonbaneConfig());
        registerStaticStaff(items, "staff_voxels", staffVoxelsConfig());
        registerStaticStaff(items, "staff_geometrize", staffGeometrizeConfig());
        registerStaticStaff(items, "staff_convergence", staffConvergenceConfig());
        registerStaticStaff(items, "staff_lakes", staffLakesConfig());
        registerStaticStaff(items, "staff_switcheroo", staffSwitcherooConfig());
    }

    private static ItemRegistryWrapper<StaticStaff> registerStaticStaff(DeferredRegister<Item> items, String name, StaticStaffConfig config) {
        if (config.devOnly() && FMLEnvironment.production) {
            return null;
        }
        ItemRegistryWrapper<StaticStaff> holder = wrap(items.register(name, () -> new StaticStaff(config)));
        REGISTERED_STATIC_STAFFS.add(holder);
        return holder;
    }

    /** Static staffs to show in creative tab and register with SpellCasterRegistry. */
    public static List<ItemRegistryWrapper<StaticStaff>> getRegisteredStaticStaffs() {
        return REGISTERED_STATIC_STAFFS;
    }

    private static StaticStaffConfig staffTelekinesisConfig() {
        return StaticStaffConfig.builder("Staff of Telekinesis", "item.ars_zero.staff_telekinesis.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_nouveau:glyph_projectile", "ars_zero:select_effect")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:anchor_effect")
                .endSpell("ars_zero:temporal_context_form", "ars_zero:push_effect")
                .tickDelay(1)
                .discountPercent(50)
                .build();
    }

    private static StaticStaffConfig staffDemonbaneConfig() {
        return StaticStaffConfig.builder("Staff of Demonbane", "item.ars_zero.staff_demonbane.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_zero:near_form", "ars_nouveau:glyph_delay", "ars_nouveau:glyph_amplify", "ars_zero:effect_beam", "ars_nouveau:glyph_split", "ars_nouveau:glyph_amplify", "ars_nouveau:glyph_amplify", "ars_nouveau:glyph_amplify")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:anchor_effect")
                .endSpell()
                .tickDelay(1)
                .devOnly()
                .build();
    }

    private static StaticStaffConfig staffVoxelsConfig() {
        return StaticStaffConfig.builder("Staff of Voxels", "item.ars_zero.staff_voxels.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_zero:near_form", "ars_zero:conjure_voxel_effect", "ars_nouveau:glyph_split", "ars_nouveau:glyph_conjure_water")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:anchor_effect")
                .endSpell("ars_zero:temporal_context_form", "ars_nouveau:glyph_explosion")
                .tickDelay(1)
                .devOnly()
                .build();
    }

    private static StaticStaffConfig staffGeometrizeConfig() {
        return StaticStaffConfig.builder("Staff of Geometrize", "item.ars_zero.staff_geometrize.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_nouveau:glyph_projectile", "ars_zero:effect_geometrize", "ars_elemental:glyph_conjure_terrain")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:anchor_effect")
                .endSpell("ars_zero:temporal_context_form")
                .tickDelay(1)
                .devOnly()
                .build();
    }

    private static StaticStaffConfig staffConvergenceConfig() {
        return StaticStaffConfig.builder("Staff of the Explosion Arch Wizard", "item.ars_zero.staff_convergence.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_nouveau:glyph_projectile", "ars_zero:effect_convergence", "ars_nouveau:glyph_explosion")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:sustain_effect")
                .endSpell()
                .tickDelay(1)
                .devOnly()
                .build();
    }

    private static StaticStaffConfig staffLakesConfig() {
        return StaticStaffConfig.builder("Staff of Lakes", "item.ars_zero.staff_lakes.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_nouveau:glyph_projectile", "ars_zero:effect_convergence", "ars_nouveau:glyph_conjure_water")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:sustain_effect")
                .endSpell()
                .tickDelay(1)
                .devOnly()
                .build();
    }

    private static StaticStaffConfig staffSwitcherooConfig() {
        return StaticStaffConfig.builder("Staff of Switcheroo", "item.ars_zero.staff_switcheroo.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_zero:near_form", "ars_zero:conjure_voxel_effect")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:anchor_effect", "ars_nouveau:glyph_extract")
                .endSpell("ars_zero:temporal_context_form", "ars_nouveau:glyph_exchange", "ars_zero:discard_effect")
                .tickDelay(1)
                .devOnly()
                .build();
    }

    private static <T extends Item> ItemRegistryWrapper<T> wrap(net.neoforged.neoforge.registries.DeferredHolder<Item, T> holder) {
        return new ItemRegistryWrapper<>(holder);
    }

    public static void registerSpellCasters() {
        ArsZero.LOGGER.debug("Registering Ars Zero staffs with SpellCasterRegistry");
        registerStaff(NOVICE_SPELL_STAFF.get());
        registerStaff(MAGE_SPELL_STAFF.get());
        registerStaff(ARCHMAGE_SPELL_STAFF.get());
        registerStaff(CREATIVE_SPELL_STAFF.get());
        for (ItemRegistryWrapper<StaticStaff> holder : REGISTERED_STATIC_STAFFS) {
            registerStaff(holder.get());
        }
        registerDevice(SPELLCASTING_CIRCLET.get());
        ArsZero.LOGGER.debug("SpellCasterRegistry registration completed");
    }

    private static void registerStaff(AbstractStaff staff) {
        SpellCasterRegistry.register(staff, stack -> stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER));
    }

    private static void registerDevice(AbstractMultiPhaseCastDevice device) {
        SpellCasterRegistry.register(device, stack -> stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER));
    }
}
