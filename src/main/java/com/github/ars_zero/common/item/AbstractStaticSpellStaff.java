package com.github.ars_zero.common.item;

import com.github.ars_zero.client.gui.StaticStaffScreen;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.client.renderer.item.StaticSpellStaffRenderer;
import com.hollingsworth.arsnouveau.api.mana.IManaDiscountEquipment;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Abstract base for preprogrammed staffs with a single fixed spell slot (three phases: Begin, Tick, End).
 * Spells cannot be altered by the player. Not obtainable as an item; use concrete subclasses (e.g. StaffTelekinesis).
 */
public abstract class AbstractStaticSpellStaff extends AbstractStaff implements IManaDiscountEquipment {

    public AbstractStaticSpellStaff() {
        super(SpellTier.ONE);
    }

    /**
     * Percentage discount applied to spell cost (0–100). Subclasses override for preset staffs.
     * Used for tooltip display (e.g. mana indicator) so the shown percentage matches the design.
     */
    public int getDiscountPercent() {
        return 0;
    }

    @Override
    public int getManaDiscount(ItemStack stack, Spell spell) {
        int pct = getDiscountPercent();
        if (pct <= 0) return 0;
        return (int) Math.round(spell.getCost() * Math.min(100, pct) / 100.0);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** Spell name for the three phases. */
    protected abstract String getSpellName();

    /** Resource IDs for begin, tick, end phases. Each inner array lists glyph IDs (e.g. "ars_zero:effect_beam"). */
    protected abstract String[][] getPresetSpellIds();

    /** Tick delay for slot 0. Default 1. */
    protected int getPresetSlotTickDelay() {
        return 1;
    }

    /**
     * Called when the staff is crafted and from {@link #ensurePresetSpells(ItemStack)} when
     * the stack has no spells in the static slot. Builds spells from {@link #getPresetSpellIds()} if available.
     */
    protected void applyPresetSpells(ItemStack stack) {
        String[][] ids = getPresetSpellIds();
        if (ids == null || ids.length < 3) return;

        Spell[] spells = new Spell[3];
        for (int phase = 0; phase < 3; phase++) {
            if (ids[phase] == null || ids[phase].length == 0) {
                spells[phase] = new Spell().withName(getSpellName());
                continue;
            }
            List<AbstractSpellPart> parts = new ArrayList<>();
            for (String id : ids[phase]) {
                AbstractSpellPart part = GlyphRegistry.getSpellPart(ResourceLocation.parse(id));
                if (part == null) return;
                parts.add(part);
            }
            spells[phase] = new Spell(parts.toArray(AbstractSpellPart[]::new)).withName(getSpellName());
        }

        SpellCaster caster = (SpellCaster) SpellCasterRegistry.from(stack);
        if (caster != null) {
            caster.setSpell(spells[0], 0).setSpell(spells[1], 1).setSpell(spells[2], 2).setCurrentSlot(0).saveToStack(stack);
            AbstractMultiPhaseCastDevice.setSlotTickDelay(stack, 0, getPresetSlotTickDelay());
        }
    }

    /**
     * If this stack has no spells in the static slot (0,1,2), calls {@link #applyPresetSpells(ItemStack)}.
     * Use from inventoryTick or when giving the item so creative-given stacks get preset spells.
     */
    protected final void ensurePresetSpells(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractStaticSpellStaff)) {
            return;
        }
        com.hollingsworth.arsnouveau.api.spell.AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            return;
        }
        boolean empty = caster.getSpell(0).isEmpty() && caster.getSpell(1).isEmpty() && caster.getSpell(2).isEmpty();
        if (empty) {
            applyPresetSpells(stack);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected Screen createDeviceScreen(ItemStack stack, InteractionHand hand) {
        return new StaticStaffScreen(stack, hand);
    }

    @Override
    public void onNextKeyPressed(ItemStack stack, ServerPlayer player) {
        // No-op: static staff has only one slot, do not switch.
    }

    @Override
    public void onPreviousKeyPressed(ItemStack stack, ServerPlayer player) {
        // No-op: static staff has only one slot, do not switch.
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onRadialKeyPressed(ItemStack stack, Player player) {
        // No-op: static staff has only one slot; V (radial) should not open any interface.
    }

    @Override
    public boolean onScribe(Level world, BlockPos pos, Player player, InteractionHand handIn, ItemStack thisStack) {
        // Static staff cannot be used to create parchments from its spells.
        return false;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        ensurePresetSpells(stack);
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster != null) {
            caster.setCurrentSlot(0);
            caster.saveToStack(stack);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        ensurePresetSpells(stack);
        if (!stack.isEmpty() && AbstractMultiPhaseCastDevice.getSlotTickDelay(stack, 0) != getPresetSlotTickDelay()) {
            AbstractMultiPhaseCastDevice.setSlotTickDelay(stack, 0, getPresetSlotTickDelay());
        }
        ensureDefaultDye(stack);
    }

    /** If this staff has a default dye, apply it to the stack. Override {@link #getDefaultDyeColor()} to provide one. */
    protected void ensureDefaultDye(ItemStack stack) {
        var dye = getDefaultDyeColor();
        if (dye != null) {
            stack.set(DataComponents.BASE_COLOR, dye);
        }
    }

    /** Default dye color for this staff, or null to use the item's default. */
    protected net.minecraft.world.item.DyeColor getDefaultDyeColor() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new StaticSpellStaffRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }
}
