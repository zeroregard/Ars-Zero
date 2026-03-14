package com.github.ars_zero.client.jade;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class EntityManaProvider implements IServerDataProvider<EntityAccessor> {

    public static final EntityManaProvider INSTANCE = new EntityManaProvider();
    static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("ars_zero", "entity_mana");
    private static final String KEY_CURRENT = "mana_current";
    private static final String KEY_MAX = "mana_max";

    @Override
    public void appendServerData(@NotNull CompoundTag data, @NotNull EntityAccessor accessor) {
        IManaCap mana = CapabilityRegistry.getMana((LivingEntity) accessor.getEntity());
        if (mana != null) {
            data.putFloat(KEY_CURRENT, (float) mana.getCurrentMana());
            data.putInt(KEY_MAX, mana.getMaxMana());
        }
    }

    @Override
    public boolean shouldRequestData(@NotNull EntityAccessor accessor) {
        return accessor.getEntity() instanceof AbstractBlightedSkeleton;
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return UID;
    }

    public static class Client extends EntityManaProvider implements IEntityComponentProvider {

        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(
                @NotNull ITooltip tooltip,
                @NotNull EntityAccessor accessor,
                @NotNull IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_CURRENT)) return;
            int current = (int) data.getFloat(KEY_CURRENT);
            int max = data.getInt(KEY_MAX);
            tooltip.add(Component.literal(String.format("Mana: %d / %d", current, max))
                    .withStyle(style -> style.withColor(0x9D4BDD)));
        }
    }
}
