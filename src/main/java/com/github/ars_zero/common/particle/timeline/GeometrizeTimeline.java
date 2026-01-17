package com.github.ars_zero.common.particle.timeline;

import com.github.ars_zero.registry.ModParticleTimelines;
import com.google.common.collect.ImmutableList;
import com.hollingsworth.arsnouveau.api.particle.PropertyParticleOptions;
import com.hollingsworth.arsnouveau.api.particle.configurations.IParticleMotionType;
import com.hollingsworth.arsnouveau.api.particle.configurations.NoneMotion;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.BaseProperty;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.MotionProperty;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.SoundProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.BaseTimeline;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimelineType;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineOption;
import com.hollingsworth.arsnouveau.api.sound.ConfiguredSpellSound;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GeometrizeTimeline extends BaseTimeline<GeometrizeTimeline> {
    public static final MapCodec<GeometrizeTimeline> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        TimelineEntryData.CODEC.fieldOf("onResolvingEffect").forGetter(GeometrizeTimeline::onResolvingEffect),
        SoundProperty.CODEC.fieldOf("resolveSound").forGetter(GeometrizeTimeline::resolveSound)
    ).apply(instance, GeometrizeTimeline::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GeometrizeTimeline> STREAM_CODEC = StreamCodec.composite(
        TimelineEntryData.STREAM,
        GeometrizeTimeline::onResolvingEffect,
        SoundProperty.STREAM_CODEC,
        GeometrizeTimeline::resolveSound,
        GeometrizeTimeline::new
    );

    public static final List<IParticleMotionType<?>> RESOLVING_OPTIONS = new CopyOnWriteArrayList<>();

    private TimelineEntryData onResolvingEffect;
    private SoundProperty resolveSound = new SoundProperty();

    public GeometrizeTimeline() {
        this(new TimelineEntryData(new NoneMotion(), new PropertyParticleOptions()), new SoundProperty(ConfiguredSpellSound.EMPTY));
    }

    public GeometrizeTimeline(TimelineEntryData onResolvingEffect, SoundProperty resolveSound) {
        this.onResolvingEffect = onResolvingEffect;
        this.resolveSound = resolveSound;
    }

    public TimelineEntryData onResolvingEffect() {
        return onResolvingEffect;
    }

    public SoundProperty resolveSound() {
        return resolveSound;
    }

    @Override
    public IParticleTimelineType<GeometrizeTimeline> getType() {
        return ModParticleTimelines.GEOMETRIZE_TIMELINE.get();
    }

    @Override
    public List<BaseProperty<?>> getProperties() {
        return List.of(new MotionProperty(new TimelineOption(TimelineOption.IMPACT, onResolvingEffect, ImmutableList.copyOf(RESOLVING_OPTIONS)), List.of(resolveSound)));
    }
}
