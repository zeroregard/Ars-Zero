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
import net.neoforged.neoforge.registries.DeferredRegister;

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

    // Static staffs
    public static ItemRegistryWrapper<StaticStaff> STAFF_TELEKINESIS;
    public static ItemRegistryWrapper<StaticStaff> STAFF_DEMONBANE;
    public static ItemRegistryWrapper<StaticStaff> STAFF_VOXELS;
    public static ItemRegistryWrapper<StaticStaff> STAFF_GEOMETRIZE;
    public static ItemRegistryWrapper<StaticStaff> STAFF_CONVERGENCE;
    public static ItemRegistryWrapper<StaticStaff> STAFF_LAKES;

    private ModStaffItems() {
    }

    public static void register(DeferredRegister<Item> items) {
        NOVICE_SPELL_STAFF = wrap(items.register("novice_spell_staff", NoviceSpellStaff::new));
        MAGE_SPELL_STAFF = wrap(items.register("mage_spell_staff", MageSpellStaff::new));
        ARCHMAGE_SPELL_STAFF = wrap(items.register("archmage_spell_staff", ArchmageSpellStaff::new));
        CREATIVE_SPELL_STAFF = wrap(items.register("creative_spell_staff", CreativeSpellStaff::new));
        SPELLCASTING_CIRCLET = wrap(items.register("spellcasting_circlet", SpellcastingCirclet::new));

        STAFF_TELEKINESIS = wrap(items.register("staff_telekinesis", () -> new StaticStaff(staffTelekinesisConfig())));
        STAFF_DEMONBANE = wrap(items.register("staff_demonbane", () -> new StaticStaff(staffDemonbaneConfig())));
        STAFF_VOXELS = wrap(items.register("staff_voxels", () -> new StaticStaff(staffVoxelsConfig())));
        STAFF_GEOMETRIZE = wrap(items.register("staff_geometrize", () -> new StaticStaff(staffGeometrizeConfig())));
        STAFF_CONVERGENCE = wrap(items.register("staff_convergence", () -> new StaticStaff(staffConvergenceConfig())));
        STAFF_LAKES = wrap(items.register("staff_lakes", () -> new StaticStaff(staffLakesConfig())));
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
                .build();
    }

    private static StaticStaffConfig staffVoxelsConfig() {
        return StaticStaffConfig.builder("Staff of Voxels", "item.ars_zero.staff_voxels.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_zero:near_form", "ars_zero:conjure_voxel_effect", "ars_nouveau:glyph_split", "ars_nouveau:glyph_conjure_water")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:anchor_effect")
                .endSpell("ars_zero:temporal_context_form", "ars_nouveau:glyph_explosion")
                .tickDelay(1)
                .build();
    }

    private static StaticStaffConfig staffGeometrizeConfig() {
        return StaticStaffConfig.builder("Staff of Geometrize", "item.ars_zero.staff_geometrize.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_nouveau:glyph_projectile", "ars_zero:effect_geometrize", "ars_elemental:glyph_conjure_terrain")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:anchor_effect")
                .endSpell("ars_zero:temporal_context_form", "ars_nouveau:glyph_explosion")
                .tickDelay(1)
                .build();
    }

    private static StaticStaffConfig staffConvergenceConfig() {
        return StaticStaffConfig.builder("Staff of the Explosion Arch Wizard", "item.ars_zero.staff_convergence.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_nouveau:glyph_projectile", "ars_zero:effect_convergence", "ars_nouveau:glyph_explosion")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:sustain_effect")
                .endSpell()
                .tickDelay(1)
                .build();
    }

    private static StaticStaffConfig staffLakesConfig() {
        return StaticStaffConfig.builder("Staff of Lakes", "item.ars_zero.staff_lakes.desc")
                .renderer(TelekinesisStaffRenderer::new)
                .beginSpell("ars_nouveau:glyph_projectile", "ars_zero:effect_convergence", "ars_nouveau:glyph_conjure_water")
                .tickSpell("ars_zero:temporal_context_form", "ars_zero:sustain_effect")
                .endSpell()
                .tickDelay(1)
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
        registerStaff(STAFF_TELEKINESIS.get());
        registerStaff(STAFF_DEMONBANE.get());
        registerStaff(STAFF_VOXELS.get());
        registerStaff(STAFF_GEOMETRIZE.get());
        registerStaff(STAFF_CONVERGENCE.get());
        registerStaff(STAFF_LAKES.get());
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
