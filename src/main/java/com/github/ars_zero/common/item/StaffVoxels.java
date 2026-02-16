package com.github.ars_zero.common.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.TelekinesisStaffRenderer;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.List;
import java.util.function.Consumer;

/**
 * QA testing staff: Begin = Near + Conjure Voxel + Conjure Water + Split,
 * Tick = Temporal Context + Anchor, End = Temporal Context + Explosion.
 * Covers ConjureVoxelEffect split voxels temporal context.
 */
public class StaffVoxels extends AbstractStaticSpellStaff {

    private static final String SPELL_NAME = "Staff of Voxels";

    public StaffVoxels() {
        super();
    }

    @Override
    public int getDiscountPercent() {
        return 0;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.ars_zero.staff_voxels.desc"));
    }

    @Override
    protected void applyPresetSpells(ItemStack stack) {
        AbstractSpellPart near = GlyphRegistry.getSpellPart(ArsZero.prefix("near_form"));
        AbstractSpellPart conjureVoxel = GlyphRegistry.getSpellPart(ArsZero.prefix("conjure_voxel_effect"));
        AbstractSpellPart split = GlyphRegistry.getSpellPart(ResourceLocation.parse("ars_nouveau:glyph_split"));
        AbstractSpellPart conjureWater = GlyphRegistry.getSpellPart(ResourceLocation.parse("ars_nouveau:glyph_conjure_water"));
        AbstractSpellPart temporalContext = GlyphRegistry.getSpellPart(ArsZero.prefix("temporal_context_form"));
        AbstractSpellPart anchor = GlyphRegistry.getSpellPart(ArsZero.prefix("anchor_effect"));
        AbstractSpellPart explosion = GlyphRegistry.getSpellPart(ResourceLocation.parse("ars_nouveau:glyph_explosion"));

        if (near == null || conjureVoxel == null || split == null || conjureWater == null || temporalContext == null || anchor == null || explosion == null) {
            return;
        }

        Spell beginSpell = new Spell(near, conjureVoxel, split, conjureWater).withName(SPELL_NAME);
        Spell tickSpell = new Spell(temporalContext, anchor).withName(SPELL_NAME);
        Spell endSpell = new Spell(temporalContext, explosion).withName(SPELL_NAME);

        SpellCaster caster = (SpellCaster) com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(stack);
        if (caster != null) {
            caster.setSpell(beginSpell, 0).setSpell(tickSpell, 1).setSpell(endSpell, 2).setCurrentSlot(0).saveToStack(stack);
            AbstractMultiPhaseCastDevice.setSlotTickDelay(stack, 0, 1);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!stack.isEmpty() && AbstractMultiPhaseCastDevice.getSlotTickDelay(stack, 0) != 1) {
            AbstractMultiPhaseCastDevice.setSlotTickDelay(stack, 0, 1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new TelekinesisStaffRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }
}
