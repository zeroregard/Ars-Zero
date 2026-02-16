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
 * QA testing staff: Begin = Touch + Geometrize + Conjure Terrain,
 * Tick = Temporal Context + Anchor, End = Temporal Context + Explosion.
 * Covers EffectGeometrize + Conjure Terrain temporal context.
 */
public class StaffGeometrize extends AbstractStaticSpellStaff {

    private static final String SPELL_NAME = "Staff of Geometrize";

    public StaffGeometrize() {
        super();
    }

    @Override
    public int getDiscountPercent() {
        return 0;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.ars_zero.staff_geometrize.desc"));
    }

    @Override
    protected void applyPresetSpells(ItemStack stack) {
        AbstractSpellPart touch = GlyphRegistry.getSpellPart(ResourceLocation.parse("ars_nouveau:glyph_touch"));
        AbstractSpellPart geometrize = GlyphRegistry.getSpellPart(ArsZero.prefix("effect_geometrize"));
        AbstractSpellPart conjureTerrain = GlyphRegistry.getSpellPart(ResourceLocation.parse("ars_elemental:glyph_conjure_terrain"));
        AbstractSpellPart temporalContext = GlyphRegistry.getSpellPart(ArsZero.prefix("temporal_context_form"));
        AbstractSpellPart anchor = GlyphRegistry.getSpellPart(ArsZero.prefix("anchor_effect"));
        AbstractSpellPart explosion = GlyphRegistry.getSpellPart(ResourceLocation.parse("ars_nouveau:glyph_explosion"));

        if (touch == null || geometrize == null || conjureTerrain == null || temporalContext == null || anchor == null || explosion == null) {
            return;
        }

        Spell beginSpell = new Spell(touch, geometrize, conjureTerrain).withName(SPELL_NAME);
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
