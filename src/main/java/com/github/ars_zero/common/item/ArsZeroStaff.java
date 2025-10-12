package com.github.ars_zero.common.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.spell.CastPhase;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.common.spell.StaffContextRegistry;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import net.minecraft.world.entity.LivingEntity;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenuSlot;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.SecondaryIconPosition;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketSetCasterSlot;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArsZeroStaff extends Item implements ICasterTool, IRadialProvider, GeoItem {
    
    public enum StaffPhase {
        BEGIN,
        TICK,
        END
    }
    
    private StaffPhase currentPhase = StaffPhase.BEGIN;
    private boolean isHeld = false;
    private int tickCount = 0;
    
    // Hybrid grace period system
    private static final int GRACE_PERIOD_BEGIN = 5;  // Minimum hold time to "commit" to a spell
    private static final int GRACE_PERIOD_END = 2;   // Grace period for ending a committed spell
    private static final int COOLDOWN_TICKS = 10;     // Cooldown after committed spell ends
    
    private Map<UUID, Integer> gracePeriodTicks = new HashMap<>();
    private Map<UUID, Boolean> committedSpells = new HashMap<>();  // Track if player has a committed spell
    private Map<UUID, Integer> cooldownTicks = new HashMap<>();   // Track cooldown after committed spell ends
    
    // Track which players are currently using the staff to prevent multiple instances
    private static final Map<UUID, Boolean> playerHoldingStaff = new HashMap<>();
    
    // Track which players have run out of mana and should stop using the staff
    private static final Map<UUID, Boolean> playerOutOfMana = new HashMap<>();
    
    // Context storage for temporal context form - now using new system
    private static final Map<UUID, StaffCastContext> playerContexts = new HashMap<>();
    
    public static class ArsNoitaSpellContext extends SpellContext {
        public final StaffPhase phase;
        
        public ArsNoitaSpellContext(Level world, Spell spell, LivingEntity caster, StaffPhase phase) {
            super(world, spell, caster, LivingCaster.from(caster), caster.getMainHandItem());
            this.phase = phase;
        }
    }
    
    // GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArsNoitaStaff() {
        super(new Item.Properties().stacksTo(1).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(30)));
    }
    
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster != null) {
            caster.setCurrentSlot(0);
            caster.saveToStack(stack);
        }
    }

    
    @Override
    public void onUseTick(Level level, net.minecraft.world.entity.LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player && !level.isClientSide) {
            if (remainingUseDuration == getUseDuration(stack, livingEntity) - 1) {
                beginPhase(player, stack);
            } else {
                // Only call tickPhase if we're actually holding the staff and it's the right item
                boolean isHoldingStaff = playerHoldingStaff.getOrDefault(player.getUUID(), false);
                boolean isUsingStaff = player.getUseItem() == stack;
                
                if (isHoldingStaff && isUsingStaff) {
                    tickPhase(player, stack);
                }
            }
        }
    }
    

    @OnlyIn(Dist.CLIENT)
    private void openStaffGUI(Player player) {
        Minecraft.getInstance().setScreen(new ArsNoitaStaffGUI());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onOpenBookMenuKeyPressed(ItemStack stack, Player player) {
        openStaffGUI(player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onRadialKeyPressed(ItemStack stack, Player player) {
        if (Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new GuiRadialMenu<>(getRadialMenuProviderForSpellpart(stack)));
        }
    }

    public RadialMenu<AbstractSpellPart> getRadialMenuProviderForSpellpart(ItemStack itemStack) {
        return new RadialMenu<>((int logicalSlot) -> {
            AbstractCaster<?> caster = SpellCasterRegistry.from(itemStack);
            if (caster != null) {
                // Set the logical slot (0-9) which represents the hotkey
                caster.setCurrentSlot(logicalSlot);
                caster.saveToStack(itemStack);
            }
            // Send the logical slot to server
            Networking.sendToServer(new PacketSetCasterSlot(logicalSlot));
        },
                getRadialMenuSlotsForSpellpart(itemStack),
                SecondaryIconPosition.NORTH,
                RenderUtils::drawSpellPart,
                0);
    }

    public List<RadialMenuSlot<AbstractSpellPart>> getRadialMenuSlotsForSpellpart(ItemStack itemStack) {
        AbstractCaster<?> spellCaster = SpellCasterRegistry.from(itemStack);
        List<RadialMenuSlot<AbstractSpellPart>> radialMenuSlots = new ArrayList<>();
        
        // Show 10 logical slots, each displaying the TICK spell (middle of 3 physical slots)
        for (int logicalSlot = 0; logicalSlot < 10; logicalSlot++) {
            // Each logical slot uses 3 physical slots: logicalSlot*3 + phase
            // TICK phase is phase 1, so physical slot = logicalSlot*3 + 1
            int tickPhysicalSlot = logicalSlot * 3 + StaffPhase.TICK.ordinal();
            Spell spell = spellCaster.getSpell(tickPhysicalSlot);
            
            if (!spell.isEmpty()) {
                radialMenuSlots.add(new RadialMenuSlot<AbstractSpellPart>(spellCaster.getSpellName(tickPhysicalSlot), spell.recipe().iterator().next()));
            } else {
                radialMenuSlots.add(new RadialMenuSlot<AbstractSpellPart>("Empty", null));
            }
        }
        return radialMenuSlots;
    }

    // ICasterTool implementation - getSpellCaster is handled by SpellCasterRegistry registration


    private void beginPhase(Player player, ItemStack stack) {
        ArsNoita.LOGGER.info("BEGIN SPELL FIRED for player {}", player.getName().getString());
        
        currentPhase = StaffPhase.BEGIN;
        isHeld = true;
        tickCount = 0;
        
        playerHoldingStaff.put(player.getUUID(), true);
        playerOutOfMana.remove(player.getUUID());
        
        // Create new context for this cast
        UUID castId = UUID.randomUUID();
        StaffCastContext context = new StaffCastContext(castId, player.getUUID(), CastPhase.BEGIN);
        StaffContextRegistry.register(context);
        playerContexts.put(player.getUUID(), context);
        
        executeSpell(player, stack, StaffPhase.BEGIN);
    }

    public void tickPhase(Player player, ItemStack stack) {
        if (isHeld) {
            boolean outOfMana = playerOutOfMana.getOrDefault(player.getUUID(), false);
            
            if (outOfMana && !player.isCreative()) {
                return;
            }
            
            currentPhase = StaffPhase.TICK;
            tickCount++;
            
            // Check if we should commit to the spell (held for GRACE_PERIOD_BEGIN ticks)
            UUID playerId = player.getUUID();
            if (tickCount >= GRACE_PERIOD_BEGIN && !committedSpells.getOrDefault(playerId, false)) {
                committedSpells.put(playerId, true);
            }
            
            StaffCastContext context = playerContexts.get(player.getUUID());
            
            if (context != null && context.beginFinished) {
                // Only execute tick if begin phase is finished
                executeSpell(player, stack, StaffPhase.TICK);
            }
        }
        
        // Handle grace period and cooldown countdown
        handleGracePeriod(player);
    }
    
    private void handleGracePeriod(Player player) {
        UUID playerId = player.getUUID();
        
        // Handle grace period countdown
        Integer graceTicks = gracePeriodTicks.get(playerId);
        if (graceTicks != null) {
            graceTicks--;
            
            if (graceTicks <= 0) {
                // Grace period expired, actually stop the staff
                gracePeriodTicks.remove(playerId);
                ArsNoita.LOGGER.info("HOLDING DOWN: NO (grace period expired) for player {}", player.getName().getString());
                endPhase(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            } else {
                // Update grace period counter
                gracePeriodTicks.put(playerId, graceTicks);
            }
        }
        
        // Handle cooldown countdown
        Integer cooldownTicks = this.cooldownTicks.get(playerId);
        if (cooldownTicks != null) {
            cooldownTicks--;
            
            if (cooldownTicks <= 0) {
                // Cooldown expired, allow new spells
                this.cooldownTicks.remove(playerId);
            } else {
                // Update cooldown counter
                this.cooldownTicks.put(playerId, cooldownTicks);
            }
        }
    }

    public void endPhase(Player player, ItemStack stack) {
        boolean playerIsHolding = playerHoldingStaff.getOrDefault(player.getUUID(), false);
        
        if (isHeld && playerIsHolding) {
            currentPhase = StaffPhase.END;
            isHeld = false;
            
            playerHoldingStaff.remove(player.getUUID());
            playerOutOfMana.remove(player.getUUID());
            
            StaffCastContext context = playerContexts.get(player.getUUID());
                
            if (context != null && context.beginFinished) {
                // Only execute end if begin phase is finished
                executeSpell(player, stack, StaffPhase.END);
            }
            
            // Cleanup context
            if (context != null) {
                StaffContextRegistry.remove(context.castId);
                playerContexts.remove(player.getUUID());
            }
            
            // Start cooldown if this was a committed spell
            boolean wasCommitted = committedSpells.getOrDefault(player.getUUID(), false);
            if (wasCommitted) {
                cooldownTicks.put(player.getUUID(), COOLDOWN_TICKS);
            }
            
            // Clear committed spell state
            committedSpells.remove(player.getUUID());
        }
    }

    private void executeSpell(Player player, ItemStack stack, StaffPhase phase) {
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            return;
        }
        
        int currentLogicalSlot = caster.getCurrentSlot();
        if (currentLogicalSlot < 0 || currentLogicalSlot >= 10) {
            currentLogicalSlot = 0;
        }
        
        boolean hasSpells = false;
        for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
            int testSlot = currentLogicalSlot * 3 + phaseIndex;
            if (!caster.getSpell(testSlot).isEmpty()) {
                hasSpells = true;
                break;
            }
        }
        
        if (!hasSpells) {
            for (int testLogicalSlot = 0; testLogicalSlot < 10; testLogicalSlot++) {
                for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
                    int testSlot = testLogicalSlot * 3 + phaseIndex;
                    if (!caster.getSpell(testSlot).isEmpty()) {
                        currentLogicalSlot = testLogicalSlot;
                        hasSpells = true;
                        break;
                    }
                }
                if (hasSpells) break;
            }
        }
        
        int physicalSlot = currentLogicalSlot * 3 + phase.ordinal();
        Spell spell = caster.getSpell(physicalSlot);
        
        if (spell.isEmpty()) {
            return;
        }
        
        int originalSlot = caster.getCurrentSlot();
        caster.setCurrentSlot(physicalSlot);
        caster.saveToStack(stack);
        
        Spell currentSpell = caster.getSpell(physicalSlot);
        if (currentSpell.isEmpty()) {
            caster.setCurrentSlot(originalSlot);
            caster.saveToStack(stack);
            return;
        }
        
        if (isTemporalContextFormSpell(currentSpell)) {
            // Temporal context is now handled via events
        }
        
        if (!checkManaAndCast(player, stack, currentSpell, phase)) {
            if (!player.isCreative() && !playerOutOfMana.containsKey(player.getUUID())) {
                playerOutOfMana.put(player.getUUID(), true);
            }
            caster.setCurrentSlot(originalSlot);
            caster.saveToStack(stack);
            return;
        }
        
        caster.setCurrentSlot(originalSlot);
        caster.saveToStack(stack);
    }
    
    private boolean checkManaAndCast(Player player, ItemStack stack, Spell spell, StaffPhase phase) {
        ArsNoitaSpellContext context = new ArsNoitaSpellContext(player.level(), spell, player, phase);
        SpellResolver resolver = new SpellResolver(context);
        
        // Wrap the resolver for Begin phase to capture results
        if (phase == StaffPhase.BEGIN) {
            StaffCastContext staffContext = playerContexts.get(player.getUUID());
            if (staffContext != null) {
                resolver = new WrappedSpellResolver(resolver, staffContext.castId, CastPhase.BEGIN, true);
            }
        }
        
        boolean canCast = resolver.canCast(player);
        
        if (canCast) {
            try {
                boolean castSuccess = resolver.onCast(stack, player.level());
                
                if (!castSuccess) {
                    return false;
                }
            } catch (Exception e) {
                ArsNoita.LOGGER.error("Exception during cast: ", e);
                return false;
            }
        }
        
        return canCast;
    }

    public StaffPhase getCurrentPhase() {
        return currentPhase;
    }

    public boolean isHeld() {
        return isHeld;
    }

    public int getTickCount() {
        return tickCount;
    }
    
    // New temporal context management using StaffCastContext
    public static StaffCastContext getStaffContext(Player player) {
        return playerContexts.get(player.getUUID());
    }
    
    public static void clearStaffContext(Player player) {
        StaffCastContext context = playerContexts.get(player.getUUID());
        if (context != null) {
            StaffContextRegistry.remove(context.castId);
            playerContexts.remove(player.getUUID());
        }
    }
    
    
    /**
     * Check if a spell starts with TemporalContextForm
     */
    private boolean isTemporalContextFormSpell(Spell spell) {
        if (spell.isEmpty()) return false;
        
        // Check if the first glyph in the spell is TemporalContextForm
        return spell.recipe().iterator().next() instanceof TemporalContextForm;
    }
    

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                openStaffGUI(player);
            }
            return InteractionResultHolder.success(stack);
        }
        
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        
        UUID playerId = player.getUUID();
        boolean isHolding = playerHoldingStaff.getOrDefault(playerId, false);
        boolean inGracePeriod = gracePeriodTicks.containsKey(playerId);
        boolean hasCommittedSpell = committedSpells.getOrDefault(playerId, false);
        boolean inCooldown = cooldownTicks.containsKey(playerId);
        
        // Check if player is already using the staff
        if (isHolding) {
            return InteractionResultHolder.pass(stack);
        }
        
        // If player has a committed spell, block new spell attempts
        if (hasCommittedSpell) {
            return InteractionResultHolder.pass(stack);
        }
        
        // If player is in cooldown after committed spell, block new spell attempts
        if (inCooldown) {
            return InteractionResultHolder.pass(stack);
        }
        
        // Check if player is in grace period and wants to resume
        if (inGracePeriod) {
            // Cancel grace period and resume staff usage
            gracePeriodTicks.remove(playerId);
            // Don't start using item again, just cancel grace period
            return InteractionResultHolder.pass(stack);
        }
        
        // Start using the staff
        ArsNoita.LOGGER.info("HOLDING DOWN: YES for player {}", player.getName().getString());
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        boolean hasCommittedSpell = committedSpells.getOrDefault(playerId, false);
        
        if (hasCommittedSpell) {
            // Committed spell - start grace period for ending
            gracePeriodTicks.put(playerId, GRACE_PERIOD_END);
            // Keep isHeld true during grace period so we can continue ticking
        } else {
            // Quick tap - immediate end (fire-and-forget)
            ArsNoita.LOGGER.info("HOLDING DOWN: NO for player {}", player.getName().getString());
            endPhase(player, stack);
        }
    }
    
    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity livingEntity) {
        return 72000; // 1 hour - effectively infinite for our purposes
    }
    
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Use bow animation for staff
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity livingEntity) {
        return stack;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("item.ars_noita.ars_noita_staff.desc");
    }

    // GeckoLib implementation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "idle_controller", 20, this::idlePredicate));
    }

    private <P extends Item & GeoAnimatable> PlayState idlePredicate(AnimationState<P> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new com.github.ars_noita.client.renderer.item.CreativeSpellStaffRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }
}
