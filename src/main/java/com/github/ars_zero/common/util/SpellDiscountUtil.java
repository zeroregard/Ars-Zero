package com.github.ars_zero.common.util;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;

import java.util.List;

public class SpellDiscountUtil {

	public static int computeAdjacentPairCost(Iterable<AbstractSpellPart> recipe, List<Class<? extends AbstractSpellPart>> effectClasses) {
		AbstractSpellPart prev = null;
		int total = 0;
		for (AbstractSpellPart part : recipe) {
			if (prev instanceof ConjureVoxelEffect) {
				for (Class<? extends AbstractSpellPart> effectClass : effectClasses) {
					if (effectClass.isInstance(part)) {
						total += ((ConjureVoxelEffect) prev).getCastingCost();
						total += part.getCastingCost();
						break;
					}
				}
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

