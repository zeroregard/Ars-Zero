package com.github.ars_zero.common.glyph.convergence;

import alexthw.ars_elemental.common.glyphs.EffectConjureTerrain;
import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.common.spell.SpellAugmentExtractor;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class ConjureTerrainConvergenceHelper {
    private static final int DEFAULT_LIFESPAN = 20;

    private ConjureTerrainConvergenceHelper() {
    }

    public static void handleConjureTerrain(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
            SpellContext spellContext, EffectConvergence convergence) {
        BlockPos centerBlock = BlockPos.containing(pos);
        Vec3 center = Vec3.atCenterOf(centerBlock);

        ConjureTerrainConvergenceEntity entity = new ConjureTerrainConvergenceEntity(
                ModEntities.CONJURE_TERRAIN_CONVERGENCE_CONTROLLER.get(), serverLevel);
        entity.setPos(center.x, center.y, center.z);
        if (shooter != null) {
            entity.setCaster(shooter);
            if (shooter instanceof Player player) {
                entity.setMarkerPos(player.blockPosition());
            }
        }
        entity.setLifespan(DEFAULT_LIFESPAN);

        TerrainResult result = determineTerrainBlockState(spellContext, serverLevel);
        entity.setTerrainBlockState(result.blockState);
        int augmentCount = countConjureTerrainAugments(spellContext);
        entity.setAugmentCount(augmentCount);

        GeometryDescription geometryDescription = ConvergenceCompatibilityHelper.resolveGeometryDescription(
                spellContext,
                shooter);
        entity.setGeometryDescription(geometryDescription);

        SpellContext iterator = spellContext.clone();
        while (iterator.hasNextPart()) {
            AbstractSpellPart next = iterator.nextPart();
            if (next instanceof EffectConjureTerrain conjureTerrainEffect) {
                serverLevel.addFreshEntity(entity);
                convergence.updateTemporalContext(shooter, entity, spellContext);
                convergence.consumeEffect(spellContext, conjureTerrainEffect);
                if (result.modifierEffect != null) {
                    convergence.consumeEffect(spellContext, result.modifierEffect);
                }
                spellContext.setCanceled(true);
                convergence.triggerResolveEffects(spellContext, serverLevel, center);
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

        boolean isRandomized = hasRandomizeAugment(spellContext, EffectConjureTerrain.INSTANCE);

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

    private static boolean hasRandomizeAugment(SpellContext context, AbstractEffect targetEffect) {
        ResourceLocation targetId = targetEffect.getRegistryName();
        SpellContext iterator = context.clone();
        boolean foundTarget = false;

        while (iterator.hasNextPart()) {
            AbstractSpellPart part = iterator.nextPart();

            if (!foundTarget) {
                if (part instanceof AbstractEffect effect && effectsMatch(effect, targetEffect, targetId)) {
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

    private static int countConjureTerrainAugments(SpellContext context) {
        ResourceLocation targetId = EffectConjureTerrain.INSTANCE.getRegistryName();
        SpellContext iterator = context.clone();
        boolean foundTarget = false;
        int count = 0;

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

            if (part instanceof AbstractAugment) {
                count++;
            }
        }

        return count;
    }
}
