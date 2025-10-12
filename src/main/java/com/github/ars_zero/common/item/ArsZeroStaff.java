package com.github.ars_zero.common.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.network.PacketStaffSpellFired;
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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
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
    
    private static final Map<UUID, StaffPhase> playerPhase = new HashMap<>();
    private static final Map<UUID, Boolean> playerHoldingStaff = new HashMap<>();
    private static final Map<UUID, Integer> playerTickCount = new HashMap<>();
    private static final Map<UUID, Boolean> playerOutOfMana = new HashMap<>();
    private static final Map<UUID, StaffCastContext> playerContexts = new HashMap<>();
    private static final Map<UUID, Integer> playerUseCount = new HashMap<>();
    private static final Map<UUID, Integer> playerBeginCount = new HashMap<>();
    private static final Map<UUID, Integer> playerEndCount = new HashMap<>();
    private static final Map<UUID, Integer> playerReleaseCount = new HashMap<>();
    private static final Map<UUID, Integer> playerSequenceTick = new HashMap<>(); // Tracks which tick in the sequence we're on
    
    public static class ArsZeroSpellContext extends SpellContext {
        public final StaffPhase phase;
        
        public ArsZeroSpellContext(Level world, Spell spell, LivingEntity caster, StaffPhase phase) {
            super(world, spell, caster, LivingCaster.from(caster), caster.getMainHandItem());
            this.phase = phase;
        }
    }
    
    // GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArsZeroStaff() {
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
            UUID playerId = player.getUUID();
            int totalDuration = getUseDuration(stack, livingEntity);
            boolean isFirstTick = remainingUseDuration == totalDuration - 1; // 71999 for duration 72000
            boolean alreadyHolding = playerHoldingStaff.getOrDefault(playerId, false);
            
            if (isFirstTick && !alreadyHolding) {
                // First tick: BEGIN phase
                beginPhase(player, stack);
                playerSequenceTick.put(playerId, 0); // Tick 0: BEGIN
            } else if (!isFirstTick && alreadyHolding) {
                // Subsequent ticks: TICK phase
                tickPhase(player, stack);
                int sequenceTick = playerSequenceTick.getOrDefault(playerId, 0) + 1;
                playerSequenceTick.put(playerId, sequenceTick);
            }
        }
    }
    

    @OnlyIn(Dist.CLIENT)
    private void openStaffGUI(Player player) {
        Minecraft.getInstance().setScreen(new ArsZeroStaffGUI());
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
        UUID playerId = player.getUUID();
        
        if (playerHoldingStaff.getOrDefault(playerId, false)) {
            return;
        }
        
        int beginCount = playerBeginCount.getOrDefault(playerId, 0) + 1;
        playerBeginCount.put(playerId, beginCount);
        
        playerPhase.put(playerId, StaffPhase.BEGIN);
        playerHoldingStaff.put(playerId, true);
        playerTickCount.put(playerId, 0);
        playerOutOfMana.remove(playerId);
        
        UUID castId = UUID.randomUUID();
        StaffCastContext context = new StaffCastContext(castId, playerId, CastPhase.BEGIN);
        StaffContextRegistry.register(context);
        playerContexts.put(playerId, context);
        
        executeSpell(player, stack, StaffPhase.BEGIN);
        
        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, StaffPhase.BEGIN);
        }
    }

    public void tickPhase(Player player, ItemStack stack) {
        UUID playerId = player.getUUID();
        
        if (!playerHoldingStaff.getOrDefault(playerId, false)) {
            return;
        }
        
        playerPhase.put(playerId, StaffPhase.TICK);
        int ticks = playerTickCount.getOrDefault(playerId, 0);
        playerTickCount.put(playerId, ticks + 1);
        
        executeSpell(player, stack, StaffPhase.TICK);
        
        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, StaffPhase.TICK);
        }
    }
    

    public void endPhase(Player player, ItemStack stack) {
        UUID playerId = player.getUUID();
        boolean playerIsHolding = playerHoldingStaff.getOrDefault(playerId, false);
        
        int endCount = playerEndCount.getOrDefault(playerId, 0) + 1;
        playerEndCount.put(playerId, endCount);
        
        if (!playerIsHolding) {
            return;
        }
        
        playerPhase.put(playerId, StaffPhase.END);
        
        executeSpell(player, stack, StaffPhase.END);
        
        if (player instanceof ServerPlayer serverPlayer) {
            sendSpellFiredPacket(serverPlayer, StaffPhase.END);
        }
        
        playerHoldingStaff.remove(playerId);
        playerTickCount.remove(playerId);
        playerOutOfMana.remove(playerId);
        playerSequenceTick.remove(playerId); // Clean up sequence tracking
        
        StaffCastContext context = playerContexts.get(playerId);
        if (context != null) {
            StaffContextRegistry.remove(context.castId);
            playerContexts.remove(playerId);
        }
        
        playerPhase.remove(playerId);
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
        
        checkManaAndCast(player, stack, currentSpell, phase);
        
        caster.setCurrentSlot(originalSlot);
        caster.saveToStack(stack);
    }
    
    private boolean checkManaAndCast(Player player, ItemStack stack, Spell spell, StaffPhase phase) {
        ArsZeroSpellContext context = new ArsZeroSpellContext(player.level(), spell, player, phase);
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
                ArsZero.LOGGER.error("Exception during cast: ", e);
                return false;
            }
        }
        
        return canCast;
    }

    public StaffPhase getCurrentPhase(UUID playerId) {
        return playerPhase.getOrDefault(playerId, StaffPhase.BEGIN);
    }

    public boolean isHeld(UUID playerId) {
        return playerHoldingStaff.getOrDefault(playerId, false);
    }

    public int getTickCount(UUID playerId) {
        return playerTickCount.getOrDefault(playerId, 0);
    }
    
    public static boolean isPlayerHoldingStaff(UUID playerId) {
        return playerHoldingStaff.getOrDefault(playerId, false);
    }
    
    public static Boolean isPlayerOutOfMana(UUID playerId) {
        return playerOutOfMana.get(playerId);
    }
    
    public static int getUseCount(UUID playerId) {
        return playerUseCount.getOrDefault(playerId, 0);
    }
    
    public static int getBeginCount(UUID playerId) {
        return playerBeginCount.getOrDefault(playerId, 0);
    }
    
    public static int getEndCount(UUID playerId) {
        return playerEndCount.getOrDefault(playerId, 0);
    }
    
    public static int getReleaseCount(UUID playerId) {
        return playerReleaseCount.getOrDefault(playerId, 0);
    }
    
    public static int getSequenceTick(UUID playerId) {
        return playerSequenceTick.getOrDefault(playerId, 0);
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
        
        if (isHolding) {
            return InteractionResultHolder.pass(stack);
        }
        
        // Only increment useCount AFTER confirming we can start using
        int useCount = playerUseCount.getOrDefault(playerId, 0) + 1;
        playerUseCount.put(playerId, useCount);
        
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        boolean wasHolding = playerHoldingStaff.getOrDefault(playerId, false);
        
        int releaseCount = playerReleaseCount.getOrDefault(playerId, 0) + 1;
        playerReleaseCount.put(playerId, releaseCount);
        
        if (wasHolding) {
            // Check if this was a quick click (no TICK phase happened)
            int sequenceTick = playerSequenceTick.getOrDefault(playerId, 0);
            
            if (sequenceTick == 0) {
                // Quick click: Tick 0: BEGIN → Tick 1: TICK → Tick 2: END
                tickPhase(player, stack); // Tick 1: TICK
                endPhase(player, stack);  // Tick 2: END
            } else {
                // Normal release: END spell
                endPhase(player, stack);
            }
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
        return Component.translatable("item.ars_zero.ars_zero_staff.desc");
    }
    
    private void sendSpellFiredPacket(ServerPlayer player, StaffPhase phase) {
        PacketStaffSpellFired packet = new PacketStaffSpellFired(phase.ordinal());
        PacketDistributor.sendToPlayer(player, packet);
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
            private final BlockEntityWithoutLevelRenderer renderer = new com.github.ars_zero.client.renderer.item.CreativeSpellStaffRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }


}
