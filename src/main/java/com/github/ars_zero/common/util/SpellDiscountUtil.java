package com.github.ars_zero.common.util;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;

public class SpellDiscountUtil {

	public static int computeAdjacentPairCost(Iterable<AbstractSpellPart> recipe, Class<? extends AbstractSpellPart> effectClass) {
		AbstractSpellPart prev = null;
		int total = 0;
		for (AbstractSpellPart part : recipe) {
			if (prev instanceof ConjureVoxelEffect && effectClass.isInstance(part)) {
				total += ((ConjureVoxelEffect) prev).getCastingCost();
				total += part.getCastingCost();
			}
			prev = part;
		}
		return total;
	}

	public static double computeReductionPercent(double power) {
		if (power >= 4) {
			return Math.min(85.0 + (power - 4) * 5.0, 95.0);
		} else if (power >= 3) {
			return 75.0;
		} else if (power >= 2) {
			return 60.0;
		} else if (power > 0) {
			return 40.0;
		}
		return 0.0;
	}
}

