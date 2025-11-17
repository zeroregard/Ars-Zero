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
                ArsZero.LOGGER.info("Curio cast input: already active for {}", player.getScoreboardName());
                return;
            }
            MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.getCastContext(player, MultiPhaseCastContext.CastSource.CURIO);
            if (context != null && context.isCasting) {
                ArsZero.LOGGER.info("Curio cast input: player already casting with circlet for {}", player.getScoreboardName());
                return;
            }
            Optional<ItemStack> stack = findCirclet(player);
            if (stack.isEmpty()) {
                ArsZero.LOGGER.info("Curio cast input: no circlet found for {}", player.getScoreboardName());
                return;
            }
            ItemStack circletStack = stack.get();
            if (circletStack.getItem() instanceof SpellcastingCirclet circlet) {
                ArsZero.LOGGER.info("Begin curio casting for {}", player.getScoreboardName());
                circlet.beginCurioCast(player, circletStack);
                ACTIVE_CASTERS.add(player.getUUID());
            }
        } else {
            if (ACTIVE_CASTERS.remove(player.getUUID())) {
                ArsZero.LOGGER.info("Curio cast input: releasing for {}", player.getScoreboardName());
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
            ArsZero.LOGGER.info("Curio tick: circlet removed, finishing cast for {}", serverPlayer.getScoreboardName());
            ACTIVE_CASTERS.remove(serverPlayer.getUUID());
            finishCurioCast(serverPlayer);
            return;
        }
        ItemStack circletStack = stack.get();
        if (!(circletStack.getItem() instanceof AbstractMultiPhaseCastDevice device)) {
            ArsZero.LOGGER.info("Curio tick: circlet is not a cast device, finishing cast for {}", serverPlayer.getScoreboardName());
            ACTIVE_CASTERS.remove(serverPlayer.getUUID());
            finishCurioCast(serverPlayer);
            return;
        }
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.getCastContext(serverPlayer, MultiPhaseCastContext.CastSource.CURIO);
        if (context == null) {
            ArsZero.LOGGER.info("Curio tick: no context for {} (begin may not have completed yet)", serverPlayer.getScoreboardName());
            return;
        }
        if (!context.isCasting) {
            ArsZero.LOGGER.info("Curio tick: context.isCasting=false for {}", serverPlayer.getScoreboardName());
            return;
        }
        if (context.source != MultiPhaseCastContext.CastSource.CURIO) {
            ArsZero.LOGGER.info("Curio tick: context.source={} (expected CURIO) for {}", context.source, serverPlayer.getScoreboardName());
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
            ArsZero.LOGGER.info("End curio casting: context={}, source={} for {}", 
                context != null ? "exists" : "null", 
                context != null ? context.source : "N/A",
                player.getScoreboardName());
            return;
        }
        ArsZero.LOGGER.info("End curio casting for {}", player.getScoreboardName());
        ItemStack castingStack = context.castingStack;
        if (castingStack.getItem() instanceof AbstractMultiPhaseCastDevice device) {
            device.endPhase(player, castingStack);
        } else {
            ArsZero.LOGGER.info("Missing casting stack for curio spellcasting on player {}", player.getScoreboardName());
        }
    }
    
    private static Optional<ItemStack> findCirclet(Player player) {
        return CuriosApi.getCuriosHelper().findEquippedCurio(
            stack -> stack.getItem() instanceof SpellcastingCirclet,
            player
        ).map(result -> result.getRight());
    }
}
