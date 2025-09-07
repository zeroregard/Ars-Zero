package com.github.ars_noita.common.item;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.client.gui.ArsNoitaStaffGUI;
import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
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

public class ArsNoitaStaff extends Item implements ICasterTool, IRadialProvider, GeoItem {
    
    public enum StaffPhase {
        BEGIN,
        TICK,
        END
    }
    
    private StaffPhase currentPhase = StaffPhase.BEGIN;
    private boolean isHeld = false;
    private int tickCount = 0;
    
    // GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArsNoitaStaff() {
        super(new Item.Properties().stacksTo(1).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(10)));
        ArsNoita.LOGGER.debug("Creating ArsNoitaStaff item instance with SpellCaster data component (10 slots)");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ArsNoita.LOGGER.debug("ArsNoitaStaff use() called by {} in hand {}", player.getName().getString(), hand.name());
        
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                ArsNoita.LOGGER.debug("Opening staff GUI for player {}", player.getName().getString());
                openStaffGUI(player);
                return InteractionResultHolder.success(stack);
            } else {
                ArsNoita.LOGGER.debug("Starting begin phase for player {}", player.getName().getString());
                beginPhase(player, stack);
                return InteractionResultHolder.success(stack);
            }
        }
        
        ArsNoita.LOGGER.debug("Server-side use, passing through");
        return InteractionResultHolder.pass(stack);
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
        return new RadialMenu<>((int slot) -> {
            AbstractCaster<?> caster = SpellCasterRegistry.from(itemStack);
            if (caster != null) {
                caster.setCurrentSlot(slot);
                caster.saveToStack(itemStack);
            }
            Networking.sendToServer(new PacketSetCasterSlot(slot));
        },
                getRadialMenuSlotsForSpellpart(itemStack),
                SecondaryIconPosition.NORTH,
                RenderUtils::drawSpellPart,
                0);
    }

    public List<RadialMenuSlot<AbstractSpellPart>> getRadialMenuSlotsForSpellpart(ItemStack itemStack) {
        AbstractCaster<?> spellCaster = SpellCasterRegistry.from(itemStack);
        List<RadialMenuSlot<AbstractSpellPart>> radialMenuSlots = new ArrayList<>();
        
        // Ensure we have 10 slots for the staff
        int maxSlots = Math.max(10, spellCaster.getMaxSlots());
        
        for (int i = 0; i < maxSlots; i++) {
            Spell spell = spellCaster.getSpell(i);
            if (!spell.isEmpty()) {
                radialMenuSlots.add(new RadialMenuSlot<AbstractSpellPart>(spellCaster.getSpellName(i), spell.recipe().iterator().next()));
            } else {
                radialMenuSlots.add(new RadialMenuSlot<AbstractSpellPart>("Empty", null));
            }
        }
        return radialMenuSlots;
    }

    // ICasterTool implementation - getSpellCaster is handled by SpellCasterRegistry registration

    @Override
    public boolean canQuickCast() {
        // Return true to allow quick casting with the staff
        return true;
    }

    private void beginPhase(Player player, ItemStack stack) {
        ArsNoita.LOGGER.debug("Starting BEGIN phase for player {}", player.getName().getString());
        currentPhase = StaffPhase.BEGIN;
        isHeld = true;
        tickCount = 0;
        
        // Execute begin spell
        executeSpell(player, stack, StaffPhase.BEGIN);
    }

    public void tickPhase(Player player, ItemStack stack) {
        if (isHeld) {
            ArsNoita.LOGGER.debug("Executing TICK phase for player {} (tick #{})", player.getName().getString(), tickCount + 1);
            currentPhase = StaffPhase.TICK;
            tickCount++;
            
            // Execute tick spell
            executeSpell(player, stack, StaffPhase.TICK);
        }
    }

    public void endPhase(Player player, ItemStack stack) {
        if (isHeld) {
            ArsNoita.LOGGER.debug("Ending staff use for player {} after {} ticks", player.getName().getString(), tickCount);
            currentPhase = StaffPhase.END;
            isHeld = false;
            
            // Execute end spell
            executeSpell(player, stack, StaffPhase.END);
        }
    }

    private void executeSpell(Player player, ItemStack stack, StaffPhase phase) {
        // TODO: Implement spell execution based on phase
        // This will integrate with Ars Nouveau's spell system
        ArsNoita.LOGGER.info("Executing {} spell for staff (Player: {}, Tick: {})", phase.name(), player.getName().getString(), tickCount);
        ArsNoita.LOGGER.debug("Spell execution details - Phase: {}, IsHeld: {}, TickCount: {}", phase, isHeld, tickCount);
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
