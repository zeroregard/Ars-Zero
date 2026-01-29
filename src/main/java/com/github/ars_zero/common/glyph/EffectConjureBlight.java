package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModFluids;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.items.curios.ShapersFocus;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EffectConjureBlight extends AbstractEffect {

    public static EffectConjureBlight INSTANCE = new EffectConjureBlight();

    private EffectConjureBlight() {
        super(ArsZero.prefix("effect_conjure_blight"), "Conjure Blight");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        Entity entity = rayTraceResult.getEntity();
        if (spellStats.isSensitive()) {
            placeBlight((ServerLevel) world, shooter, spellContext, resolver, entity.blockPosition(), Direction.UP);
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        double aoeBuff = spellStats.getAoeMultiplier();
        List<BlockPos> posList = SpellUtil.calcAOEBlocks(shooter, rayTraceResult.getBlockPos(), rayTraceResult, aoeBuff, spellStats.getBuffCount(AugmentPierce.INSTANCE));
        for (BlockPos pos1 : posList) {
            placeBlight((ServerLevel) world, shooter, spellContext, resolver, pos1, rayTraceResult.getDirection());
        }
    }

    private void placeBlight(ServerLevel world, @NotNull LivingEntity shooter, SpellContext spellContext, SpellResolver resolver, BlockPos pos1, Direction direction) {
        if (!BlockUtil.destroyRespectsClaim(getPlayer(shooter, world), world, pos1))
            return;
        if (!world.isInWorldBounds(pos1))
            return;
        BlockState hitState = world.getBlockState(pos1);
        if (hitState.getBlock() instanceof LiquidBlockContainer liquidBlockContainer && liquidBlockContainer.canPlaceLiquid(getPlayer(shooter, world), world, pos1, world.getBlockState(pos1), ModFluids.BLIGHT_FLUID.get())) {
            liquidBlockContainer.placeLiquid(world, pos1, hitState, ModFluids.BLIGHT_FLUID.get().getSource(true));
            ShapersFocus.tryPropagateBlockSpell(new BlockHitResult(
                    new Vec3(pos1.getX(), pos1.getY(), pos1.getZ()), direction, pos1, false
            ), world, shooter, spellContext, resolver);
        } else if (world.getBlockState(pos1.relative(direction)).canBeReplaced(ModFluids.BLIGHT_FLUID.get())) {
            pos1 = pos1.relative(direction);
            world.setBlockAndUpdate(pos1, ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState());
            ShapersFocus.tryPropagateBlockSpell(new BlockHitResult(
                    new Vec3(pos1.getX(), pos1.getY(), pos1.getZ()), direction, pos1, false
            ), world, shooter, spellContext, resolver);
        }
    }

    @Override
    public int getDefaultManaCost() {
        return 100;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentPierce.INSTANCE, AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        addBlockAoeAugmentDescriptions(map);
        map.put(AugmentSensitive.INSTANCE, "Places blight at a target entity's feet.");
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        super.addDefaultAugmentLimits(defaults);
        defaults.put(AugmentSensitive.INSTANCE.getRegistryName(), 1);
    }

    @Override
    public String getBookDescription() {
        return "Places blight liquid at a location.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
    }
}
