package com.github.ars_zero.common.spell;

import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Records SpellResult(s) from effects that create entities or block groups,
 * so onEffectResolved can consume them and apply replace semantics.
 * Store in spellContext.tag; consume via take().
 */
public final class TemporalContextRecorder {

    private static final String KEY_ENTITY = "ars_zero:temporal_entity";
    private static final String KEY_ENTITY_COUNT = "ars_zero:temporal_entity_count";
    private static final String KEY_ENTITY_PREFIX = "ars_zero:temporal_entity_";
    private static final String KEY_BLOCK_GROUP = "ars_zero:temporal_block_group";
    private static final String KEY_BLOCK_POSITIONS = "ars_zero:temporal_block_positions";

    private TemporalContextRecorder() {}

    /**
     * Record a single entity for temporal context. Replace semantics will be applied.
     */
    public static void record(SpellContext context, Entity entity) {
        if (context == null || entity == null) {
            return;
        }
        ensureTag(context);
        context.tag.remove(KEY_ENTITY_COUNT);
        for (int i = 0; context.tag.contains(KEY_ENTITY_PREFIX + i); i++) {
            context.tag.remove(KEY_ENTITY_PREFIX + i);
        }
        context.tag.remove(KEY_BLOCK_GROUP);
        context.tag.remove(KEY_BLOCK_POSITIONS);
        context.tag.putUUID(KEY_ENTITY, entity.getUUID());
    }

    /**
     * Record multiple entities for temporal context. Replace semantics will be applied.
     */
    public static void record(SpellContext context, Entity... entities) {
        if (context == null || entities == null || entities.length == 0) {
            return;
        }
        ensureTag(context);
        context.tag.remove(KEY_ENTITY);
        context.tag.remove(KEY_BLOCK_GROUP);
        context.tag.remove(KEY_BLOCK_POSITIONS);
        context.tag.putInt(KEY_ENTITY_COUNT, entities.length);
        for (int i = 0; i < entities.length; i++) {
            if (entities[i] != null) {
                context.tag.putUUID(KEY_ENTITY_PREFIX + i, entities[i].getUUID());
            }
        }
    }

    /**
     * Record multiple entities for temporal context. Replace semantics will be applied.
     */
    public static void record(SpellContext context, List<? extends Entity> entities) {
        if (context == null || entities == null || entities.isEmpty()) {
            return;
        }
        ensureTag(context);
        context.tag.remove(KEY_ENTITY);
        context.tag.remove(KEY_BLOCK_GROUP);
        context.tag.remove(KEY_BLOCK_POSITIONS);
        context.tag.putInt(KEY_ENTITY_COUNT, entities.size());
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (e != null) {
                context.tag.putUUID(KEY_ENTITY_PREFIX + i, e.getUUID());
            }
        }
    }

    /**
     * Record a block group for temporal context. Replace semantics will be applied.
     */
    public static void record(SpellContext context, BlockGroupEntity blockGroup, List<BlockPos> blockPositions) {
        if (context == null || blockGroup == null) {
            return;
        }
        ensureTag(context);
        context.tag.remove(KEY_ENTITY);
        context.tag.remove(KEY_ENTITY_COUNT);
        for (int i = 0; context.tag.contains(KEY_ENTITY_PREFIX + i); i++) {
            context.tag.remove(KEY_ENTITY_PREFIX + i);
        }
        context.tag.putUUID(KEY_BLOCK_GROUP, blockGroup.getUUID());
        if (blockPositions != null && !blockPositions.isEmpty()) {
            long[] longs = new long[blockPositions.size()];
            for (int i = 0; i < blockPositions.size(); i++) {
                longs[i] = blockPositions.get(i).asLong();
            }
            context.tag.putLongArray(KEY_BLOCK_POSITIONS, longs);
        }
    }

    /**
     * Record block positions only (no block group entity). Propagates positions to later spell phases
     * without creating a BlockGroupEntity. Used e.g. with AugmentExtract on Select.
     */
    public static void recordBlockPositionsOnly(SpellContext context, List<BlockPos> blockPositions) {
        if (context == null || blockPositions == null || blockPositions.isEmpty()) {
            return;
        }
        ensureTag(context);
        context.tag.remove(KEY_ENTITY);
        context.tag.remove(KEY_ENTITY_COUNT);
        for (int i = 0; context.tag.contains(KEY_ENTITY_PREFIX + i); i++) {
            context.tag.remove(KEY_ENTITY_PREFIX + i);
        }
        context.tag.remove(KEY_BLOCK_GROUP);
        long[] longs = new long[blockPositions.size()];
        for (int i = 0; i < blockPositions.size(); i++) {
            longs[i] = blockPositions.get(i).asLong();
        }
        context.tag.putLongArray(KEY_BLOCK_POSITIONS, longs);
    }

    /**
     * Take any recorded results from the context. Returns null if nothing was recorded.
     * Clears the recorded data after consumption.
     */
    public static List<SpellResult> take(SpellContext context, Level level) {
        if (context == null || context.tag == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        List<SpellResult> results = new ArrayList<>();
        IWrappedCaster caster = context.getCaster();

        if (context.tag.hasUUID(KEY_ENTITY)) {
            UUID uuid = context.tag.getUUID(KEY_ENTITY);
            Entity entity = serverLevel.getEntity(uuid);
            if (entity != null && entity.isAlive()) {
                SpellResult result = SpellResult.fromHitResultWithCaster(
                        new EntityHitResult(entity),
                        SpellEffectType.RESOLVED,
                        caster);
                applyGeometryEntityFields(result, entity);
                results.add(result);
            }
            context.tag.remove(KEY_ENTITY);
            return results.isEmpty() ? null : results;
        }

        if (context.tag.contains(KEY_ENTITY_COUNT, Tag.TAG_INT)) {
            int count = context.tag.getInt(KEY_ENTITY_COUNT);
            for (int i = 0; i < count; i++) {
                if (context.tag.hasUUID(KEY_ENTITY_PREFIX + i)) {
                    UUID uuid = context.tag.getUUID(KEY_ENTITY_PREFIX + i);
                    Entity entity = serverLevel.getEntity(uuid);
                    if (entity != null && entity.isAlive()) {
                        SpellResult result = SpellResult.fromHitResultWithCaster(
                                new EntityHitResult(entity),
                                SpellEffectType.RESOLVED,
                                caster);
                        applyGeometryEntityFields(result, entity);
                        results.add(result);
                    }
                }
            }
            context.tag.remove(KEY_ENTITY_COUNT);
            for (int i = 0; i < count; i++) {
                context.tag.remove(KEY_ENTITY_PREFIX + i);
            }
            return results.isEmpty() ? null : results;
        }

        if (context.tag.hasUUID(KEY_BLOCK_GROUP)) {
            UUID uuid = context.tag.getUUID(KEY_BLOCK_GROUP);
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof BlockGroupEntity blockGroup) {
                List<BlockPos> positions = new ArrayList<>();
                if (context.tag.contains(KEY_BLOCK_POSITIONS, Tag.TAG_LONG_ARRAY)) {
                    long[] longs = context.tag.getLongArray(KEY_BLOCK_POSITIONS);
                    for (long l : longs) {
                        positions.add(BlockPos.of(l));
                    }
                }
                results.add(SpellResult.fromBlockGroup(blockGroup, positions, caster));
            }
            context.tag.remove(KEY_BLOCK_GROUP);
            context.tag.remove(KEY_BLOCK_POSITIONS);
            return results.isEmpty() ? null : results;
        }

        if (context.tag.contains(KEY_BLOCK_POSITIONS, Tag.TAG_LONG_ARRAY)) {
            long[] longs = context.tag.getLongArray(KEY_BLOCK_POSITIONS);
            List<BlockPos> positions = new ArrayList<>();
            for (long l : longs) {
                positions.add(BlockPos.of(l));
            }
            context.tag.remove(KEY_BLOCK_POSITIONS);
            if (!positions.isEmpty()) {
                results.add(SpellResult.fromBlockPositions(positions, caster));
            }
            return results.isEmpty() ? null : results;
        }

        return null;
    }

    private static void ensureTag(SpellContext context) {
        if (context.tag == null) {
            context.tag = new CompoundTag();
        }
    }

    private static void applyGeometryEntityFields(SpellResult result, Entity entity) {
        if (entity instanceof AbstractGeometryProcessEntity geometryEntity) {
            result.userOffset = geometryEntity.getUserOffset();
            result.depth = geometryEntity.getDepth();
        }
    }
}
