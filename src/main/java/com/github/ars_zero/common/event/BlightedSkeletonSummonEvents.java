package com.github.ars_zero.common.event;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.hollingsworth.arsnouveau.api.entity.ISummon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Prevents summons owned by blighted skeletons from targeting blighted skeletons
 * or other summons (so Necromancer/Lich revenants do not attack each other or player summons).
 */
@EventBusSubscriber(modid = com.github.ars_zero.ArsZero.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BlightedSkeletonSummonEvents {

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !(event.getEntity() instanceof ISummon summon)) {
            return;
        }
        if (!(summon.getOwner() instanceof AbstractBlightedSkeleton)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (target instanceof AbstractBlightedSkeleton || target instanceof ISummon) {
            mob.setTarget(null);
        }
    }
}
