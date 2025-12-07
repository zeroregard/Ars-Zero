package com.github.ars_zero.common.block;

import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MultiphaseSpellTurret extends BasicSpellTurret {

    public MultiphaseSpellTurret(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public MultiphaseSpellTurret() {
        super(defaultProperties().noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MultiphaseSpellTurretTile(pos, state);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hitResult) {
        if (handIn != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        Item item = stack.getItem();
        if (!(item instanceof AbstractMultiPhaseCastDevice)) {
            if (item instanceof SpellBook) {
                PortUtil.sendMessage(player, Component.translatable("ars_zero.alert.multiphase_turret.multi_phase_required"));
            }
            return super.useItemOn(stack, state, level, pos, player, handIn, hitResult);
        }
        if (level.getBlockEntity(pos) instanceof MultiphaseSpellTurretTile tile) {
            configureFromDevice(stack, player, level, pos, tile);
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, handIn, hitResult);
    }

    private void configureFromDevice(ItemStack stack, Player player, Level level, BlockPos pos, MultiphaseSpellTurretTile tile) {
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            PortUtil.sendMessage(player, Component.translatable("ars_zero.alert.multiphase_turret.no_spell_data"));
            return;
        }
        int logicalSlot = caster.getCurrentSlot();
        if (logicalSlot < 0 || logicalSlot >= 10) {
            logicalSlot = 0;
        }
        Spell begin = copySpell(caster.getSpell(logicalSlot * 3 + 0));
        Spell tick = copySpell(caster.getSpell(logicalSlot * 3 + 1));
        Spell end = copySpell(caster.getSpell(logicalSlot * 3 + 2));
        if (begin.isEmpty() && tick.isEmpty() && end.isEmpty()) {
            PortUtil.sendMessage(player, Component.translatable("ars_zero.alert.multiphase_turret.empty_slot"));
            return;
        }
        int tickDelayOffset = AbstractMultiPhaseCastDevice.getSlotTickDelayOffset(stack, logicalSlot);
        UUID owner = player.getUUID();
        tile.configureSpells(begin, tick, end, owner, tickDelayOffset);
        tile.updateBlock();
        level.sendBlockUpdated(pos, tile.getBlockState(), tile.getBlockState(), 2);
        PortUtil.sendMessage(player, Component.translatable("ars_zero.alert.multiphase_turret.spell_set"));
    }

    private static Spell copySpell(Spell spell) {
        if (spell == null || spell.isEmpty()) {
            return new Spell();
        }
        String json = spell.toJson();
        return json.isEmpty() ? new Spell() : Spell.fromJson(json);
    }
}
