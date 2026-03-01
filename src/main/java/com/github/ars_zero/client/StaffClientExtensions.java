package com.github.ars_zero.client;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.StaticStaffRendererProvider;
import com.github.ars_zero.common.item.StaticStaffRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = ArsZero.MOD_ID)
public class StaffClientExtensions {

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        StaticStaffRendererRegistry.RENDERER_FACTORY = StaticStaffRendererProvider::create;

        IClientItemExtensions staffExtensions = new IClientItemExtensions() {
            @Nullable
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack itemStack) {
                if (entity instanceof Player player && player.isUsingItem()) {
                    return HumanoidModel.ArmPose.EMPTY;
                }
                return HumanoidModel.ArmPose.ITEM;
            }
        };
        
        event.registerItem(staffExtensions,
            com.github.ars_zero.registry.ModItems.NOVICE_SPELL_STAFF.get(),
            com.github.ars_zero.registry.ModItems.MAGE_SPELL_STAFF.get(),
            com.github.ars_zero.registry.ModItems.ARCHMAGE_SPELL_STAFF.get(),
            com.github.ars_zero.registry.ModItems.CREATIVE_SPELL_STAFF.get()
        );
        for (var holder : com.github.ars_zero.registry.ModStaffItems.getRegisteredStaticStaffs()) {
            event.registerItem(staffExtensions, holder.get());
        }
    }
}
