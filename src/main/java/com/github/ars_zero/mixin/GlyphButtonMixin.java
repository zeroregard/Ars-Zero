package com.github.ars_zero.mixin;

import com.github.ars_zero.client.gui.SpellCompositeContext;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GlyphButton.class)
public class GlyphButtonMixin {

    @Inject(method = "getTooltip", at = @At("TAIL"), remap = false)
    private void arsZero$injectSubsequentEffectTooltip(List<Component> tip, CallbackInfo ci) {
        GlyphButton self = (GlyphButton) (Object) this;
        if (self.abstractSpellPart == null) {
            return;
        }
        
        SpellCompositeContext context = SpellCompositeContext.getInstance();
        if (!context.isSubsequentEffect(self.abstractSpellPart.getRegistryName())) {
            return;
        }
        
        if (!Screen.hasShiftDown()) {
            return;
        }
        
        AbstractSpellPart lastEffect = context.getLastEffect();
        if (lastEffect instanceof ISubsequentEffectProvider provider) {
            Component tooltip = provider.createSubsequentGlyphTooltip(self.abstractSpellPart.getRegistryName());
            if (tooltip != null) {
                tip.add(tooltip.copy().withStyle(ChatFormatting.AQUA));
            }
        }
    }
}

