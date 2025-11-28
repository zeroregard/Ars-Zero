package com.github.ars_zero.client.gui.validators;

import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellValidationError;
import com.hollingsworth.arsnouveau.common.spell.validation.BaseSpellValidationError;
import com.hollingsworth.arsnouveau.common.spell.validation.ScanningSpellValidator;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellPhaseValidator extends ScanningSpellValidator<AbstractMultiPhaseCastDeviceScreen.DevicePhase> {
    
    private static final Map<Class<? extends AbstractSpellPart>, EnumSet<AbstractMultiPhaseCastDeviceScreen.DevicePhase>> ALLOWED_PHASES = new HashMap<>();
    
    static {
        ALLOWED_PHASES.put(TemporalContextForm.class, EnumSet.of(
            AbstractMultiPhaseCastDeviceScreen.DevicePhase.TICK,
            AbstractMultiPhaseCastDeviceScreen.DevicePhase.END
        ));
        ALLOWED_PHASES.put(AnchorEffect.class, EnumSet.of(
            AbstractMultiPhaseCastDeviceScreen.DevicePhase.TICK
        ));
    }
    
    private final AbstractMultiPhaseCastDeviceScreen.DevicePhase phase;
    
    public SpellPhaseValidator(AbstractMultiPhaseCastDeviceScreen.DevicePhase phase) {
        this.phase = phase;
    }
    
    @Override
    protected AbstractMultiPhaseCastDeviceScreen.DevicePhase initContext() {
        return phase;
    }
    
    @Override
    protected void digestSpellPart(
            AbstractMultiPhaseCastDeviceScreen.DevicePhase ctx,
            int position,
            AbstractSpellPart part,
            List<SpellValidationError> errors
    ) {
        EnumSet<AbstractMultiPhaseCastDeviceScreen.DevicePhase> allowed = ALLOWED_PHASES.get(part.getClass());
        
        if (allowed != null && !allowed.contains(ctx)) {
            errors.add(new PhaseRestrictionValidationError(position, part, "phase_restriction"));
        }
    }
    
    private static class PhaseRestrictionValidationError extends BaseSpellValidationError {
        public PhaseRestrictionValidationError(int position, AbstractSpellPart spellPart, String localizationCode) {
            super(position, spellPart, localizationCode, spellPart);
        }
    }
}
