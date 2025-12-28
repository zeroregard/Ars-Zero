package com.github.ars_zero.common.block;

import com.github.ars_zero.registry.ModFluids;
import com.github.ars_zero.registry.ModParticles;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BlightCauldronBlock extends AbstractCauldronBlock implements EntityBlock {
    
    public static final int MAX_FILL_LEVEL = 3;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;
    private static final TagKey<Block> REPLACEABLE_PLANTS = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("minecraft", "replaceable_plants")
    );
    private static final TagKey<Item> VAPORIZES_ITEMS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("ars_zero", "blight_cauldron_vaporizes")
    );
    public static final MapCodec<BlightCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(propertiesCodec()).apply(instance, BlightCauldronBlock::new)
    );
    
    private static final CauldronInteraction.InteractionMap INTERACTIONS = CauldronInteraction.newInteractionMap("ars_zero_blight");
    
    static {
        Map<Item, CauldronInteraction> interactions = INTERACTIONS.map();
        interactions.put(
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                new ItemStack(ModFluids.BLIGHT_FLUID_BUCKET.get()),
                s -> s.getValue(LEVEL) == MAX_FILL_LEVEL,
                SoundEvents.BUCKET_FILL
            )
        );
        interactions.put(
            Items.WATER_BUCKET,
            (state, level, pos, player, hand, stack) -> {
                if (!level.isClientSide) {
                    Item item = stack.getItem();
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                    player.awardStat(Stats.FILL_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(item));
                    level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 12, 0.25, 0.15, 0.25, 0.02);
                        serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 8, 0.25, 0.15, 0.25, 0.0);
                    }
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        );
        interactions.put(
            Items.LAVA_BUCKET,
            (state, level, pos, player, hand, stack) -> {
                if (!level.isClientSide) {
                    Item item = stack.getItem();
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                    player.awardStat(Stats.FILL_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(item));
                    level.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 12, 0.25, 0.15, 0.25, 0.02);
                        serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 8, 0.25, 0.15, 0.25, 0.0);
                    }
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        );
    }
    
    public BlightCauldronBlock(Properties properties) {
        super(properties, INTERACTIONS);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 1));
    }
    
    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }
    
    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == MAX_FILL_LEVEL;
    }
    
    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0 + (double)state.getValue(LEVEL).intValue() * 3.0) / 16.0;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }
    
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }
    
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        if (!this.isEntityInsideContent(state, pos, entity)) {
            return;
        }
        
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        } else if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block converted = BlightItemConversions.convert(blockItem.getBlock());
                if (converted != null) {
                    itemEntity.setItem(new ItemStack(converted, stack.getCount()));
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.0f);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 8, 0.2, 0.15, 0.2, 0.02);
                        serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 6, 0.25, 0.15, 0.25, 0.0);
                    }
                    return;
                }
            }
            boolean shouldVaporize = stack.is(VAPORIZES_ITEMS);
            if (!shouldVaporize && stack.getItem() instanceof BlockItem blockItem) {
                BlockState blockState = blockItem.getBlock().defaultBlockState();
                shouldVaporize = blockState.is(BlockTags.FLOWERS)
                    || blockState.is(BlockTags.SAPLINGS)
                    || blockState.is(BlockTags.LEAVES)
                    || blockState.is(REPLACEABLE_PLANTS);
            }
            if (shouldVaporize) {
                itemEntity.discard();
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.0f);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 10, 0.2, 0.15, 0.2, 0.02);
                    serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 10, 0.25, 0.15, 0.25, 0.0);
                }
            }
        }
    }
    
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlightCauldronBlockEntity(pos, state);
    }
}
