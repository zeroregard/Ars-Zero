package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.glyph.geometrize.EffectGeometrize;
import com.github.ars_zero.common.particle.timeline.GeometrizeTimeline;
import com.github.ars_zero.registry.ModParticleTimelines;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.SoundProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.registry.ParticleTimelineRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class GeometryEntity extends AbstractGeometryProcessEntity {

    private static final EntityDataAccessor<Float> DATA_COLOR_R = SynchedEntityData
            .defineId(GeometryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_COLOR_G = SynchedEntityData
            .defineId(GeometryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_COLOR_B = SynchedEntityData
            .defineId(GeometryEntity.class, EntityDataSerializers.FLOAT);

    private double forwardedSpellManaCost = 0.0;

    public GeometryEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setSpellContext(@Nullable SpellContext context, @Nullable SpellResolver resolver) {
        super.setSpellContext(context, resolver);

        float r = 1.0f, g = 1.0f, b = 1.0f;
        if (context != null) {
            var timeline = context.getParticleTimeline(
                    ParticleTimelineRegistry.MAGEBLOCK_TIMELINE.get());
            ParticleColor color = timeline.getColor();
            if (color != null) {
                r = color.getRed();
                g = color.getGreen();
                b = color.getBlue();
            }

            forwardedSpellManaCost = calculateForwardedSpellManaCost(context);
        }

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_COLOR_R, r);
            this.entityData.set(DATA_COLOR_G, g);
            this.entityData.set(DATA_COLOR_B, b);
        }
    }

    private double calculateForwardedSpellManaCost(SpellContext context) {
        if (context == null || context.getSpell() == null) {
            return 0.0;
        }

        var recipe = context.getSpell().unsafeList();
        boolean foundGeometrize = false;
        java.util.List<AbstractSpellPart> forwardedParts = new java.util.ArrayList<>();

        for (AbstractSpellPart part : recipe) {
            if (!foundGeometrize) {
                if (part == EffectGeometrize.INSTANCE) {
                    foundGeometrize = true;
                }
                continue;
            }

            forwardedParts.add(part);
        }

        if (forwardedParts.isEmpty()) {
            return 0.0;
        }

        Spell forwardedSpell = new Spell(forwardedParts);
        int totalCost = forwardedSpell.getCost();
        double multiplier = EffectGeometrize.INSTANCE.getGenericResolverManaCostMultiplier();

        return totalCost * multiplier;
    }

    public float getColorR() {
        return this.entityData.get(DATA_COLOR_R);
    }

    public float getColorG() {
        return this.entityData.get(DATA_COLOR_G);
    }

    public float getColorB() {
        return this.entityData.get(DATA_COLOR_B);
    }

    @Override
    protected float getBlocksPerTick() {
        return 1.0f;
    }

    @Override
    protected void tickProcess() {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        updateTargetBlock();

        if (this.processIndex >= this.processQueue.size()) {
            this.discard();
            return;
        }

        if (this.paused) {
            return;
        }

        Player claimActor = getClaimActor(serverLevel);
        if (claimActor == null) {
            return;
        }

        processGenericSpellResolution(serverLevel, claimActor);
    }

    private void processGenericSpellResolution(ServerLevel serverLevel, Player claimActor) {
        if (this.processIndex >= this.processQueue.size() || spellContext == null || spellResolver == null) {
            return;
        }

        if (!consumeManaForBlock(claimActor)) {
            return;
        }

        BlockPos targetPos = this.processQueue.get(this.processIndex);
        this.processIndex++;

        BlockHitResult blockHit = new BlockHitResult(
                Vec3.atCenterOf(targetPos), Direction.UP, targetPos, false);

        applyTimelineEffects(serverLevel, Vec3.atCenterOf(targetPos));

        SpellContext childContext = spellContext.makeChildContext();
        SpellResolver childResolver = spellResolver.getNewResolver(childContext);

        childResolver.onResolveEffect(serverLevel, blockHit);
    }

    private boolean consumeManaForBlock(Player player) {
        if (forwardedSpellManaCost <= 0.0) {
            return true;
        }

        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap != null && manaCap.getCurrentMana() >= forwardedSpellManaCost) {
            manaCap.removeMana(forwardedSpellManaCost);
            return true;
        }
        return false;
    }

    private void applyTimelineEffects(ServerLevel serverLevel, Vec3 position) {
        if (spellContext == null) {
            return;
        }

        GeometrizeTimeline timeline = spellContext.getParticleTimeline(ModParticleTimelines.GEOMETRIZE_TIMELINE.get());
        if (timeline == null) {
            return;
        }

        TimelineEntryData entryData = timeline.onResolvingEffect();
        if (entryData != null) {
            ParticleEmitter particleEmitter = new ParticleEmitter(() -> position, () -> new Vec2(0, 0),
                    entryData.motion(), entryData.particleOptions());
            particleEmitter.tick(serverLevel);
        }

        SoundProperty resolveSound = timeline.resolveSound();
        if (resolveSound != null && resolveSound.sound != null) {
            var spellSound = resolveSound.sound.getSound();
            if (spellSound != null) {
                var soundEventHolder = spellSound.getSoundEvent();
                if (soundEventHolder != null) {
                    serverLevel.playSound(null, BlockPos.containing(position), soundEventHolder.value(),
                            SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
            }
        }
    }

    @Override
    protected ProcessResult processBlock(ServerLevel level, BlockPos pos) {
        return ProcessResult.SKIPPED;
    }

    @Override
    protected SoundEvent getProcessSound(SoundType soundType) {
        return soundType.getPlaceSound();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR_R, 1.0f);
        builder.define(DATA_COLOR_G, 1.0f);
        builder.define(DATA_COLOR_B, 1.0f);
    }

    @Nullable
    private Player getClaimActor(ServerLevel level) {
        UUID caster = getCasterUUID();
        if (caster == null)
            return null;
        if (level.getServer() == null || level.getServer().getPlayerList() == null)
            return null;
        return level.getServer().getPlayerList().getPlayer(caster);
    }
}
