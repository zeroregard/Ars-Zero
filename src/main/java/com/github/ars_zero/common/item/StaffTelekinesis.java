package com.github.ars_zero.common.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.TelekinesisStaffRenderer;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.List;
import java.util.function.Consumer;

public class StaffTelekinesis extends AbstractStaticSpellStaff {

    private static final String SPELL_NAME = "Staff of Telekinesis";

    public StaffTelekinesis() {
        super();
    }

    @Override
    public int getDiscountPercent() {
        return 50;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.ars_zero.staff_telekinesis.desc"));
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
