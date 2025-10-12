package com.github.ars_zero.client.renderer;

import com.github.ars_zero.common.item.ArsZeroStaff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StaffDebugHUD {
    
    private static long lastBeginTime = 0;
    private static long lastTickTime = 0;
    private static long lastEndTime = 0;
    private static String lastSpellFired = "NONE";
    private static int consecutiveTicks = 0;
    private static boolean wasHolding = false;
    private static long sessionStartTime = 0;
    private static int beginFireCount = 0;
    private static int tickFireCount = 0;
    private static int endFireCount = 0;
    private static long lastPhaseChangeTime = 0;
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        
        Player player = mc.player;
        ItemStack mainHandItem = player.getMainHandItem();
        
        if (!(mainHandItem.getItem() instanceof ArsZeroStaff staff)) {
            if (wasHolding) {
                reset();
            }
            wasHolding = false;
            return;
        }
        
        boolean isUsing = player.isUsingItem() && player.getUseItem() == mainHandItem;
        
        if (isUsing && !wasHolding) {
            sessionStartTime = System.currentTimeMillis();
        }
        wasHolding = isUsing;
        
        GuiGraphics graphics = event.getGuiGraphics();
        
        List<String> debugInfo = new ArrayList<>();
        debugInfo.add("=== STAFF DEBUG INFO ===");
        debugInfo.add("");
        
        long currentTime = System.currentTimeMillis();
        debugInfo.add(String.format("Session Time: %.1fs", sessionStartTime > 0 ? (currentTime - sessionStartTime) / 1000.0 : 0));
        debugInfo.add("");
        
        debugInfo.add("Current State:");
        debugInfo.add(String.format("  Phase: %s", staff.getCurrentPhase(player.getUUID())));
        debugInfo.add(String.format("  Is Held (Item): %s", staff.isHeld(player.getUUID())));
        debugInfo.add(String.format("  Tick Count: %d", staff.getTickCount(player.getUUID())));
        debugInfo.add(String.format("  Using Item (MC): %s", isUsing));
        debugInfo.add(String.format("  Time Since Phase Change: %dms", lastPhaseChangeTime > 0 ? currentTime - lastPhaseChangeTime : 0));
        debugInfo.add("");
        
        debugInfo.add("Internal Flags:");
        debugInfo.add(String.format("  Player Holding Staff: %s", ArsZeroStaff.isPlayerHoldingStaff(player.getUUID())));
        Boolean outOfMana = ArsZeroStaff.isPlayerOutOfMana(player.getUUID());
        debugInfo.add(String.format("  Out of Mana: %s", outOfMana != null ? outOfMana : "false"));
        int sequenceTick = ArsZeroStaff.getSequenceTick(player.getUUID());
        debugInfo.add(String.format("  Sequence Tick: %d", sequenceTick));
        debugInfo.add("");
        
        debugInfo.add("Action Counts:");
        debugInfo.add(String.format("  Mouse downs (use()): %d", ArsZeroStaff.getUseCount(player.getUUID())));
        debugInfo.add(String.format("  Mouse ups (release): %d", ArsZeroStaff.getReleaseCount(player.getUUID())));
        debugInfo.add(String.format("  BEGIN spells fired: %d", ArsZeroStaff.getBeginCount(player.getUUID())));
        debugInfo.add(String.format("  END spells fired: %d", ArsZeroStaff.getEndCount(player.getUUID())));
        debugInfo.add("");
        
        debugInfo.add("Spell Fire Counts:");
        debugInfo.add(String.format("  BEGIN: %d times", beginFireCount));
        debugInfo.add(String.format("  TICK: %d times", tickFireCount));
        debugInfo.add(String.format("  END: %d times", endFireCount));
        debugInfo.add("");
        
        debugInfo.add("Last Spell Fired:");
        debugInfo.add(String.format("  Type: %s", lastSpellFired));
        debugInfo.add(String.format("  BEGIN: %dms ago", lastBeginTime > 0 ? currentTime - lastBeginTime : -1));
        debugInfo.add(String.format("  TICK: %dms ago", lastTickTime > 0 ? currentTime - lastTickTime : -1));
        debugInfo.add(String.format("  END: %dms ago", lastEndTime > 0 ? currentTime - lastEndTime : -1));
        debugInfo.add("");
        
        debugInfo.add(String.format("Consecutive Ticks: %d", consecutiveTicks));
        debugInfo.add("");
        
        int useCount = ArsZeroStaff.getUseCount(player.getUUID());
        int beginCount = ArsZeroStaff.getBeginCount(player.getUUID());
        int endCount = ArsZeroStaff.getEndCount(player.getUUID());
        
        if (useCount != beginCount) {
            debugInfo.add(String.format("WARNING: Right-clicks (%d) != BEGIN spells (%d)!", useCount, beginCount));
        }
        
        if (beginCount != endCount) {
            debugInfo.add(String.format("WARNING: BEGIN spells (%d) != END spells (%d)!", beginCount, endCount));
        }
        
        if (tickFireCount > 0 && beginFireCount == 0) {
            debugInfo.add("WARNING: TICK fired without BEGIN!");
        }
        
        int x = 10;
        int y = 10;
        int lineHeight = 10;
        int backgroundColor = 0x80000000;
        
        for (String line : debugInfo) {
            int textWidth = mc.font.width(line);
            graphics.fill(x - 2, y - 1, x + textWidth + 2, y + lineHeight - 1, backgroundColor);
            graphics.drawString(mc.font, line, x, y, 0xFFFFFF, false);
            y += lineHeight;
        }
    }
    
    public static void onSpellFired(ArsZeroStaff.StaffPhase phase) {
        long currentTime = System.currentTimeMillis();
        lastSpellFired = phase.name();
        lastPhaseChangeTime = currentTime;
        
        switch (phase) {
            case BEGIN:
                lastBeginTime = currentTime;
                consecutiveTicks = 0;
                beginFireCount++;
                break;
            case TICK:
                lastTickTime = currentTime;
                consecutiveTicks++;
                tickFireCount++;
                break;
            case END:
                lastEndTime = currentTime;
                consecutiveTicks = 0;
                endFireCount++;
                break;
        }
    }
    
    public static void reset() {
        lastBeginTime = 0;
        lastTickTime = 0;
        lastEndTime = 0;
        lastSpellFired = "NONE";
        consecutiveTicks = 0;
        sessionStartTime = 0;
        beginFireCount = 0;
        tickFireCount = 0;
        endFireCount = 0;
        lastPhaseChangeTime = 0;
    }
}

