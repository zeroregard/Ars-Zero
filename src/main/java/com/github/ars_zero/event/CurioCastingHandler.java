package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
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
            MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.getCastContext(player, MultiPhaseCastContext.CastSource.CURIO);
            if (context != null && context.isCasting) {
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
        if (!(circletStack.getItem() instanceof AbstractMultiPhaseCastDevice device)) {
            ACTIVE_CASTERS.remove(serverPlayer.getUUID());
            finishCurioCast(serverPlayer);
            return;
        }
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.getCastContext(serverPlayer, MultiPhaseCastContext.CastSource.CURIO);
        if (context == null || !context.isCasting || context.source != MultiPhaseCastContext.CastSource.CURIO) {
            return;
        }
        device.tickPhase(serverPlayer, circletStack);
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
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.getCastContext(player, MultiPhaseCastContext.CastSource.CURIO);
        if (context == null) {
            return;
        }
        ItemStack castingStack = context.castingStack;
        if (castingStack.getItem() instanceof AbstractMultiPhaseCastDevice device) {
            device.endPhase(player, castingStack);
        }
    }
    
    private static Optional<ItemStack> findCirclet(Player player) {
        return CuriosApi.getCuriosHelper().findEquippedCurio(
            stack -> stack.getItem() instanceof SpellcastingCirclet,
            player
        ).map(result -> result.getRight());
    }
}
