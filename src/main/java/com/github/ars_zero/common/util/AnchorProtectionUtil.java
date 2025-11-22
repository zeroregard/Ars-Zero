package com.github.ars_zero.common.util;

import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for Anchor anti-griefing protection.
 * 
 * Protection rules:
 * 1. Anchor does not work on players by default - they must opt-in via /anchoropt command
 * 2. Server operators can always anchor any entity, including players
 * 3. BlockGroupEntity movement is protected - all blocks in the group must pass claim protection checks
 * 4. Entity movement in protected chunks is blocked (uses BlockUtil.destroyRespectsClaim)
 * 
 * Note: The Break spell already has protection via BlockUtil.destroyRespectsClaim when blocks
 * are initially selected/created into BlockGroupEntity (see SelectEffect and ArsZeroResolverEvents).
 * This utility provides additional protection when moving entities and block groups.
 */
public class AnchorProtectionUtil {
    
    private static final Set<UUID> OPTED_IN_PLAYERS = ConcurrentHashMap.newKeySet();
    
    public static void setOptedIn(Player player, boolean optedIn) {
        if (optedIn) {
            OPTED_IN_PLAYERS.add(player.getUUID());
        } else {
            OPTED_IN_PLAYERS.remove(player.getUUID());
        }
    }
    
    public static boolean isOptedIn(Player player) {
        return OPTED_IN_PLAYERS.contains(player.getUUID());
    }
    
    public static boolean canAnchorEntity(Player caster, Entity target, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        if (!(target instanceof Player targetPlayer)) {
            return true;
        }
        
        if (isServerOp(caster, serverLevel)) {
            return true;
        }
        
        return isOptedIn(targetPlayer);
    }
    
    public static boolean canMoveBlockGroup(Player caster, BlockGroupEntity blockGroup, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        if (isServerOp(caster, serverLevel)) {
            return true;
        }
        
        for (BlockGroupEntity.BlockData blockData : blockGroup.getBlocks()) {
            BlockPos originalPos = blockData.originalPosition;
            if (!BlockUtil.destroyRespectsClaim(caster, level, originalPos)) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean canMoveEntityInChunk(Player caster, Entity entity, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        if (isServerOp(caster, serverLevel)) {
            return true;
        }
        
        BlockPos entityPos = BlockPos.containing(entity.position());
        return BlockUtil.destroyRespectsClaim(caster, level, entityPos);
    }
    
    private static boolean isServerOp(Player player, ServerLevel level) {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.getServer() != null && 
                   serverPlayer.getServer().getPlayerList().isOp(serverPlayer.getGameProfile());
        }
        return false;
    }
}

