package com.github.ars_zero.mixin;

import com.github.ars_zero.client.gui.SpellCompositeContext;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GlyphButton.class)
public class GlyphButtonMixin {

    @Inject(method = "renderWidget", at = @At("TAIL"), remap = false)
    private void arsZero$renderSubsequentEffectHighlight(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        GlyphButton self = (GlyphButton) (Object) this;
        if (!self.visible || self.abstractSpellPart == null) {
            return;
        }
        
        SpellCompositeContext context = SpellCompositeContext.getInstance();
        if (!context.isSubsequentEffect(self.abstractSpellPart.getRegistryName())) {
            return;
        }
        
        int borderColor = 0xFFF1C550;
        int left = self.getX() - 1;
        int top = self.getY() - 1;
        int right = left + 18;
        int bottom = top + 18;
        graphics.fill(left, top, right, top + 1, borderColor);
        graphics.fill(left, bottom - 1, right, bottom, borderColor);
        graphics.fill(left, top, left + 1, bottom, borderColor);
        graphics.fill(right - 1, top, right, bottom, borderColor);
    }

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

