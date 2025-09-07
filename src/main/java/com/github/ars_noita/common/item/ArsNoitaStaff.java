package com.github.ars_noita.common.item;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.client.gui.ArsNoitaStaffGUI;
import com.github.ars_noita.common.glyph.TemporalContextForm;
import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public class ArsNoitaStaff extends Item implements ICasterTool, IRadialProvider, GeoItem {
    
    public enum StaffPhase {
        BEGIN,
        TICK,
        END
    }
    
    private StaffPhase currentPhase = StaffPhase.BEGIN;
    private boolean isHeld = false;
    private int tickCount = 0;
    
    // Track which players are currently using the staff to prevent multiple instances
    private static final Map<UUID, Boolean> playerHoldingStaff = new HashMap<>();
    
    // Track which players have run out of mana and should stop using the staff
    private static final Map<UUID, Boolean> playerOutOfMana = new HashMap<>();
    
    // Context storage for temporal context form
    private static final Map<UUID, TemporalContext> playerContexts = new HashMap<>();
    
    public static class TemporalContext {
        public BlockHitResult beginBlockHitResult;
        public EntityHitResult beginEntityHitResult;
        public BlockHitResult tickBlockHitResult;
        public EntityHitResult tickEntityHitResult;
        
        public TemporalContext() {
            this.beginBlockHitResult = null;
            this.beginEntityHitResult = null;
            this.tickBlockHitResult = null;
            this.tickEntityHitResult = null;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                openStaffGUI(player);
            }
            return InteractionResultHolder.success(stack);
        } else {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }
    
    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity livingEntity) {
        return 72000;
    }
    
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity livingEntity) {
        return stack;
    }
    
    @Override
    public void onUseTick(Level level, net.minecraft.world.entity.LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player && !level.isClientSide) {
            // On first tick, execute begin phase
            if (remainingUseDuration == getUseDuration(stack, livingEntity) - 1) {
                ArsNoita.LOGGER.debug("First tick - executing begin phase for player {}", player.getName().getString());
                beginPhase(player, stack);
            }
            // Execute tick phase every tick (every 0.05 seconds)
            else {
                tickPhase(player, stack);
            }
        }
    }
    
    @Override
    public void releaseUsing(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity livingEntity, int timeCharged) {
        if (livingEntity instanceof Player player) {
            boolean playerIsHolding = playerHoldingStaff.getOrDefault(player.getUUID(), false);
            boolean playerOutOfManaFlag = playerOutOfMana.getOrDefault(player.getUUID(), false);
            
            ArsNoita.LOGGER.info("ArsNoitaStaff releaseUsing() called by {} after {} ticks (clientSide: {}, isHeld: {}, playerIsHolding: {}, outOfMana: {})", 
                player.getName().getString(), timeCharged, level.isClientSide, isHeld, playerIsHolding, playerOutOfManaFlag);
            
            // Only execute END phase on server side and if we're actually holding the item
            // Use playerHoldingStaff as the single source of truth
            if (!level.isClientSide && playerIsHolding) {
                ArsNoita.LOGGER.info("Player {} released the staff, executing END phase", player.getName().getString());
                endPhase(player, stack);
            } else if (level.isClientSide) {
                ArsNoita.LOGGER.debug("Client-side releaseUsing called, not executing END phase");
            } else if (!playerIsHolding) {
                ArsNoita.LOGGER.debug("Player not holding staff (playerIsHolding = false), not executing END phase");
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void openStaffGUI(Player player) {
        ArsNoita.LOGGER.debug("Opening Ars Noita Staff GUI for player {}", player.getName().getString());
        Minecraft.getInstance().setScreen(new ArsNoitaStaffGUI());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onOpenBookMenuKeyPressed(ItemStack stack, Player player) {
        ArsNoita.LOGGER.info("C key pressed - opening Ars Noita Staff GUI for player {}", player.getName().getString());
        ArsNoita.LOGGER.debug("Staff stack: {}, Player: {}", stack, player);
        openStaffGUI(player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onRadialKeyPressed(ItemStack stack, Player player) {
        ArsNoita.LOGGER.info("V key pressed - opening radial menu for player {}", player.getName().getString());
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
        currentPhase = StaffPhase.BEGIN;
        isHeld = true;
        tickCount = 0;
        
        playerHoldingStaff.put(player.getUUID(), true);
        playerOutOfMana.remove(player.getUUID());
        
        TemporalContext context = getTemporalContext(player);
        context.beginBlockHitResult = null;
        context.beginEntityHitResult = null;
        context.tickBlockHitResult = null;
        context.tickEntityHitResult = null;
        
        executeSpell(player, stack, StaffPhase.BEGIN);
    }

    public void tickPhase(Player player, ItemStack stack) {
        if (isHeld) {
            // Check if player has run out of mana
            boolean outOfMana = playerOutOfMana.getOrDefault(player.getUUID(), false);
            ArsNoita.LOGGER.info("TICK phase - Player: {}, outOfMana: {}, isHeld: {}", player.getName().getString(), outOfMana, isHeld);
            
            if (outOfMana) {
                ArsNoita.LOGGER.info("Player {} ran out of mana, stopping staff use", player.getName().getString());
                // Just stop processing ticks, don't force release
                return;
            }
            
            ArsNoita.LOGGER.info("Executing TICK phase for player {} (tick #{})", player.getName().getString(), tickCount + 1);
            currentPhase = StaffPhase.TICK;
            tickCount++;
            
            // Execute tick spell (will skip if empty)
            executeSpell(player, stack, StaffPhase.TICK);
        }
    }

    public void endPhase(Player player, ItemStack stack) {
        // Check both instance flag and player-specific flag
        boolean playerIsHolding = playerHoldingStaff.getOrDefault(player.getUUID(), false);
        
        if (isHeld && playerIsHolding) {
            ArsNoita.LOGGER.info("=== ENDING STAFF USE ===");
            ArsNoita.LOGGER.info("Player: {}, Total ticks: {}", player.getName().getString(), tickCount);
            currentPhase = StaffPhase.END;
            isHeld = false;
            
            // Clear player-specific flags
            playerHoldingStaff.remove(player.getUUID());
            playerOutOfMana.remove(player.getUUID());
            
            ArsNoita.LOGGER.debug("Set isHeld = false for player {} (UUID: {})", player.getName().getString(), player.getUUID());
            
            // Execute end spell (will skip if empty)
            executeSpell(player, stack, StaffPhase.END);
            
            // Clear temporal context after all phases are complete
            clearTemporalContext(player);
            
            ArsNoita.LOGGER.info("END phase completed");
        } else {
            ArsNoita.LOGGER.debug("endPhase called but isHeld = {} and playerIsHolding = {} for player {}, skipping", 
                isHeld, playerIsHolding, player.getName().getString());
        }
    }

    private void executeSpell(Player player, ItemStack stack, StaffPhase phase) {
        ArsNoita.LOGGER.info("=== EXECUTING {} SPELL ===", phase.name());
        ArsNoita.LOGGER.info("Player: {}, Tick: {}, Level: {}", player.getName().getString(), tickCount, player.level().isClientSide ? "CLIENT" : "SERVER");
        
        // Get the caster from the staff
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            ArsNoita.LOGGER.error("No caster found on staff for player {}", player.getName().getString());
            return;
        }
        ArsNoita.LOGGER.info("Caster found: {}", caster.getClass().getSimpleName());
        
        // Get the current logical slot (0-9, representing the 10 hotkeys)
        int currentLogicalSlot = caster.getCurrentSlot();
        ArsNoita.LOGGER.info("Current logical slot: {}", currentLogicalSlot);
        
        // Debug: Check if the slot is valid and has spells
        if (currentLogicalSlot < 0 || currentLogicalSlot >= 10) {
            ArsNoita.LOGGER.warn("Invalid logical slot {}, defaulting to 0", currentLogicalSlot);
            currentLogicalSlot = 0;
        }
        
        // Check if this logical slot has any spells
        boolean hasSpells = false;
        for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
            int testSlot = currentLogicalSlot * 3 + phaseIndex;
            if (!caster.getSpell(testSlot).isEmpty()) {
                hasSpells = true;
                break;
            }
        }
        
        if (!hasSpells) {
            ArsNoita.LOGGER.warn("No spells found in logical slot {}, searching for first available slot...", currentLogicalSlot);
            // Find the first logical slot that has spells
            for (int testLogicalSlot = 0; testLogicalSlot < 10; testLogicalSlot++) {
                for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
                    int testSlot = testLogicalSlot * 3 + phaseIndex;
                    if (!caster.getSpell(testSlot).isEmpty()) {
                        ArsNoita.LOGGER.info("Found spells in logical slot {}, switching to it", testLogicalSlot);
                        currentLogicalSlot = testLogicalSlot;
                        hasSpells = true;
                        break;
                    }
                }
                if (hasSpells) break;
            }
        }
        
        // Calculate the physical slot for this phase
        // Each logical slot uses 3 physical slots: logicalSlot*3 + phase
        // Phase ordinals: BEGIN=0, TICK=1, END=2
        int physicalSlot = currentLogicalSlot * 3 + phase.ordinal();
        ArsNoita.LOGGER.info("Physical slot for {} phase: {} (logical: {} * 3 + phase.ordinal: {})", phase, physicalSlot, currentLogicalSlot, phase.ordinal());
        
        // Get the spell for this phase
        Spell spell = caster.getSpell(physicalSlot);
        if (spell.isEmpty()) {
            ArsNoita.LOGGER.debug("No spell found for {} phase in physical slot {}, skipping", phase, physicalSlot);
            return;
        }
        
        ArsNoita.LOGGER.info("Found spell in physical slot {}: {} (recipe: {})", physicalSlot, caster.getSpellName(physicalSlot), spell.recipe());
        
        // Store original slot and switch to the physical slot for casting
        int originalSlot = caster.getCurrentSlot();
        
        // Switch to the physical slot for casting
        caster.setCurrentSlot(physicalSlot);
        caster.saveToStack(stack);
        
        // Double-check that the spell is still valid after switching slots
        Spell currentSpell = caster.getSpell(physicalSlot);
        if (currentSpell.isEmpty()) {
            ArsNoita.LOGGER.warn("Spell became empty after switching to physical slot {}, skipping cast", physicalSlot);
            // Restore the original logical slot
            caster.setCurrentSlot(originalSlot);
            caster.saveToStack(stack);
            return;
        }
        
        ArsNoita.LOGGER.info("Attempting to cast spell from physical slot {}...", physicalSlot);
        
        // Check if this is a temporal context form spell and handle context replacement
        if (isTemporalContextFormSpell(currentSpell)) {
            ArsNoita.LOGGER.info("Detected TemporalContextForm spell, handling context replacement");
            ArsNoita.LOGGER.info("TemporalContextForm spell recipe: {}", currentSpell.recipe());
            handleTemporalContextSpell(player, currentSpell, phase);
        }
        
        // Check mana before casting (this will handle creative mode automatically)
        if (!checkManaAndCast(player, stack, currentSpell, phase)) {
            // If mana check failed and player is not creative, mark them as out of mana
            if (!player.isCreative() && !playerOutOfMana.containsKey(player.getUUID())) {
                ArsNoita.LOGGER.info("Player {} ran out of mana, marking for staff release", player.getName().getString());
                playerOutOfMana.put(player.getUUID(), true);
            }
            // Restore the original logical slot
            caster.setCurrentSlot(originalSlot);
            caster.saveToStack(stack);
            return;
        }
        
        // Restore the original logical slot
        caster.setCurrentSlot(originalSlot);
        caster.saveToStack(stack);
    }
    
    private boolean checkManaAndCast(Player player, ItemStack stack, Spell spell, StaffPhase phase) {
        // Create a spell context to check mana requirements
        SpellContext context = new SpellContext(player.level(), spell, player, LivingCaster.from(player), stack);
        SpellResolver resolver = new SpellResolver(context);
        
        // Check if player has enough mana or is in creative mode
        boolean canCast = resolver.canCast(player);
        ArsNoita.LOGGER.info("Mana check - Player: {}, Creative: {}, canCast: {}", player.getName().getString(), player.isCreative(), canCast);
        
        if (canCast) {
            // Cast the spell using Ars Nouveau's system with proper mana handling
            try {
                // Use the resolver to cast the spell, which handles mana consumption correctly
                boolean castSuccess = resolver.onCast(stack, player.level());
                if (castSuccess) {
                    ArsNoita.LOGGER.info("Spell cast completed for {} phase", phase);
                } else {
                    ArsNoita.LOGGER.warn("Spell cast failed for {} phase", phase);
                    return false;
                }
            } catch (Exception e) {
                ArsNoita.LOGGER.warn("Failed to cast spell: {}", e.getMessage());
                return false;
            }
        } else {
            ArsNoita.LOGGER.debug("Not enough mana to cast spell for {} phase (cost: {})", phase, resolver.getResolveCost());
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
    
    // Temporal context management
    public static TemporalContext getTemporalContext(Player player) {
        return playerContexts.computeIfAbsent(player.getUUID(), k -> new TemporalContext());
    }
    
    public static void setTemporalContext(Player player, BlockPos blockPos, Entity entity, StaffPhase phase) {
        TemporalContext context = getTemporalContext(player);
        if (blockPos != null) {
            context.lastBlockPos = blockPos;
        }
        if (entity != null) {
            context.lastEntity = entity;
            context.lastEntityId = entity.getUUID();
        }
        context.lastPhase = phase;
        ArsNoita.LOGGER.debug("Updated temporal context for player {}: blockPos={}, entity={}, phase={}", 
            player.getName().getString(), blockPos, entity != null ? entity.getName().getString() : "null", phase);
    }
    
    public static void clearTemporalContext(Player player) {
        playerContexts.remove(player.getUUID());
        ArsNoita.LOGGER.debug("Cleared temporal context for player {}", player.getName().getString());
    }
    
    /**
     * Check if a spell starts with TemporalContextForm
     */
    private boolean isTemporalContextFormSpell(Spell spell) {
        if (spell.isEmpty()) return false;
        
        // Check if the first glyph in the spell is TemporalContextForm
        return spell.recipe().iterator().next() instanceof TemporalContextForm;
    }
    
    /**
     * Handle temporal context form spells by replacing the hit result with stored context
     */
    private void handleTemporalContextSpell(Player player, Spell spell, StaffPhase phase) {
        TemporalContext temporalContext = getTemporalContext(player);
        
        ArsNoita.LOGGER.info("Handling temporal context spell for phase {} with context: blockPos={}, entity={}", 
            phase, temporalContext.lastBlockPos, temporalContext.lastEntity != null ? temporalContext.lastEntity.getName().getString() : "null");
        
        // For now, just log what we would do
        // In a full implementation, we would need to:
        // 1. Create a custom SpellResolver that uses the stored context
        // 2. Replace the normal hit result with the temporal context
        // 3. Execute the rest of the spell with the replaced context
        
        if (temporalContext.lastBlockPos != null) {
            ArsNoita.LOGGER.info("Would target block at {} for temporal context spell", temporalContext.lastBlockPos);
        }
        
        if (temporalContext.lastEntity != null) {
            ArsNoita.LOGGER.info("Would target entity {} for temporal context spell", temporalContext.lastEntity.getName().getString());
        }
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
