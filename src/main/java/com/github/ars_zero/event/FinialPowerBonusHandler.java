package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "ars_zero")
public class FinialPowerBonusHandler {
    
    private static final ResourceLocation FINIAL_POWER_BONUS_ID = ArsZero.prefix("finial_power_bonus");
    
    @SubscribeEvent
    public static void onSpellResolvePre(SpellResolveEvent.Pre event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver.spellContext.getCaster() instanceof Player player)) {
            return;
        }
        
        ItemStack casterTool = event.resolver.spellContext.getCasterTool();
        if (casterTool.isEmpty() || !(casterTool.getItem() instanceof AbstractSpellStaff)) {
            return;
        }
        
        String finial = AbstractSpellStaff.getFinial(casterTool);
        if (finial == null) {
            return;
        }
        
        AttributeInstance powerAttribute = getPowerAttributeForFinial(player, finial);
        if (powerAttribute != null) {
            AttributeModifier modifier = new AttributeModifier(
                FINIAL_POWER_BONUS_ID,
                1.0,
                AttributeModifier.Operation.ADD_VALUE
            );
            powerAttribute.addTransientModifier(modifier);
        }
    }
    
    @SubscribeEvent
    public static void onSpellResolvePost(SpellResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver.spellContext.getCaster() instanceof Player player)) {
            return;
        }
        
        ItemStack casterTool = event.resolver.spellContext.getCasterTool();
        if (casterTool.isEmpty() || !(casterTool.getItem() instanceof AbstractSpellStaff)) {
            return;
        }
        
        String finial = AbstractSpellStaff.getFinial(casterTool);
        if (finial == null) {
            return;
        }
        
        AttributeInstance powerAttribute = getPowerAttributeForFinial(player, finial);
        if (powerAttribute != null) {
            powerAttribute.removeModifier(FINIAL_POWER_BONUS_ID);
        }
    }
    
    private static AttributeInstance getPowerAttributeForFinial(Player player, String finial) {
        return switch (finial) {
            case "fire" -> player.getAttribute(ModRegistry.FIRE_POWER);
            case "water" -> player.getAttribute(ModRegistry.WATER_POWER);
            default -> null;
        };
    }
}
