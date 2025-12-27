package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModBlocks;
import com.github.ars_zero.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class IceVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0xFFFFFF;
    private static final double DAMAGE_SPEED_THRESHOLD = 0.35;
    private static final float BASE_DAMAGE = 1.5f;
    private static final float DAMAGE_SCALE = 2.0f;
    private static final float MAX_DAMAGE = 6.0f;
    
    private static final Set<Block> BREAKABLE_BLOCKS = Set.of(
        Blocks.GLASS,
        Blocks.GLASS_PANE,
        Blocks.WHITE_STAINED_GLASS,
        Blocks.ORANGE_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.LIME_STAINED_GLASS,
        Blocks.PINK_STAINED_GLASS,
        Blocks.GRAY_STAINED_GLASS,
        Blocks.LIGHT_GRAY_STAINED_GLASS,
        Blocks.CYAN_STAINED_GLASS,
        Blocks.PURPLE_STAINED_GLASS,
        Blocks.BLUE_STAINED_GLASS,
        Blocks.BROWN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS,
        Blocks.BLACK_STAINED_GLASS,
        Blocks.WHITE_STAINED_GLASS_PANE,
        Blocks.ORANGE_STAINED_GLASS_PANE,
        Blocks.MAGENTA_STAINED_GLASS_PANE,
        Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
        Blocks.YELLOW_STAINED_GLASS_PANE,
        Blocks.LIME_STAINED_GLASS_PANE,
        Blocks.PINK_STAINED_GLASS_PANE,
        Blocks.GRAY_STAINED_GLASS_PANE,
        Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
        Blocks.CYAN_STAINED_GLASS_PANE,
        Blocks.PURPLE_STAINED_GLASS_PANE,
        Blocks.BLUE_STAINED_GLASS_PANE,
        Blocks.BROWN_STAINED_GLASS_PANE,
        Blocks.GREEN_STAINED_GLASS_PANE,
        Blocks.RED_STAINED_GLASS_PANE,
        Blocks.BLACK_STAINED_GLASS_PANE,
        Blocks.DANDELION,
        Blocks.POPPY,
        Blocks.BLUE_ORCHID,
        Blocks.ALLIUM,
        Blocks.AZURE_BLUET,
        Blocks.RED_TULIP,
        Blocks.ORANGE_TULIP,
        Blocks.WHITE_TULIP,
        Blocks.PINK_TULIP,
        Blocks.OXEYE_DAISY,
        Blocks.CORNFLOWER,
        Blocks.LILY_OF_THE_VALLEY,
        Blocks.WITHER_ROSE,
        Blocks.SUNFLOWER,
        Blocks.LILAC,
        Blocks.ROSE_BUSH,
        Blocks.PEONY,
        Blocks.TALL_GRASS,
        Blocks.DEAD_BUSH,
        Blocks.FERN,
        Blocks.LARGE_FERN,
        Blocks.SUGAR_CANE,
        Blocks.CACTUS,
        Blocks.BAMBOO
    );
    
    public IceVoxelEntity(EntityType<? extends IceVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public IceVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.ICE_VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
        this.setNoGravityCustom(false);
    }
    
    @Override
    public int getColor() {
        return COLOR;
    }
    
    @Override
    public boolean isEmissive() {
        return false;
    }
    
    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        if (handlePhysicalCollision(blockHit)) {
            return;
        }
        
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = this.level().getBlockState(pos);
        Block block = state.getBlock();
        FluidState fluidState = state.getFluidState();
        
        if (fluidState.isSource() && fluidState.getType() == ModFluids.BLIGHT_FLUID.get()) {
            if (!this.level().isClientSide) {
                this.level().setBlock(pos, ModBlocks.FROZEN_BLIGHT.get().defaultBlockState(), 3);
                spawnHitParticles(blockHit.getLocation());
            }
            this.discard();
            return;
        }
        
        if (block == Blocks.WATER) {
            if (!this.level().isClientSide) {
                this.level().setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                spawnHitParticles(blockHit.getLocation());
            }
            this.discard();
            return;
        }
        
        BlockPos adjacentPos = pos.relative(blockHit.getDirection());
        BlockState adjacentState = this.level().getBlockState(adjacentPos);
        FluidState adjacentFluidState = adjacentState.getFluidState();
        if (adjacentFluidState.isSource() && adjacentFluidState.getType() == ModFluids.BLIGHT_FLUID.get()) {
            if (!this.level().isClientSide) {
                this.level().setBlock(adjacentPos, ModBlocks.FROZEN_BLIGHT.get().defaultBlockState(), 3);
                spawnHitParticles(blockHit.getLocation());
            }
            this.discard();
            return;
        }
        if (adjacentState.getBlock() == Blocks.WATER) {
            if (!this.level().isClientSide) {
                this.level().setBlock(adjacentPos, Blocks.ICE.defaultBlockState(), 3);
                spawnHitParticles(blockHit.getLocation());
            }
            this.discard();
            return;
        }
        
        if (BREAKABLE_BLOCKS.contains(block)) {
            breakBlock(pos, state);
        } else {
            if (!this.level().isClientSide) {
                Vec3 location = blockHit.getLocation();
                this.level().playSound(null, location.x, location.y, location.z, 
                    SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6f, 1.0f + this.random.nextFloat() * 0.3f);
            }
            spawnHitParticles(blockHit.getLocation());
        }
        
        this.discard();
    }
    
    private void breakBlock(BlockPos pos, BlockState state) {
        if (this.level().isClientSide) {
            return;
        }
        
        ServerLevel serverLevel = (ServerLevel) this.level();
        
        LootParams.Builder lootParams = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .withParameter(LootContextParams.THIS_ENTITY, this);
        
        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        
        for (ItemStack drop : state.getDrops(lootParams)) {
            Block.popResource(serverLevel, pos, drop);
        }
        
        serverLevel.levelEvent(2001, pos, Block.getId(state));
        spawnHitParticles(Vec3.atCenterOf(pos));
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (hit instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        if (!this.level().isClientSide) {
            Vec3 location = result.getLocation();
            this.level().playSound(null, location.x, location.y, location.z, 
                SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6f, 1.0f + this.random.nextFloat() * 0.3f);
            
            if (hit instanceof LivingEntity living) {
                applyImpactDamage(living);
            }
            spawnHitParticles(result.getLocation());
        }
        this.discard();
    }
    
    private void applyImpactDamage(LivingEntity target) {
        double speed = this.getDeltaMovement().length();
        if (speed < DAMAGE_SPEED_THRESHOLD) {
            return;
        }
        float sizeScale = Math.max(1.0f, this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE);
        float damage = BASE_DAMAGE + (float) ((speed - DAMAGE_SPEED_THRESHOLD) * DAMAGE_SCALE);
        damage *= sizeScale;
        damage = Math.min(damage, MAX_DAMAGE);
        LivingEntity sender = this.getStoredCaster();
        net.minecraft.world.damagesource.DamageSource damageSource;
        if (sender != null) {
            damageSource = this.level().damageSources().indirectMagic(this, sender);
            target.setLastHurtByMob(sender);
            if (sender instanceof net.minecraft.world.entity.player.Player) {
                target.setLastHurtByPlayer((net.minecraft.world.entity.player.Player) sender);
            }
        } else {
            damageSource = this.level().damageSources().magic();
        }
        target.hurt(damageSource, damage);
        Vec3 impulse = this.getDeltaMovement().scale(0.35);
        target.push(impulse.x, Math.max(0.1, impulse.y + 0.15), impulse.z);
        target.hurtMarked = true;
    }
    
    @Override
    protected ParticleOptions getAmbientParticle() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());
    }
    
    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ParticleOptions option = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());
        int particleCount = Math.max(8, (int) (this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE) * 6);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.4;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.4;
            serverLevel.sendParticles(option, location.x + offsetX, location.y + offsetY, location.z + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
