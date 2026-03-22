package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.particle.timeline.BeamTimeline;
import com.github.ars_zero.common.particle.timeline.ConvergenceTimeline;
import com.github.ars_zero.common.particle.timeline.DiscardTimeline;
import com.github.ars_zero.common.particle.timeline.GeometrizeTimeline;
import com.github.ars_zero.common.particle.timeline.NearTimeline;
import com.github.ars_zero.common.particle.timeline.SelectTimeline;
import com.github.ars_zero.common.particle.timeline.TemporalContextTimeline;
import com.hollingsworth.arsnouveau.api.particle.configurations.IParticleMotionType;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimelineType;
import com.hollingsworth.arsnouveau.api.particle.timelines.SimpleParticleTimelineType;
import com.hollingsworth.arsnouveau.api.registry.ParticleMotionRegistry;
import com.hollingsworth.arsnouveau.api.registry.ParticleTimelineRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticleTimelines {
    public static final DeferredRegister<IParticleTimelineType<?>> TIMELINES = DeferredRegister.create(ParticleTimelineRegistry.PARTICLE_TIMELINE_REGISTRY, ArsZero.MOD_ID);

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<NearTimeline>> NEAR_TIMELINE = TIMELINES
            .register(
                    "near",
                    () -> new SimpleParticleTimelineType<>(ModGlyphs.NEAR_FORM, NearTimeline.CODEC,
                            NearTimeline.STREAM_CODEC, NearTimeline::new));

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<TemporalContextTimeline>> TEMPORAL_CONTEXT_TIMELINE = TIMELINES
            .register(
                    "temporal_context",
                    () -> new SimpleParticleTimelineType<>(ModGlyphs.TEMPORAL_CONTEXT_FORM,
                            TemporalContextTimeline.CODEC, TemporalContextTimeline.STREAM_CODEC,
                            TemporalContextTimeline::new));

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<ConvergenceTimeline>> CONVERGENCE_TIMELINE = TIMELINES
            .register(
                    "convergence",
                    () -> new SimpleParticleTimelineType<>(ModGlyphs.EFFECT_CONVERGENCE, ConvergenceTimeline.CODEC,
                            ConvergenceTimeline.STREAM_CODEC, ConvergenceTimeline::new));

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<DiscardTimeline>> DISCARD_TIMELINE = TIMELINES
            .register(
                    "discard",
                    () -> new SimpleParticleTimelineType<>(ModGlyphs.DISCARD_EFFECT, DiscardTimeline.CODEC,
                            DiscardTimeline.STREAM_CODEC, DiscardTimeline::new));

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GeometrizeTimeline>> GEOMETRIZE_TIMELINE = TIMELINES
            .register(
                    "geometrize",
                    () -> new SimpleParticleTimelineType<>(ModGlyphs.EFFECT_GEOMETRIZE, GeometrizeTimeline.CODEC,
                            GeometrizeTimeline.STREAM_CODEC, GeometrizeTimeline::new));

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<BeamTimeline>> BEAM_TIMELINE = TIMELINES
            .register(
                    "beam",
                    () -> new SimpleParticleTimelineType<>(ModGlyphs.EFFECT_BEAM, BeamTimeline.CODEC,
                            BeamTimeline.STREAM_CODEC, BeamTimeline::new));

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<SelectTimeline>> SELECT_TIMELINE = TIMELINES
            .register(
                    "select",
                    () -> new SimpleParticleTimelineType<>(ModGlyphs.SELECT_EFFECT, SelectTimeline.CODEC,
                            SelectTimeline.STREAM_CODEC, SelectTimeline::new));

    public static void init(IEventBus eventBus) {
        TIMELINES.register(eventBus);
    }

    public static void configureTimelineOptions() {
        IParticleMotionType<?> burst = ParticleMotionRegistry.BURST_TYPE.get();
        IParticleMotionType<?> none = ParticleMotionRegistry.NONE_TYPE.get();
        NearTimeline.RESOLVING_OPTIONS.add(none);
        NearTimeline.RESOLVING_OPTIONS.add(burst);
        TemporalContextTimeline.RESOLVING_OPTIONS.add(none);
        TemporalContextTimeline.RESOLVING_OPTIONS.add(burst);
        ConvergenceTimeline.RESOLVING_OPTIONS.add(none);
        ConvergenceTimeline.RESOLVING_OPTIONS.add(burst);
        DiscardTimeline.RESOLVING_OPTIONS.add(none);
        DiscardTimeline.RESOLVING_OPTIONS.add(burst);
        GeometrizeTimeline.RESOLVING_OPTIONS.add(none);
        GeometrizeTimeline.RESOLVING_OPTIONS.add(burst);
    }
}
