package com.github.ars_zero.common.entity.break_blocks;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.datagen.BlockTagProvider;
import com.hollingsworth.arsnouveau.common.util.HolderHelper;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public class BreakConvergenceEntity extends AbstractGeometryProcessEntity {

  private static final double BASE_MANA_COST_PER_BLOCK = 0.4;
  private static final float BLOCK_BREAKING_SPEED_MULTIPLIER = 2.0f;

  private float earthPower = 0.0f;
  private int harvestLevel = 3;
  private int fortuneCount = 0;
  private int extractCount = 0;
  private int randomizeCount = 0;
  private boolean sensitive = false;

  public BreakConvergenceEntity(EntityType<?> entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public void setCaster(@Nullable LivingEntity caster) {
    super.setCaster(caster);
    if (caster != null) {
      this.earthPower = getEarthPower(caster);
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
    return true;
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

    if (!consumeManaForBlock(player)) {
      return ProcessResult.WAITING_FOR_MANA;
    }

    ItemStack toolStack = buildToolStack(level, pos, state);
    BlockUtil.breakExtraBlock(level, pos, toolStack, getCasterUUID(), true);
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

    if (fortuneCount > 0 && stack.getEnchantmentLevel(HolderHelper.unwrap(level, Enchantments.FORTUNE)) < fortuneCount) {
      stack.enchant(HolderHelper.unwrap(level, Enchantments.FORTUNE), fortuneCount);
    }
    if (extractCount > 0 && stack.getEnchantmentLevel(HolderHelper.unwrap(level, Enchantments.SILK_TOUCH)) < extractCount) {
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

  private boolean consumeManaForBlock(Player player) {
    IManaCap manaCap = CapabilityRegistry.getMana(player);
    if (manaCap != null && manaCap.getCurrentMana() >= BASE_MANA_COST_PER_BLOCK) {
      manaCap.removeMana(BASE_MANA_COST_PER_BLOCK);
      return true;
    }
    return false;
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
