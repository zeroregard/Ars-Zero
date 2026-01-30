package com.github.ars_zero.common.particle.timeline;

import com.github.ars_zero.registry.ModParticleTimelines;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.BaseProperty;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.ColorProperty;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.PropMap;
import com.hollingsworth.arsnouveau.api.particle.timelines.BaseTimeline;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimelineType;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public class BeamTimeline extends BaseTimeline<BeamTimeline> {
    public static final MapCodec<BeamTimeline> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        PropMap.CODEC.fieldOf("propMap").forGetter(i -> i.propMap)
    ).apply(instance, BeamTimeline::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeamTimeline> STREAM_CODEC = StreamCodec.composite(
        PropMap.STREAM_CODEC,
        i -> i.propMap,
        BeamTimeline::new
    );

    public PropMap propMap;

    public BeamTimeline() {
        this(new PropMap());
    }

    public BeamTimeline(PropMap propMap) {
        this.propMap = propMap == null ? new PropMap() : propMap;
        this.propMap.createIfMissing(new ColorProperty(ParticleColor.WHITE, false));
    }

    public ParticleColor getColor() {
        return propMap.getParticleColor();
    }

    @Override
    public IParticleTimelineType<BeamTimeline> getType() {
        return ModParticleTimelines.BEAM_TIMELINE.get();
    }

    @Override
    public List<BaseProperty<?>> getProperties() {
        return List.of(propMap.createIfMissing(new ColorProperty(ParticleColor.WHITE, false)));
    }
}
