package com.github.ars_zero.client.registry;

import com.github.ars_zero.ArsNoita;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModKeyBindings {

    public static final String CATEGORY = "key.category.ars_zero.general";

    // For now, we'll use the same key as Ars Nouveau (C) for opening the staff GUI
    // This will be handled by Ars Nouveau's key handler since our staff implements ISpellHotkeyListener
    // If we want our own key binding later, we can add it here:
    // public static final KeyMapping OPEN_STAFF_GUI = new KeyMapping("key.ars_zero.open_staff_gui", GLFW.GLFW_KEY_C, CATEGORY);

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        ArsZero.LOGGER.debug("Registering Ars Zero key bindings");
        // For now, we don't need to register any key bindings since we're using Ars Nouveau's C key
        // If we add custom key bindings later, register them here:
        // event.register(OPEN_STAFF_GUI);
    }
}
