package com.github.ars_zero.client.event;

import com.github.ars_zero.client.gui.GuiManaDrainOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "ars_zero", value = Dist.CLIENT)
public class ManaDrainOverlayHandler {

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    GuiManaDrainOverlay.tick();
  }
}
