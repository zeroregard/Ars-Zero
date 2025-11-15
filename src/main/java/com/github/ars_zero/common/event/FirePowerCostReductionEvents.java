package com.github.ars_zero.common.event;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;

public class FirePowerCostReductionEvents {

    @SubscribeEvent
    public static void onSpellCostCalcPre(SpellCostCalcEvent.Pre event) {
        applyCostReduction(event);
    }

    @SubscribeEvent
    public static void onSpellCostCalcPost(SpellCostCalcEvent.Post event) {
        applyCostReduction(event);
    }

    private static void applyCostReduction(SpellCostCalcEvent event) {
        if (!(event.context.getCaster() instanceof LivingCaster caster)) {
            return;
        }
        if (!(caster.livingEntity instanceof Player player)) {
            return;
        }
        if (player instanceof FakePlayer) {
            return;
        }
        if (!hasFireVoxelGlyphs(event.context.getSpell().recipe())) {
            return;
        }

        AttributeInstance firePower = player.getAttribute(ModRegistry.FIRE_POWER);
        if (firePower == null) {
            return;
        }

        double power = firePower.getValue();
        if (power <= 0) {
            return;
        }

        double reductionPercent;
        if (power >= 4) {
            reductionPercent = Math.min(85.0 + (power - 4) * 5.0, 95.0);
        } else if (power >= 3) {
            reductionPercent = 75.0;
        } else if (power >= 2) {
            reductionPercent = 60.0;
        } else {
            reductionPercent = 40.0;
        }

        int totalReduction = (int) Math.ceil(event.currentCost * reductionPercent / 100.0);
        event.currentCost = Math.max(0, event.currentCost - totalReduction);
    }

    private static boolean hasFireVoxelGlyphs(Iterable<AbstractSpellPart> recipe) {
        boolean hasVoxel = false;
        boolean hasFire = false;

        for (AbstractSpellPart part : recipe) {
            if (part instanceof ConjureVoxelEffect) {
                hasVoxel = true;
            }
            if (part instanceof EffectIgnite) {
                hasFire = true;
            }
            if (hasVoxel && hasFire) {
                return true;
            }
        }

        return false;
    }
}
