package com.github.ars_zero.common.item;

import com.github.ars_zero.client.gui.StaticStaffScreen;
import com.github.ars_zero.client.renderer.item.StaticSpellStaffRenderer;
import com.hollingsworth.arsnouveau.api.mana.IManaDiscountEquipment;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
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

import java.util.List;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Abstract base for preprogrammed staffs with a single fixed spell slot (three phases: Begin, Tick, End).
 * Spells cannot be altered by the player. Not obtainable as an item; use concrete subclasses (e.g. WandTelekinesis).
 */
public abstract class AbstractStaticSpellStaff extends AbstractSpellStaff implements IManaDiscountEquipment {

    public AbstractStaticSpellStaff() {
        super(SpellTier.ONE);
    }

    /**
     * Percentage discount applied to spell cost (0–100). Subclasses override for preset staffs.
     */
    protected int getDiscountPercent() {
        return 0;
    }

    @Override
    public int getManaDiscount(ItemStack stack, Spell spell) {
        int pct = getDiscountPercent();
        if (pct <= 0) return 0;
        return (int) Math.round(spell.getCost() * Math.min(100, pct) / 100.0);
    }

    /** If true, a separate "Reduces spell cost by X%." line is added. Subclasses (e.g. wands with discount in desc) can override to false. */
    protected boolean addDiscountToTooltip() {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (addDiscountToTooltip() && getDiscountPercent() > 0) {
            tooltip.add(Component.translatable("ars_zero.tooltip.static_staff.discount", getDiscountPercent()));
        }
    }

    /**
     * Called when the staff is crafted and from {@link #ensurePresetSpells(ItemStack)} when
     * the stack has no spells in the static slot. Override in subclasses to write begin/tick/end
     * spells to physical slots 0, 1, 2. Default: no-op.
     */
    protected void applyPresetSpells(ItemStack stack) {
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
