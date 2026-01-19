package com.github.ars_zero.common.entity.break_blocks;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.entity.IManaDrainable;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.datagen.BlockTagProvider;
import com.hollingsworth.arsnouveau.common.util.HolderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.hollingsworth.arsnouveau.common.items.curios.ShapersFocus;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.UUID;

public class GeometryBreakEntity extends AbstractGeometryProcessEntity implements IManaDrainable {

  private static final double BASE_MANA_COST_PER_BLOCK = 0.5;
  private static final float BLOCK_BREAKING_SPEED_MULTIPLIER = 2.0f;

  private float earthPower = 0.0f;
  private double accumulatedDrain = 0.0;
  private int ticksSinceLastDrainSync = 0;
  @Nullable
  private Player casterPlayer = null;
  private int harvestLevel = 3;
  private int fortuneCount = 0;
  private int extractCount = 0;
  private int randomizeCount = 0;
  private boolean sensitive = false;

  public GeometryBreakEntity(EntityType<?> entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    controllers.add(new AnimationController<>(this, "main_controller", 1, state -> {
      if (!isBuilding()) {
        return PlayState.STOP;
      }
      state.getController().setAnimation(RawAnimation.begin().thenPlay("harvest2"));
      if (isPaused()) {
        state.getController().setAnimationSpeed(0.0);
      } else {
        state.getController().setAnimationSpeed(1.0);
      }
      return PlayState.CONTINUE;
    }));
  }

  @Override
  public void setCaster(@Nullable LivingEntity caster) {
    super.setCaster(caster);
    if (caster instanceof Player player) {
      this.casterPlayer = player;
      this.earthPower = getEarthPower(caster);
    } else {
      this.casterPlayer = null;
    }
  }

  public void setSpellStats(int harvestLevel, int fortune, int extract, int randomize, boolean sensitive) {
    this.harvestLevel = harvestLevel;
    this.fortuneCount = fortune;
    this.extractCount = extract;
    this.randomizeCount = randomize;
    this.sensitive = sensitive;
  }

  private float getEarthPower(LivingEntity caster) {
    if (caster instanceof Player player) {
      AttributeInstance instance = player.getAttribute(ModRegistry.EARTH_POWER);
      if (instance != null) {
        return (float) instance.getValue();
      }
    }
    return 0.0f;
  }

  @Override
  protected float getBlocksPerTick() {
    return BASE_BLOCKS_PER_TICK * BLOCK_BREAKING_SPEED_MULTIPLIER * (1.0f + earthPower / 2.0f);
  }

  @Override
  protected boolean shouldReverseProcessOrder() {
    return super.shouldReverseProcessOrder();
  }

  @Override
  protected void tickProcess() {
    super.tickProcess();
    if (this.level() instanceof ServerLevel serverLevel) {
      tickAndSyncDrain(serverLevel);
    }
  }

  @Override
  public double getManaCostPerBlock() {
    double manaCost = BASE_MANA_COST_PER_BLOCK;
    manaCost *= 1.0 + (fortuneCount * 0.1);
    manaCost *= 1.0 + (extractCount * 0.3);
    return manaCost;
  }

  @Override
  public double getAccumulatedDrain() {
    return accumulatedDrain;
  }

  @Override
  public void setAccumulatedDrain(double value) {
    this.accumulatedDrain = value;
  }

  @Override
  public int getTicksSinceLastDrainSync() {
    return ticksSinceLastDrainSync;
  }

  @Override
  public void setTicksSinceLastDrainSync(int value) {
    this.ticksSinceLastDrainSync = value;
  }

  @Override
  public Player getCasterPlayer() {
    return casterPlayer;
  }

  @Override
  public void setCasterPlayer(Player player) {
    this.casterPlayer = player;
  }

  @Override
  protected ProcessResult processBlock(ServerLevel level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    if (state.isAir() || state.liquid()) {
      return ProcessResult.SKIPPED;
    }

    if (state.is(BlockTagProvider.BREAK_BLACKLIST)) {
      return ProcessResult.SKIPPED;
    }

    if (randomizeCount > 0 && level.random.nextFloat() < randomizeCount * 0.25F) {
      return ProcessResult.SKIPPED;
    }

    Player player = getClaimActor(level);
    if (player == null) {
      return ProcessResult.SKIPPED;
    }

    if (!canBlockBeHarvested(level, pos, state, this.harvestLevel)) {
      return ProcessResult.SKIPPED;
    }

    if (!BlockUtil.destroyRespectsClaim(player, level, pos)) {
      return ProcessResult.SKIPPED;
    }

    if (!(this.level() instanceof ServerLevel serverLevel)) {
      return ProcessResult.SKIPPED;
    }

    ItemStack toolStack = buildToolStack(level, pos, state);
    BlockState stateBefore = level.getBlockState(pos);
    BlockUtil.breakExtraBlock(level, pos, toolStack, getCasterUUID(), true);
    BlockState stateAfter = level.getBlockState(pos);

    if (stateBefore == stateAfter || !stateAfter.isAir()) {
      return ProcessResult.SKIPPED;
    }

    if (!consumeManaAndAccumulate(serverLevel, getManaCostPerBlock())) {
      return ProcessResult.WAITING_FOR_MANA;
    }

    if (spellContext != null && spellResolver != null) {
      ShapersFocus.tryPropagateBlockSpell(
          new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false),
          level, player, spellContext, spellResolver);
    }

    return ProcessResult.PROCESSED;
  }

  private boolean canBlockBeHarvested(ServerLevel level, BlockPos pos, BlockState state, int harvestLevel) {
    return state.getDestroySpeed(level, pos) >= 0 && SpellUtil.isCorrectHarvestLevel(harvestLevel, state);
  }

  private ItemStack buildToolStack(ServerLevel level, BlockPos pos, BlockState state) {
    if (sensitive) {
      return new ItemStack(Items.SHEARS);
    }

    boolean usePick = state.is(BlockTagProvider.BREAK_WITH_PICKAXE);
    ItemStack stack = usePick ? new ItemStack(Items.DIAMOND_PICKAXE) : new ItemStack(Items.DIAMOND_PICKAXE);

    if (fortuneCount > 0
        && stack.getEnchantmentLevel(HolderHelper.unwrap(level, Enchantments.FORTUNE)) < fortuneCount) {
      stack.enchant(HolderHelper.unwrap(level, Enchantments.FORTUNE), fortuneCount);
    }
    if (extractCount > 0
        && stack.getEnchantmentLevel(HolderHelper.unwrap(level, Enchantments.SILK_TOUCH)) < extractCount) {
      stack.enchant(HolderHelper.unwrap(level, Enchantments.SILK_TOUCH), extractCount);
    }

    return stack;
  }

  private Player getClaimActor(ServerLevel level) {
    UUID caster = getCasterUUID();
    if (caster == null || level.getServer() == null)
      return null;
    return level.getServer().getPlayerList().getPlayer(caster);
  }

  @Override
  protected SoundEvent getProcessSound(SoundType soundType) {
    return soundType.getBreakSound();
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    if (compound.contains("earth_power")) {
      this.earthPower = compound.getFloat("earth_power");
    }
    if (compound.contains("harvest_level")) {
      this.harvestLevel = compound.getInt("harvest_level");
    }
    if (compound.contains("fortune_count")) {
      this.fortuneCount = compound.getInt("fortune_count");
    }
    if (compound.contains("extract_count")) {
      this.extractCount = compound.getInt("extract_count");
    }
    if (compound.contains("randomize_count")) {
      this.randomizeCount = compound.getInt("randomize_count");
    }
    if (compound.contains("sensitive")) {
      this.sensitive = compound.getBoolean("sensitive");
    }
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    compound.putFloat("earth_power", this.earthPower);
    compound.putInt("harvest_level", this.harvestLevel);
    compound.putInt("fortune_count", this.fortuneCount);
    compound.putInt("extract_count", this.extractCount);
    compound.putInt("randomize_count", this.randomizeCount);
    compound.putBoolean("sensitive", this.sensitive);
  }
}
