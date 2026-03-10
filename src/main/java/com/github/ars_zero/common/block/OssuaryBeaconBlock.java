package com.github.ars_zero.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Ossuary beacon — anchor point for necromancers.
 * Tracks up to 2 active ritualists via its block entity.
 * Drops nothing when broken; emits a blight particle burst on destroy.
 */
public class OssuaryBeaconBlock extends BaseEntityBlock {

    public static final MapCodec<OssuaryBeaconBlock> CODEC = simpleCodec(p -> new OssuaryBeaconBlock());
    private static final Vector3f BLIGHT_COLOR = new Vector3f(0.29f, 0.48f, 0.19f);

    public OssuaryBeaconBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.5f, 6.0f)
                .pushReaction(PushReaction.BLOCK)
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OssuaryBeaconBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new DustParticleOptions(BLIGHT_COLOR, 1.5f),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    40, 0.5, 0.5, 0.5, 0.05);
        }
    }
}
