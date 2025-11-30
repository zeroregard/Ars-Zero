package com.github.ars_zero.mixin;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.SpellCompositeContext;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.client.ClientInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GlyphButton.class)
public class GlyphButtonMixin {

    private static final ResourceLocation COMPOSITE_EFFECT_TEXTURE = ArsZero.prefix("textures/gui/abstract/composite_effect.png");

    @Inject(method = "renderWidget", at = @At(value = "TAIL"), remap = false)
    private void arsZero$renderSubsequentEffectHighlight(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        GlyphButton self = (GlyphButton) (Object) this;
        if (!self.visible || self.abstractSpellPart == null) {
            return;
        }
        
        SpellCompositeContext context = SpellCompositeContext.getInstance();
        if (!context.isSubsequentEffect(self.abstractSpellPart.getRegistryName())) {
            return;
        }
        
        int x = self.getX();
        int y = self.getY();
        
        // Render at a higher z-level to ensure it appears on top of the glyph
        // The glyph is rendered at z=0, so we render at z=100 to be on top
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 100);
        
        // Render the 18x18 composite effect texture, centered on the 16x16 glyph
        // Offset by -1 to center it
        graphics.blit(COMPOSITE_EFFECT_TEXTURE, x - 1, y - 1, 0, 0, 18, 18, 18, 18);
        
        // Add a diagonal shine effect - a moving white gradient overlay at 45 degrees
        int ticks = ClientInfo.ticksInGame;
        int animationLength = 20; // Animation happens in 20 ticks (1 second, twice as fast as before)
        int delayLength = 30; // 30 tick delay (75% of previous 40 ticks)
        int cycleLength = animationLength + delayLength; // Total cycle: 50 ticks (2.5 seconds)
        
        int cyclePosition = ticks % cycleLength;
        
        // Only animate during the animation phase, not during the delay
        if (cyclePosition < animationLength) {
            float shineProgress = cyclePosition / (float) animationLength; // 0.0 to 1.0
            
            // Calculate diagonal position (45 degrees: moves from bottom-left to top-right)
            // Diagonal distance across the 18x18 square
            float diagonalLength = (float) Math.sqrt(18 * 18 + 18 * 18); // ~25.5 pixels
            int shineWidth = 6;
            
            // Position along the diagonal (0 = bottom-left, 1 = top-right)
            float diagonalPos = shineProgress * (diagonalLength + shineWidth) - shineWidth;
            
            // Draw the diagonal shine line
            // For each pixel in the texture, check if it's on the shine line
            for (int px = 0; px < 18; px++) {
                for (int py = 0; py < 18; py++) {
                    // Calculate distance from this pixel to the diagonal line
                    // Line equation: y = 18 - x (45 degrees from bottom-left to top-right)
                    // Distance from point (px, py) to line y = 18 - x + b
                    // where b is the offset based on diagonalPos
                    float lineOffset = diagonalPos;
                    float distanceToLine = Math.abs(py - (18 - px) - lineOffset) / (float) Math.sqrt(2);
                    
                    if (distanceToLine < shineWidth / 2.0f) {
                        // Calculate alpha based on distance from center of shine
                        float alphaFactor = 1.0f - (distanceToLine / (shineWidth / 2.0f));
                        int alpha = (int)(255 * alphaFactor * 0.5f);
                        int color = (alpha << 24) | 0xFFFFFF; // White with alpha
                        graphics.fill(x - 1 + px, y - 1 + py, x - 1 + px + 1, y - 1 + py + 1, color);
                    }
                }
            }
        }
        
        pose.popPose();
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

