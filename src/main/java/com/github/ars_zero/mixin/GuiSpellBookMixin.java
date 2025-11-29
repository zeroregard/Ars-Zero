package com.github.ars_zero.mixin;

import com.github.ars_zero.client.gui.SpellCompositeContext;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiSpellBook.class)
public class GuiSpellBookMixin {

    @Inject(method = "validate()V", at = @At("TAIL"), remap = false)
    private void arsZero$updateSpellContext(CallbackInfo ci) {
        GuiSpellBook self = (GuiSpellBook) (Object) this;
        SpellCompositeContext.getInstance().setCurrentSpell(self.spell);
    }
}

