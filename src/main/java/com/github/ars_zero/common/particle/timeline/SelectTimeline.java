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

/** Spell timeline for Select effect; color is used for block group outline. */
public class SelectTimeline extends BaseTimeline<SelectTimeline> {
    public static final MapCodec<SelectTimeline> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        PropMap.CODEC.fieldOf("propMap").forGetter(i -> i.propMap)
    ).apply(instance, SelectTimeline::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectTimeline> STREAM_CODEC = StreamCodec.composite(
        PropMap.STREAM_CODEC,
        i -> i.propMap,
        SelectTimeline::new
    );

    public PropMap propMap;

    public SelectTimeline() {
        this(new PropMap());
    }

    public SelectTimeline(PropMap propMap) {
        this.propMap = propMap == null ? new PropMap() : propMap;
        this.propMap.createIfMissing(new ColorProperty(ParticleColor.CYAN, false));
    }

    public ParticleColor getColor() {
        return propMap.getParticleColor();
    }

    @Override
    public IParticleTimelineType<SelectTimeline> getType() {
        return ModParticleTimelines.SELECT_TIMELINE.get();
    }

    @Override
    public List<BaseProperty<?>> getProperties() {
        BaseProperty<?> colorProp = propMap.createIfMissing(new ColorProperty(ParticleColor.CYAN, false));
        colorProp.propertyHolder = this.propMap;
        return List.of(colorProp);
    }
}
