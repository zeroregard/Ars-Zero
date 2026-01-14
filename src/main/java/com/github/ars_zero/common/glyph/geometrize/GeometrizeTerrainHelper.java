package com.github.ars_zero.common.glyph.geometrize;

import alexthw.ars_elemental.common.glyphs.EffectConjureTerrain;
import com.github.ars_zero.common.entity.terrain.GeometryTerrainEntity;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.common.spell.SpellAugmentExtractor;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentRandomize;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectCrush;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectSmelt;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class GeometrizeTerrainHelper {

    private GeometrizeTerrainHelper() {
    }

    public static void handleConjureTerrain(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
                                            SpellContext spellContext, EffectGeometrize geometrize, HitResult rayTraceResult,
                                            SpellResolver resolver) {
        BlockPos centerBlock = BlockPos.containing(pos);
        Vec3 center = Vec3.atCenterOf(centerBlock);

        GeometryDescription geometryDescription = GeometrizeCompatibilityHelper.resolveGeometryDescription(
                spellContext, shooter);
        int augmentCount = GeometrizeUtils.countAugmentsAfterEffect(spellContext, EffectConjureTerrain.class);
        int size = GeometrizeUtils.getPreferredSize(shooter, augmentCount);
        int depth = GeometrizeUtils.getPreferredDepth(shooter);

        Vec3 offsetCenter = GeometrizeUtils.calculateOffsetPosition(center, rayTraceResult, size,
                geometryDescription);

        GeometryTerrainEntity entity = new GeometryTerrainEntity(
                ModEntities.GEOMETRY_TERRAIN_CONTROLLER.get(), serverLevel);
        entity.setPos(offsetCenter.x, offsetCenter.y, offsetCenter.z);
        if (shooter != null) {
            entity.setCaster(shooter);
            if (shooter instanceof Player player) {
                entity.setMarkerPos(player.blockPosition());
            }
        }
        entity.setLifespan(GeometrizeUtils.DEFAULT_LIFESPAN);

        TerrainResult result = determineTerrainBlockState(spellContext, serverLevel);
        entity.setTerrainBlockState(result.blockState);
        entity.setAugmentCount(augmentCount);
        entity.setGeometryDescription(geometryDescription);
        entity.setSize(size);
        entity.setDepth(depth);
        entity.setBasePosition(offsetCenter);
        entity.setSpellContext(spellContext, resolver);

        SpellContext iterator = spellContext.clone();
        while (iterator.hasNextPart()) {
            AbstractSpellPart next = iterator.nextPart();
            if (next instanceof EffectConjureTerrain conjureTerrainEffect) {
                serverLevel.addFreshEntity(entity);
                geometrize.updateTemporalContext(shooter, entity, spellContext);
                geometrize.consumeEffect(spellContext, conjureTerrainEffect);
                if (result.modifierEffect != null) {
                    geometrize.consumeEffect(spellContext, result.modifierEffect);
                }
                spellContext.setCanceled(true);
                geometrize.triggerResolveEffects(spellContext, serverLevel, center);
                break;
            }
        }
    }

    private static class TerrainResult {
        final BlockState blockState;
        @Nullable
        final AbstractEffect modifierEffect;

        TerrainResult(BlockState blockState, @Nullable AbstractEffect modifierEffect) {
            this.blockState = blockState;
            this.modifierEffect = modifierEffect;
        }
    }

    private static TerrainResult determineTerrainBlockState(SpellContext spellContext, ServerLevel level) {
        SpellAugmentExtractor.AugmentData augmentData = SpellAugmentExtractor
                .extractApplicableAugments(spellContext, EffectConjureTerrain.INSTANCE);
        int amps = augmentData.amplifyLevel;

        boolean isRandomized = hasRandomizeAugment(spellContext);

        BlockState toPlace = switch (amps) {
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            case 2 -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            default -> Blocks.DIRT.defaultBlockState();
        };

        if (isRandomized && toPlace.getBlock() == Blocks.DIRT) {
            toPlace = switch (level.random.nextInt(5)) {
                case 0 -> Blocks.COARSE_DIRT.defaultBlockState();
                case 1 -> Blocks.PODZOL.defaultBlockState();
                case 2 -> Blocks.GRASS_BLOCK.defaultBlockState();
                case 4 -> Blocks.GRAVEL.defaultBlockState();
                default -> Blocks.DIRT.defaultBlockState();
            };
        }

        AbstractEffect modifierEffect = null;

        SpellContext iterator = spellContext.clone();
        ResourceLocation conjureTerrainId = EffectConjureTerrain.INSTANCE.getRegistryName();
        boolean foundConjureTerrain = false;

        while (iterator.hasNextPart()) {
            AbstractSpellPart next = iterator.nextPart();

            if (!foundConjureTerrain) {
                if (next instanceof AbstractEffect effect
                        && effectsMatch(effect, EffectConjureTerrain.INSTANCE, conjureTerrainId)) {
                    foundConjureTerrain = true;
                }
                continue;
            }

            if (next instanceof AbstractEffect effect) {
                if (effect == EffectConjureWater.INSTANCE) {
                    toPlace = Blocks.MUD.defaultBlockState();
                    modifierEffect = effect;
                    break;
                } else if (effect == EffectCrush.INSTANCE) {
                    toPlace = amps > 0 ? Blocks.SANDSTONE.defaultBlockState() : Blocks.SAND.defaultBlockState();
                    if (isRandomized && level.random.nextBoolean()) {
                        toPlace = amps > 0 ? Blocks.RED_SANDSTONE.defaultBlockState()
                                : Blocks.RED_SAND.defaultBlockState();
                    }
                    modifierEffect = effect;
                    break;
                } else if (effect == EffectSmelt.INSTANCE && amps > 0) {
                    toPlace = amps > 1 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    if (isRandomized && toPlace.getBlock() == Blocks.STONE) {
                        toPlace = switch (level.random.nextInt(6)) {
                            case 0 -> Blocks.DIORITE.defaultBlockState();
                            case 1 -> Blocks.ANDESITE.defaultBlockState();
                            case 2 -> Blocks.GRANITE.defaultBlockState();
                            case 3 -> Blocks.TUFF.defaultBlockState();
                            case 4 -> Blocks.CALCITE.defaultBlockState();
                            default -> Blocks.BLACKSTONE.defaultBlockState();
                        };
                    }
                    modifierEffect = effect;
                    break;
                }
                break;
            }
        }

        return new TerrainResult(toPlace, modifierEffect);
    }

    private static boolean hasRandomizeAugment(SpellContext context) {
        ResourceLocation targetId = EffectConjureTerrain.INSTANCE.getRegistryName();
        SpellContext iterator = context.clone();
        boolean foundTarget = false;

        while (iterator.hasNextPart()) {
            AbstractSpellPart part = iterator.nextPart();

            if (!foundTarget) {
                if (part instanceof AbstractEffect effect
                        && effectsMatch(effect, EffectConjureTerrain.INSTANCE, targetId)) {
                    foundTarget = true;
                }
                continue;
            }

            if (part instanceof AbstractEffect) {
                break;
            }

            if (part == AugmentRandomize.INSTANCE) {
                return true;
            }
        }

        return false;
    }

    private static boolean effectsMatch(AbstractEffect candidate, AbstractEffect reference, ResourceLocation id) {
        if (candidate == reference) {
            return true;
        }
        ResourceLocation candidateId = candidate.getRegistryName();
        return id != null && id.equals(candidateId);
    }
}

