package com.github.ars_zero.client.registry;

import com.github.ars_zero.ArsZero;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModKeyBindings {

    public static final String CATEGORY = "key.category.ars_zero.general";
    public static final KeyMapping CURIO_CAST = new KeyMapping("key.ars_zero.curio_cast", GLFW.GLFW_KEY_U, CATEGORY);

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        ArsZero.LOGGER.debug("Registering Ars Zero key bindings");
        event.register(CURIO_CAST);
    }
}



