package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;

/**
 * A skeleton variant that spawns wearing an Ars Nouveau arcanist hat and holding a spellbook.
 * No bow, no attack goals for now. Does not burn in sunlight.
 */
public class MageSkeletonEntity extends Skeleton {

    /** Ars Nouveau registry id for the arcanist hood (helmet). See LibItemNames.ARCANIST_HOOD. */
    private static final ResourceLocation ARCANIST_HOOD_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "arcanist_hood");
    /** Ars Nouveau novice spell book (held in main hand instead of bow). */
    private static final ResourceLocation NOVICE_SPELL_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "novice_spell_book");

    public MageSkeletonEntity(net.minecraft.world.entity.EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random,
                                                net.minecraft.world.DifficultyInstance difficulty) {
        // No bow; give spellbook in main hand and arcanist hood on head
        setArcanistHat();
        setSpellbookInHand();
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    private void setArcanistHat() {
        Item hood = BuiltInRegistries.ITEM.get(ARCANIST_HOOD_ID);
        if (hood != null && hood != Items.AIR) {
            ItemStack stack = new ItemStack(hood);
            stack.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            setItemSlot(EquipmentSlot.HEAD, stack);
            setDropChance(EquipmentSlot.HEAD, 0.0f);
        } else {
            ArsZero.LOGGER.debug("Ars Nouveau arcanist hood not found at {}, mage skeleton will have no hat", ARCANIST_HOOD_ID);
        }
    }

    private void setSpellbookInHand() {
        Item spellbook = BuiltInRegistries.ITEM.get(NOVICE_SPELL_BOOK_ID);
        if (spellbook != null && spellbook != Items.AIR) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(spellbook));
            setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        } else {
            ArsZero.LOGGER.debug("Ars Nouveau novice spell book not found at {}, mage skeleton will have empty hand", NOVICE_SPELL_BOOK_ID);
        }
    }
}
