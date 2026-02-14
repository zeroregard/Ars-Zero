package com.github.ars_zero.common.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.Simple2DStaffRenderer;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Wand "Wand of Telekinesis" — Begin: select target (projectile + select);
 * Tick: hold in place (temporal context + anchor), delay 0; End: throw (temporal context + push).
 * Wands cannot be customized. Renders as a simple 2D texture. 50% mana discount.
 */
public class WandTelekinesis extends AbstractStaticSpellStaff {

    private static final String SPELL_NAME = "Wand of Telekinesis";

    public WandTelekinesis() {
        super();
    }

    @Override
    protected int getDiscountPercent() {
        return 50;
    }

    @Override
    protected boolean addDiscountToTooltip() {
        return false; // desc already says "50% discount"
    }

    @Override
    protected void applyPresetSpells(ItemStack stack) {
        AbstractSpellPart projectile = GlyphRegistry.getSpellPart(ResourceLocation.parse("ars_nouveau:glyph_projectile"));
        AbstractSpellPart selectEffect = GlyphRegistry.getSpellPart(ArsZero.prefix("select_effect"));
        AbstractSpellPart temporalContext = GlyphRegistry.getSpellPart(ArsZero.prefix("temporal_context_form"));
        AbstractSpellPart anchorEffect = GlyphRegistry.getSpellPart(ArsZero.prefix("anchor_effect"));
        AbstractSpellPart pushEffect = GlyphRegistry.getSpellPart(ArsZero.prefix("push_effect"));

        if (projectile == null || selectEffect == null || temporalContext == null || anchorEffect == null || pushEffect == null) {
            return;
        }

        Spell beginSpell = new Spell(projectile, selectEffect).withName(SPELL_NAME);
        Spell tickSpell = new Spell(temporalContext, anchorEffect).withName(SPELL_NAME);
        Spell endSpell = new Spell(temporalContext, pushEffect).withName(SPELL_NAME);

        SpellCaster caster = (SpellCaster) SpellCasterRegistry.from(stack);
        if (caster != null) {
            caster.setSpell(beginSpell, 0).setSpell(tickSpell, 1).setSpell(endSpell, 2).setCurrentSlot(0).saveToStack(stack);
            AbstractMultiPhaseCastDevice.setSlotTickDelay(stack, 0, 1); // tick phase: 1 tick delay (logical slot 0)
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new Simple2DStaffRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }
}
