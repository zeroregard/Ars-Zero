package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.registry.ModParticleTimelines;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.SoundProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class DiscardEffect extends AbstractEffect {

    public static final String ID = "discard_effect";
    public static final DiscardEffect INSTANCE = new DiscardEffect();

    public ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;

    public DiscardEffect() {
        super(ArsZero.prefix(ID), "Discard");
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        builder.comment("Discard Effect Settings").push("discard");
        BLACKLIST = builder.comment(
                "List of entity resource locations that cannot be discarded by the Discard effect.",
                "Format: [\"namespace:entity_id\", \"namespace:entity_id\", ...]",
                "Example: [\"minecraft:tnt\"]",
                "Entities in this list will be protected from being discarded.").defineList("blacklist",
                        List.of("minecraft:tnt"),
                        obj -> obj instanceof String s && ResourceLocation.read(s).isSuccess());
        builder.pop();
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter,
            SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) {
            return;
        }

        Entity target = rayTraceResult.getEntity();
        if (target == null) {
            return;
        }

        if (target instanceof LivingEntity) {
            return;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (entityId != null && BLACKLIST != null) {
            String entityIdString = entityId.toString();
            List<? extends String> blacklist = BLACKLIST.get();
            if (blacklist.contains(entityIdString)) {
                return;
            }
        }

        if (target.isRemoved()) {
            return;
        }

        Vec3 position = target.position();
        triggerResolveEffects(spellContext, world, position);

        if (target instanceof BaseVoxelEntity voxel) {
            voxel.resolveAndDiscardSelf();
            return;
        }

        target.discard();
    }

    private void triggerResolveEffects(SpellContext spellContext, Level world, Vec3 position) {
        if (world == null) {
            return;
        }
        var timeline = spellContext.getParticleTimeline(ModParticleTimelines.DISCARD_TIMELINE.get());
        TimelineEntryData entryData = timeline.onResolvingEffect();
        ParticleEmitter particleEmitter = createStaticEmitter(entryData, position);
        particleEmitter.tick(world);
        SoundProperty resolveSound = timeline.resolveSound();
        resolveSound.sound.playSound(world, position.x, position.y, position.z);
    }

    @Override
    public int getDefaultManaCost() {
        return 5;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of();
    }

    @Override
    public String getBookDescription() {
        return "Discards non-living entities. Useful for removing Arcane Voxels to trigger their resolve behavior.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }
}
