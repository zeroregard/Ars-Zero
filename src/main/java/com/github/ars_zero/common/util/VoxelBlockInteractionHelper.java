package com.github.ars_zero.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class VoxelBlockInteractionHelper {
    
    public static boolean handlePhysicalCollision(Level level, Entity entity, BlockHitResult blockHit) {
        if (level.isClientSide) {
            return false;
        }
        
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        if (tryInteractWithBlock(level, entity, pos, state, block)) {
            return true;
        }
        
        Direction hitDirection = blockHit.getDirection();
        BlockPos adjacentPos = pos.relative(hitDirection);
        BlockState adjacentState = level.getBlockState(adjacentPos);
        Block adjacentBlock = adjacentState.getBlock();
        
        return tryInteractWithBlock(level, entity, adjacentPos, adjacentState, adjacentBlock);
    }
    
    private static boolean tryInteractWithBlock(Level level, Entity entity, BlockPos pos, BlockState state, Block block) {
        if (block instanceof LeverBlock) {
            BooleanProperty powered = BlockStateProperties.POWERED;
            if (state.hasProperty(powered)) {
                boolean currentPowered = state.getValue(powered);
                level.setBlock(pos, state.setValue(powered, !currentPowered), Block.UPDATE_ALL);
                level.playSound(null, pos, state.getSoundType().getHitSound(), 
                    entity.getSoundSource(), 0.5f, 1.0f);
                return true;
            }
        } else if (block instanceof DoorBlock && block != Blocks.IRON_DOOR) {
            BooleanProperty open = BlockStateProperties.OPEN;
            if (state.hasProperty(open) && !state.getValue(open)) {
                level.setBlock(pos, state.setValue(open, true), Block.UPDATE_ALL);
                level.playSound(null, pos, state.getSoundType().getPlaceSound(), 
                    entity.getSoundSource(), 0.5f, 1.0f);
                return true;
            }
        } else if (block instanceof TrapDoorBlock && block != Blocks.IRON_TRAPDOOR) {
            BooleanProperty open = BlockStateProperties.OPEN;
            if (state.hasProperty(open) && !state.getValue(open)) {
                level.setBlock(pos, state.setValue(open, true), Block.UPDATE_ALL);
                level.playSound(null, pos, state.getSoundType().getPlaceSound(), 
                    entity.getSoundSource(), 0.5f, 1.0f);
                return true;
            }
        } else if (block instanceof ButtonBlock) {
            BooleanProperty powered = BlockStateProperties.POWERED;
            if (state.hasProperty(powered) && !state.getValue(powered)) {
                level.setBlock(pos, state.setValue(powered, true), Block.UPDATE_ALL);
                level.scheduleTick(pos, block, 20);
                level.playSound(null, pos, state.getSoundType().getHitSound(), 
                    entity.getSoundSource(), 0.5f, 1.0f);
                return true;
            }
        } else if (block instanceof PressurePlateBlock) {
            Vec3 oldPos = entity.position();
            Vec3 plateTop = Vec3.atBottomCenterOf(pos).add(0, 0.0625, 0);
            
            entity.setPos(plateTop.x, plateTop.y, plateTop.z);
            entity.setBoundingBox(entity.getBoundingBox().move(plateTop.subtract(oldPos)));
            
            state.entityInside(level, pos, entity);
            
            entity.setPos(oldPos.x, oldPos.y, oldPos.z);
            entity.setBoundingBox(entity.getBoundingBox().move(oldPos.subtract(plateTop)));
            
            level.playSound(null, pos, state.getSoundType().getHitSound(), 
                entity.getSoundSource(), 0.5f, 1.0f);
            return true;
        }
        
        return false;
    }
}

