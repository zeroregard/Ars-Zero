package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.spell.StaffCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CurioCastingHandler {
    private static final Set<UUID> ACTIVE_CASTERS = ConcurrentHashMap.newKeySet();
    
    public static void handleInput(ServerPlayer player, boolean pressed) {
        if (pressed) {
            if (ACTIVE_CASTERS.contains(player.getUUID())) {
                return;
            }
            StaffCastContext context = AbstractSpellStaff.getStaffContext(player);
            if (context != null && context.isHoldingStaff) {
                return;
            }
            Optional<ItemStack> stack = findCirclet(player);
            if (stack.isEmpty()) {
                return;
            }
            ItemStack circletStack = stack.get();
            if (circletStack.getItem() instanceof SpellcastingCirclet circlet) {
                circlet.beginCurioCast(player, circletStack);
                ACTIVE_CASTERS.add(player.getUUID());
            }
        } else {
            if (ACTIVE_CASTERS.remove(player.getUUID())) {
                finishCurioCast(player);
            }
        }
    }
    
    @SubscribeEvent
    public static void handlePlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ACTIVE_CASTERS.contains(serverPlayer.getUUID())) {
            return;
        }
        Optional<ItemStack> stack = findCirclet(serverPlayer);
        if (stack.isEmpty()) {
            ACTIVE_CASTERS.remove(serverPlayer.getUUID());
            finishCurioCast(serverPlayer);
            return;
        }
        ItemStack circletStack = stack.get();
        if (!(circletStack.getItem() instanceof AbstractSpellStaff staff)) {
            ACTIVE_CASTERS.remove(serverPlayer.getUUID());
            finishCurioCast(serverPlayer);
            return;
        }
        StaffCastContext context = AbstractSpellStaff.getStaffContext(serverPlayer);
        if (context == null || !context.isHoldingStaff || context.source != StaffCastContext.CastSource.CURIO) {
            if (staff instanceof SpellcastingCirclet circlet) {
                circlet.beginCurioCast(serverPlayer, circletStack);
            }
            return;
        }
        staff.tickPhase(serverPlayer, circletStack);
    }
    
    @SubscribeEvent
    public static void handleLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (ACTIVE_CASTERS.remove(serverPlayer.getUUID())) {
                finishCurioCast(serverPlayer);
            }
        }
    }
    
    @SubscribeEvent
    public static void handleRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (ACTIVE_CASTERS.remove(serverPlayer.getUUID())) {
                finishCurioCast(serverPlayer);
            }
        }
    }
    
    private static void finishCurioCast(ServerPlayer player) {
        StaffCastContext context = AbstractSpellStaff.getStaffContext(player);
        if (context == null || context.source != StaffCastContext.CastSource.CURIO) {
            return;
        }
        ItemStack castingStack = context.castingStack;
        if (castingStack.getItem() instanceof AbstractSpellStaff staff) {
            staff.endPhase(player, castingStack);
        } else {
            ArsZero.LOGGER.debug("Missing casting stack for curio spellcasting on player {}", player.getScoreboardName());
        }
    }
    
    private static Optional<ItemStack> findCirclet(Player player) {
        return CuriosApi.getCuriosHelper().findEquippedCurio(
            stack -> stack.getItem() instanceof SpellcastingCirclet,
            player
        ).map(result -> result.getRight());
    }
}
