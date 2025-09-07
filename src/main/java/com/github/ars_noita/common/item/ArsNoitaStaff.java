package com.github.ars_noita.common.item;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.client.gui.ArsNoitaStaffGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ArsNoitaStaff extends Item {
    
    public enum StaffPhase {
        BEGIN,
        TICK,
        END
    }
    
    private StaffPhase currentPhase = StaffPhase.BEGIN;
    private boolean isHeld = false;
    private int tickCount = 0;

    public ArsNoitaStaff() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                openStaffGUI(player);
                return InteractionResultHolder.success(stack);
            } else {
                beginPhase(player, stack);
                return InteractionResultHolder.success(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openStaffGUI(Player player) {
        Minecraft.getInstance().setScreen(new ArsNoitaStaffGUI());
    }

    private void beginPhase(Player player, ItemStack stack) {
        currentPhase = StaffPhase.BEGIN;
        isHeld = true;
        tickCount = 0;
        
        // Execute begin spell
        executeSpell(player, stack, StaffPhase.BEGIN);
    }

    public void tickPhase(Player player, ItemStack stack) {
        if (isHeld) {
            currentPhase = StaffPhase.TICK;
            tickCount++;
            
            // Execute tick spell
            executeSpell(player, stack, StaffPhase.TICK);
        }
    }

    public void endPhase(Player player, ItemStack stack) {
        if (isHeld) {
            currentPhase = StaffPhase.END;
            isHeld = false;
            
            // Execute end spell
            executeSpell(player, stack, StaffPhase.END);
        }
    }

    private void executeSpell(Player player, ItemStack stack, StaffPhase phase) {
        // TODO: Implement spell execution based on phase
        // This will integrate with Ars Nouveau's spell system
        ArsNoita.LOGGER.info("Executing {} spell for staff", phase.name());
    }

    public StaffPhase getCurrentPhase() {
        return currentPhase;
    }

    public boolean isHeld() {
        return isHeld;
    }

    public int getTickCount() {
        return tickCount;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("item.ars_noita.ars_noita_staff.desc");
    }
}
