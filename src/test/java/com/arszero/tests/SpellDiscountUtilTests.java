package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.util.SpellDiscountUtil;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpellDiscountUtilTests {

	public static void registerGameTests(RegisterGameTestsEvent event) {
		if (TestRegistrationFilter.shouldRegister(SpellDiscountUtilTests.class)) {
			event.register(SpellDiscountUtilTests.class);
		}
	}

	@GameTest(batch = "SpellDiscountUtilTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
	public static void adjacentConjureWaterAddsPairCost(GameTestHelper helper) {
		AbstractSpellPart conjure = ConjureVoxelEffect.INSTANCE;
		AbstractSpellPart water = EffectConjureWater.INSTANCE;
		int expected = conjure.getCastingCost() + water.getCastingCost();
		int total = SpellDiscountUtil.computeAdjacentPairCost(List.of(conjure, water), EffectConjureWater.class);
		if (total != expected) {
			helper.fail("Expected adjacent pair cost " + expected + " but was " + total);
			return;
		}
		helper.succeed();
	}

	@GameTest(batch = "SpellDiscountUtilTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
	public static void twoAdjacentConjureIgnitePairsSumTwice(GameTestHelper helper) {
		AbstractSpellPart conjure = ConjureVoxelEffect.INSTANCE;
		AbstractSpellPart ignite = EffectIgnite.INSTANCE;
		int pair = conjure.getCastingCost() + ignite.getCastingCost();
		int total = SpellDiscountUtil.computeAdjacentPairCost(List.of(conjure, ignite, conjure, ignite), EffectIgnite.class);
		if (total != pair * 2) {
			helper.fail("Expected cost " + (pair * 2) + " but was " + total);
			return;
		}
		helper.succeed();
	}

	@GameTest(batch = "SpellDiscountUtilTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
	public static void nonAdjacentDoesNotCount(GameTestHelper helper) {
		int total = SpellDiscountUtil.computeAdjacentPairCost(List.of(EffectConjureWater.INSTANCE, ConjureVoxelEffect.INSTANCE), EffectConjureWater.class);
		if (total != 0) {
			helper.fail("Expected cost 0 for non-adjacent order but was " + total);
			return;
		}
		helper.succeed();
	}

	@GameTest(batch = "SpellDiscountUtilTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
	public static void reductionPercentThresholds(GameTestHelper helper) {
		if (Math.abs(SpellDiscountUtil.computeReductionPercent(0.0) - 0.0) > 0.0001) {
			helper.fail("Expected 0% at power 0");
			return;
		}
		if (Math.abs(SpellDiscountUtil.computeReductionPercent(0.5) - 40.0) > 0.0001) {
			helper.fail("Expected 40% at power 0.5");
			return;
		}
		if (Math.abs(SpellDiscountUtil.computeReductionPercent(2.0) - 60.0) > 0.0001) {
			helper.fail("Expected 60% at power 2");
			return;
		}
		if (Math.abs(SpellDiscountUtil.computeReductionPercent(3.0) - 75.0) > 0.0001) {
			helper.fail("Expected 75% at power 3");
			return;
		}
		if (Math.abs(SpellDiscountUtil.computeReductionPercent(4.0) - 85.0) > 0.0001) {
			helper.fail("Expected 85% at power 4");
			return;
		}
		if (Math.abs(SpellDiscountUtil.computeReductionPercent(5.0) - 90.0) > 0.0001) {
			helper.fail("Expected 90% at power 5");
			return;
		}
		double capped = SpellDiscountUtil.computeReductionPercent(100.0);
		if (Math.abs(capped - 95.0) > 0.0001) {
			helper.fail("Expected cap at 95% for very high power, was " + capped);
			return;
		}
		helper.succeed();
	}
}


