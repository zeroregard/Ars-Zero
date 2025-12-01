package com.github.ars_zero.client.gui.validators;

<<<<<<< HEAD
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
=======
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.spell.SpellPhase;
>>>>>>> 4e707db5d1116aed23d09488f1991c81eaf936ba
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellValidationError;
import com.hollingsworth.arsnouveau.common.spell.validation.BaseSpellValidationError;
import com.hollingsworth.arsnouveau.common.spell.validation.ScanningSpellValidator;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SpellPhaseValidator extends ScanningSpellValidator<SpellPhase> {
    
    private static final Map<Class<? extends AbstractSpellPart>, EnumSet<SpellPhase>> ALLOWED_PHASES = new HashMap<>();
    
    static {
        ALLOWED_PHASES.put(TemporalContextForm.class, EnumSet.of(
            SpellPhase.TICK,
            SpellPhase.END
        ));
        ALLOWED_PHASES.put(AnchorEffect.class, EnumSet.of(
            SpellPhase.TICK
        ));
    }
    
    private final SpellPhase phase;
    
    public SpellPhaseValidator(SpellPhase phase) {
>>>>>>> 4e707db5d1116aed23d09488f1991c81eaf936ba
        this.phase = phase;
    }
    
    @Override

    protected SpellPhase initContext() {
>>>>>>> 4e707db5d1116aed23d09488f1991c81eaf936ba
        return phase;
    }
    
    @Override
    protected void digestSpellPart(
<<<<<<< HEAD
            AbstractMultiPhaseCastDeviceScreen.DevicePhase ctx,
=======
            SpellPhase ctx,
>>>>>>> 4e707db5d1116aed23d09488f1991c81eaf936ba
            int position,
            AbstractSpellPart part,
            List<SpellValidationError> errors
    ) {
<<<<<<< HEAD
        EnumSet<AbstractMultiPhaseCastDeviceScreen.DevicePhase> allowed = ALLOWED_PHASES.get(part.getClass());
=======
        EnumSet<SpellPhase> allowed = ALLOWED_PHASES.get(part.getClass());
>>>>>>> 4e707db5d1116aed23d09488f1991c81eaf936ba
        
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
