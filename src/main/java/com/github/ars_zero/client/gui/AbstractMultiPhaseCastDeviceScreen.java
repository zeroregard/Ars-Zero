package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.animation.StaffAnimationHandler;
import com.github.ars_zero.client.gui.buttons.ManaIndicator;
import com.github.ars_zero.client.gui.buttons.StaffArrowButton;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen;
import com.hollingsworth.arsnouveau.client.gui.buttons.ClearButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.CreateSpellButton;
import com.github.ars_zero.client.gui.buttons.StaffCraftingButton;
import com.github.ars_zero.client.gui.buttons.StaffSpellSlot;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import com.hollingsworth.arsnouveau.client.gui.SearchBar;
import com.hollingsworth.arsnouveau.client.gui.book.EnterTextField;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketSetMultiPhaseSpellCastingSlot;
import com.github.ars_zero.common.network.PacketUpdateTickDelay;
import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.api.spell.ISpellValidator;
import com.hollingsworth.arsnouveau.api.spell.SpellValidationError;
import com.hollingsworth.arsnouveau.common.capability.IPlayerCap;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.common.spell.validation.CombinedSpellValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.GlyphKnownValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.GlyphMaxTierValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.ActionAugmentationPolicyValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.StartingCastMethodSpellValidator;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.registry.FamiliarRegistry;
import com.hollingsworth.arsnouveau.client.gui.GuiUtils;
import com.hollingsworth.arsnouveau.client.gui.book.GuiFamiliarScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public abstract class AbstractMultiPhaseCastDeviceScreen extends SpellSlottedScreen {

    public enum DevicePhase {
        BEGIN, TICK, END
    }

    private DevicePhase currentPhase = DevicePhase.BEGIN;
    
    private GuiImageButton beginPhaseButton;
    private GuiImageButton tickPhaseButton;
    private GuiImageButton endPhaseButton;
    
     // Spell slots for the 10 shortcuts on the right (radial menu slots)
    private StaffSpellSlot[] spellSlots = new StaffSpellSlot[10];
    private StaffSpellSlot selectedSlotButton;
    private int selectedSpellSlot = -1;
    
    // Glyph selection
    private List<GlyphButton> glyphButtons = new ArrayList<>();
    private List<CraftingButton> craftingCells = new ArrayList<>();
    private List<AbstractSpellPart> unlockedSpells;
    private List<AbstractSpellPart> displayedGlyphs;
    private int page = 0;
    private int glyphsPerPage = 70;
    
    // Category tracking for glyph organization
    public int formTextRow = 0;
    public int augmentTextRow = 0;
    public int effectTextRow = 0;
    
    private StaffArrowButton nextButton;
    private StaffArrowButton previousButton;
    
    // UI elements
    private SearchBar searchBar;
    private EnterTextField spellNameBox;
    private String previousString = "";
    private List<Button> categoryButtons = new ArrayList<>();
    
    // Phase-specific spell storage (3 phases, each with 10 crafting cells)
    private List<List<AbstractSpellPart>> phaseSpells = new ArrayList<>();
    private Button delayMenuButton;
    private TickDelaySlider delaySlider;
    private boolean delayMenuOpen;
    private final int[] slotDelays = new int[10];
    
    private ISpellValidator spellValidator;
    private List<SpellValidationError> validationErrors = new LinkedList<>();
    protected ItemStack deviceStack;
    private InteractionHand guiHand;
    
    protected abstract ResourceLocation getBackgroundTexture();

    @Override
    public void onBookstackUpdated(ItemStack stack) {
        super.onBookstackUpdated(stack);
        this.deviceStack = stack;
        loadSlotDelayValues();
        updateDelaySliderValue();
    }

    public AbstractMultiPhaseCastDeviceScreen(ItemStack stack, InteractionHand hand) {
        super(hand);
        this.deviceStack = stack;
        this.guiHand = hand;
        if (this.caster == null && stack != null && !stack.isEmpty()) {
            this.caster = SpellCasterRegistry.from(stack);
            if (this.caster != null) {
                this.selectedSpellSlot = this.caster.getCurrentSlot();
            }
        }
        
        // Initialize unlocked spells - we'll do this in init() when playerCap is available
        this.unlockedSpells = new ArrayList<>();
        this.displayedGlyphs = new ArrayList<>();
        
        // Initialize phase spells (3 phases, each with 10 crafting cells)
        for (int i = 0; i < 3; i++) {
            phaseSpells.add(new ArrayList<>());
            for (int j = 0; j < 10; j++) {
                phaseSpells.get(i).add(null);
            }
        }
        
        // Set default phase to BEGIN
        this.currentPhase = DevicePhase.BEGIN;
        Arrays.fill(slotDelays, 1);
    }

    private static final int STAFF_GUI_WIDTH = 375;
    private static final int STAFF_GUI_HEIGHT = 232;
    private static final int PHASE_ROW_TEXTURE_WIDTH = 253;
    private static final int PHASE_ROW_TEXTURE_HEIGHT = 20;
    private static final int PHASE_ROW_TEXTURE_X_OFFSET = 33;
    private static final int PHASE_SECTION_Y_OFFSET = 116;
    private static final int PHASE_ROW_HEIGHT = 20;
    private static final int CRAFTING_CELL_START_X_OFFSET = 36;
    private static final int CRAFTING_CELL_SPACING = 24;
    private static final int PHASE_SECTION_SHIFT_X = 65;
    private static final int PHASE_SECTION_SHIFT_Y = 19;
    
    @Override
    public void init() {
        super.init();
        bookLeft = width / 2 - STAFF_GUI_WIDTH / 2;
        bookTop = height / 2 - STAFF_GUI_HEIGHT / 2;
        bookRight = width / 2 + STAFF_GUI_WIDTH / 2;
        bookBottom = height / 2 + STAFF_GUI_HEIGHT / 2;
        
        if (selectedSpellSlot == -1) {
            selectedSpellSlot = caster.getCurrentSlot();
            if (selectedSpellSlot < 0 || selectedSpellSlot >= 10) {
                selectedSpellSlot = 0;
            }
        }
        
        IPlayerCap playerCapData = CapabilityRegistry.getPlayerDataCap(player);
        List<AbstractSpellPart> parts = playerCapData == null ? new ArrayList<>() : new ArrayList<>(playerCapData.getKnownGlyphs().stream().filter(AbstractSpellPart::shouldShowInSpellBook).toList());
        parts.addAll(GlyphRegistry.getDefaultStartingSpells());
        
        int tier = 1;
        boolean isCreativeStaff = false;
        if (bookStack.getItem() instanceof AbstractSpellStaff staff) {
            tier = staff.getTier().value;
            if (staff.getTier() == SpellTier.CREATIVE) {
                isCreativeStaff = true;
                parts = new ArrayList<>(GlyphRegistry.getSpellpartMap().values().stream().filter(AbstractSpellPart::shouldShowInSpellBook).toList());
            }
        } else if (bookStack.getItem() instanceof AbstractMultiPhaseCastDevice device) {
            tier = device.getTier().value;
            if (device.getTier() == SpellTier.CREATIVE) {
                isCreativeStaff = true;
                parts = new ArrayList<>(GlyphRegistry.getSpellpartMap().values().stream().filter(AbstractSpellPart::shouldShowInSpellBook).toList());
            }
        }
        
        this.spellValidator = new CombinedSpellValidator(
                ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator(),
                new ActionAugmentationPolicyValidator(),
                new GlyphMaxTierValidator(tier),
                new GlyphKnownValidator(player.isCreative() || isCreativeStaff ? null : playerCapData),
                new StartingCastMethodSpellValidator()
        );
        
        this.unlockedSpells = parts;
        this.displayedGlyphs = new ArrayList<>(this.unlockedSpells);
        
        int finalTier = tier;
        this.displayedGlyphs = this.displayedGlyphs.stream()
            .filter(part -> part.getConfigTier().value <= finalTier)
            .toList();
        
        initSpellSlots();
        
        loadSlotDelayValues();
        
        // Add phase selection buttons (16x16 buttons for each row)
        addPhaseButtons();
        
        // Add search bar
        addSearchBar();
        
        // Add spell name field and buttons
        addSpellNameAndButtons();
        
        // Add left side tabs
        addLeftSideTabs();
        
        // Initialize glyph selection
        layoutAllGlyphs(page);
        
        // Initialize crafting cells for all phases
        resetCraftingCells();
        
        // Add pagination buttons
        addPaginationButtons();
        
        loadSpellFromSlot();
        
        updateCraftingCellVisibility();
        
        initDelaySelector();
        
        selectPhase(DevicePhase.BEGIN);
        
        validate();
    }

    private void initSpellSlots() {
        int slotStartX = bookLeft + 355;
        int slotStartY = bookTop + 37;
        int slotSpacing = 16;
        
        for (int i = 0; i < 10; i++) {
            int beginPhysicalSlot = i * 3 + DevicePhase.BEGIN.ordinal();
            String name = caster.getSpellName(beginPhysicalSlot);
            StaffSpellSlot slot = new StaffSpellSlot(slotStartX, slotStartY + slotSpacing * i, i, name, (b) -> {
                if (!(b instanceof StaffSpellSlot button) || this.selectedSpellSlot == button.slotNum) {
                    return;
                }
                this.selectedSlotButton.isSelected = false;
                this.selectedSlotButton = button;
                button.isSelected = true;
                this.selectedSpellSlot = this.selectedSlotButton.slotNum;
                onSpellSlotChanged();
            });

            if (i == selectedSpellSlot) {
                selectedSlotButton = slot;
                slot.isSelected = true;
            } else {
                slot.isSelected = false;
            }
            addRenderableWidget(slot);
            spellSlots[i] = slot;
        }
    }

    private void onSpellSlotChanged() {
        loadSpellFromSlot();
        resetCraftingCells();
        
        if (spellNameBox != null) {
            int beginPhysicalSlot = selectedSpellSlot * 3 + DevicePhase.BEGIN.ordinal();
            spellNameBox.setValue(caster.getSpellName(beginPhysicalSlot));
        }
        
        validate();
        updateDelaySliderValue();
    }

    private void loadSpellFromSlot() {
        // Load all 3 phases from the extended slot system
        // Each logical slot uses 3 physical slots: slot*3 + phase (0=BEGIN, 1=TICK, 2=END)
        
        // Clear all phases first
        for (int phase = 0; phase < 3; phase++) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.get(phase);
            phaseSpell.clear();
            for (int i = 0; i < 10; i++) {
                phaseSpell.add(null);
            }
        }
        
        // Load each phase from its physical slot
        for (int phase = 0; phase < 3; phase++) {
            int physicalSlot = selectedSpellSlot * 3 + phase;
            Spell spell = caster.getSpell(physicalSlot);
            
            List<AbstractSpellPart> phaseSpell = phaseSpells.get(phase);
            
            // Load the spell parts - convert recipe to list first
            List<AbstractSpellPart> recipeList = new ArrayList<>();
            for (AbstractSpellPart part : spell.recipe()) {
                recipeList.add(part);
            }
            
            for (int i = 0; i < recipeList.size() && i < 10; i++) {
                phaseSpell.set(i, recipeList.get(i));
            }
            
            ArsZero.LOGGER.debug("Loaded {} phase from physical slot {} with {} glyphs", 
                DevicePhase.values()[phase], physicalSlot, recipeList.size());
        }
        
        // Update crafting cells to show the loaded spells
        resetCraftingCells();
    }

    private void addPhaseButtons() {
        int startY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y + 2;
        int rowHeight = PHASE_ROW_HEIGHT + 2;
        int buttonSize = 16;
        int buttonX = bookLeft + PHASE_SECTION_SHIFT_X + 11;
        
        GuiImageButton beginButton = new GuiImageButton(buttonX, startY, buttonSize, buttonSize, 
            StaffGuiTextures.ICON_START, (button) -> selectPhase(DevicePhase.BEGIN));
        beginButton.withTooltip(Component.translatable("gui.ars_zero.phase.begin"));
        beginPhaseButton = beginButton;
        addRenderableWidget(beginButton);
        
        GuiImageButton tickButton = new GuiImageButton(buttonX, startY + rowHeight, buttonSize, buttonSize, 
            StaffGuiTextures.ICON_TICK, (button) -> selectPhase(DevicePhase.TICK));
        tickButton.withTooltip(Component.translatable("gui.ars_zero.phase.tick"));
        tickPhaseButton = tickButton;
        addRenderableWidget(tickButton);
        
        GuiImageButton endButton = new GuiImageButton(buttonX, startY + rowHeight * 2, buttonSize, buttonSize, 
            StaffGuiTextures.ICON_END, (button) -> selectPhase(DevicePhase.END));
        endButton.withTooltip(Component.translatable("gui.ars_zero.phase.end"));
        endPhaseButton = endButton;
        addRenderableWidget(endButton);
    }

    private void addSearchBar() {
        String previousSearch = "";
        if (searchBar != null) {
            previousSearch = searchBar.getValue();
        }

        searchBar = new SearchBar(Minecraft.getInstance().font, bookRight - 140, bookTop + 14);
        searchBar.onClear = (val) -> {
            this.onSearchChanged("");
            return null;
        };

        searchBar.setValue(previousSearch);
        searchBar.setSuggestion(Component.translatable("ars_nouveau.spell_book_gui.search").getString());
        searchBar.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBar);
    }

    private void addSpellNameAndButtons() {
        spellNameBox = new EnterTextField(minecraft.font, bookLeft + 76, bookBottom - 29);
        
        int beginPhysicalSlot = selectedSpellSlot * 3 + DevicePhase.BEGIN.ordinal();
        spellNameBox.setValue(caster.getSpellName(beginPhysicalSlot));
        addRenderableWidget(spellNameBox);

        addRenderableWidget(new CreateSpellButton(bookRight - 84, bookBottom - 29, (b) -> {
            ArsZero.LOGGER.info("Save button clicked!");
            this.saveSpell();
        }, this::getValidationErrors));
        addRenderableWidget(new ClearButton(bookRight - 137, bookBottom - 29, Component.translatable("ars_nouveau.spell_book_gui.clear"), (button) -> clear()));
    }

    private void addLeftSideTabs() {
        addRenderableWidget(new GuiImageButton(bookLeft + 54, bookTop + 32, 16, 16, StaffGuiTextures.STYLE_ICON, (b) -> {
            openParticleScreen();
        }).withTooltip(Component.translatable("ars_nouveau.gui.spell_style")));
        
        addRenderableWidget(new GuiImageButton(bookLeft + 54, bookTop + 53, 16, 16, StaffGuiTextures.ICON_DISCORD, (b) -> {
            try {
                Util.getPlatform().openUri(new URI("https://discord.com/channels/743298050222587978/1432008462929236071"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).withTooltip(Component.translatable("ars_nouveau.gui.discord")));
        
        addRenderableWidget(new GuiImageButton(bookLeft + 54, bookTop + 73, 16, 16, StaffGuiTextures.ICON_FAMILIARS, (b) -> {
            Collection<ResourceLocation> familiarHolders = new ArrayList<>();
            IPlayerCap cap = CapabilityRegistry.getPlayerDataCap(player);
            if (cap != null) {
                familiarHolders = cap.getUnlockedFamiliars().stream().map(s -> s.familiarHolder.getRegistryName()).collect(Collectors.toList());
            }
            Collection<ResourceLocation> finalFamiliarHolders = familiarHolders;
            Minecraft.getInstance().setScreen(new GuiFamiliarScreen(FamiliarRegistry.getFamiliarHolderMap().values().stream().filter(f -> finalFamiliarHolders.contains(f.getRegistryName())).collect(Collectors.toList()), this));
        }).withTooltip(Component.translatable("ars_nouveau.gui.familiar")));
        
        addRenderableWidget(new GuiImageButton(bookLeft + 54, bookTop + 93, 16, 16, StaffGuiTextures.ICON_DOCS, (b) -> {
            GuiUtils.openWiki(player);
        }).withTooltip(Component.translatable("ars_nouveau.gui.notebook")));
        
        addAffinityButton();
    }
    
    private void addAffinityButton() {
        boolean affinityLoaded = ModList.get().isLoaded("ars_affinity");
        
        GuiImageButton affinityButton = new GuiImageButton(bookLeft + 54, bookTop + 113, 16, 16, StaffGuiTextures.ICON_AFFINITY, (b) -> {
            if (!affinityLoaded) {
                try {
                    Util.getPlatform().openUri(new URI("https://www.curseforge.com/minecraft/mc-mods/ars-affinity"));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                try {
                    Class<?> affinityScreenClass = Class.forName("com.github.ars_affinity.client.screen.AffinityScreen");
                    Object affinityScreen = affinityScreenClass.getConstructor(
                        net.minecraft.world.entity.player.Player.class,
                        boolean.class,
                        net.minecraft.client.gui.screens.Screen.class
                    ).newInstance(minecraft.player, true, this);
                    minecraft.setScreen((net.minecraft.client.gui.screens.Screen) affinityScreen);
                } catch (Exception e) {
                    ArsZero.LOGGER.error("Failed to open Ars Affinity screen", e);
                }
            }
        });
        
        Component tooltip;
        if (affinityLoaded) {
            tooltip = Component.translatable("ars_affinity.gui.affinities");
        } else {
            tooltip = Component.translatable("ars_affinity.gui.affinities").append(
                Component.literal(" - install Ars Affinities.").withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withUnderlined(true))
            );
        }
        affinityButton.withTooltip(tooltip);
        addRenderableWidget(affinityButton);
    }
    
    private void openParticleScreen() {
        StaffParticleScreen.openScreen(this, selectedSpellSlot, deviceStack, this.guiHand);
    }
    
    private int getSelectedPhysicalSlot() {
        return selectedSpellSlot * 3 + currentPhase.ordinal();
    }

    private static class TickDelaySlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        private TickDelaySlider(int x, int y, int width, int height, int min, int max, int initial, IntConsumer onChange) {
            super(x, y, width, height, Component.literal(""), 0);
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            syncToDelay(initial);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Delay: " + getDelay()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getDelay());
        }

        public void syncToDelay(int delay) {
            int clamped = Mth.clamp(delay, min, max);
            int range = max - min;
            this.value = range <= 0 ? 0 : (double) (clamped - min) / (double) range;
            updateMessage();
        }

        private int getDelay() {
            int range = max - min;
            if (range <= 0) {
                return min;
            }
            return (int) Math.round(value * range) + min;
        }
    }

    private void selectPhase(DevicePhase phase) {
        currentPhase = phase;
        
        beginPhaseButton.active = (phase != DevicePhase.BEGIN);
        tickPhaseButton.active = (phase != DevicePhase.TICK);
        endPhaseButton.active = (phase != DevicePhase.END);
        
        beginPhaseButton.image = (phase == DevicePhase.BEGIN) ? StaffGuiTextures.ICON_START_SELECTED : StaffGuiTextures.ICON_START;
        tickPhaseButton.image = (phase == DevicePhase.TICK) ? StaffGuiTextures.ICON_TICK_SELECTED : StaffGuiTextures.ICON_TICK;
        endPhaseButton.image = (phase == DevicePhase.END) ? StaffGuiTextures.ICON_END_SELECTED : StaffGuiTextures.ICON_END;
        
        updateCraftingCellVisibility();
        updateDelayMenuVisibility();
        validate();
    }
    
    private void updateCraftingCellVisibility() {
        for (int i = 0; i < craftingCells.size(); i++) {
            CraftingButton cell = craftingCells.get(i);
            cell.visible = true;
        }
    }

    private void initDelaySelector() {
        int rowX = bookLeft + PHASE_ROW_TEXTURE_X_OFFSET + PHASE_SECTION_SHIFT_X - 4;
        int rowWidth = PHASE_ROW_TEXTURE_WIDTH;
        int rowHeight = PHASE_ROW_HEIGHT + 2;
        int tickRowY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y + DevicePhase.TICK.ordinal() * rowHeight;
        int buttonWidth = 60;
        int buttonX = rowX + rowWidth - buttonWidth - 6;
        int buttonY = tickRowY + 2;
        delayMenuButton = Button.builder(Component.literal(""), (b) -> toggleDelayMenu())
            .bounds(buttonX, buttonY, buttonWidth, 16)
            .build();
        addRenderableWidget(delayMenuButton);
        int sliderWidth = 140;
        int sliderX = rowX + rowWidth - sliderWidth - 6;
        int sliderY = buttonY + 18;
        delaySlider = new TickDelaySlider(sliderX, sliderY, sliderWidth, 20, 1, 20, getStoredDelayValueForSelectedSlot(), this::onDelayValueChanged);
        delaySlider.visible = false;
        delaySlider.active = false;
        addRenderableWidget(delaySlider);
        updateDelaySliderValue();
        updateDelayMenuVisibility();
    }

    private void toggleDelayMenu() {
        delayMenuOpen = !delayMenuOpen;
        updateDelayMenuVisibility();
    }

    private void updateDelayMenuVisibility() {
        boolean isTickPhase = currentPhase == DevicePhase.TICK;
        if (!isTickPhase) {
            delayMenuOpen = false;
        }
        if (delayMenuButton != null) {
            delayMenuButton.visible = isTickPhase;
            delayMenuButton.active = isTickPhase;
        }
        if (delaySlider != null) {
            boolean show = delayMenuOpen && isTickPhase;
            delaySlider.visible = show;
            delaySlider.active = show;
        }
    }

    private void onDelayValueChanged(int value) {
        if (selectedSpellSlot < 0 || selectedSpellSlot >= slotDelays.length) {
            return;
        }
        int clamped = Mth.clamp(value, 1, 20);
        slotDelays[selectedSpellSlot] = clamped;
        updateDelayButtonLabel(clamped);
        if (deviceStack != null && !deviceStack.isEmpty()) {
            AbstractMultiPhaseCastDevice.setSlotTickDelay(deviceStack, selectedSpellSlot, clamped);
        }
        boolean mainHand = guiHand == null || guiHand == InteractionHand.MAIN_HAND;
        Networking.sendToServer(new PacketUpdateTickDelay(selectedSpellSlot, clamped, mainHand, isCircletDevice()));
    }

    private int getStoredDelayValueForSelectedSlot() {
        if (selectedSpellSlot < 0 || selectedSpellSlot >= slotDelays.length) {
            return 1;
        }
        return slotDelays[selectedSpellSlot];
    }

    private void updateDelaySliderValue() {
        int value = getStoredDelayValueForSelectedSlot();
        if (delaySlider != null) {
            delaySlider.syncToDelay(value);
        }
        updateDelayButtonLabel(value);
    }

    private void updateDelayButtonLabel(int value) {
        if (delayMenuButton != null) {
            delayMenuButton.setMessage(Component.literal("Delay " + value));
        }
    }

    private void loadSlotDelayValues() {
        Arrays.fill(slotDelays, 1);
        if (deviceStack == null || deviceStack.isEmpty()) {
            return;
        }
        int[] stored = AbstractMultiPhaseCastDevice.getSlotTickDelays(deviceStack);
        System.arraycopy(stored, 0, slotDelays, 0, Math.min(slotDelays.length, stored.length));
    }

    private boolean isCircletDevice() {
        return bookStack != null && bookStack.getItem() instanceof SpellcastingCirclet;
    }

    private void addPaginationButtons() {
        this.nextButton = addRenderableWidget(new StaffArrowButton(bookRight - 33, bookBottom - 155, true, this::onPageIncrease));
        this.previousButton = addRenderableWidget(new StaffArrowButton(bookLeft + 75, bookBottom - 155, false, this::onPageDec));
        
        updateNextPageButtons();
        previousButton.active = false;
        previousButton.visible = false;
    }

    private void layoutAllGlyphs(int page) {
        for (Button b : categoryButtons) {
            removeWidget(b);
        }
        categoryButtons.clear();
        clearGlyphButtons(glyphButtons);
        formTextRow = 0;
        augmentTextRow = 0;
        effectTextRow = 0;
        
        if (displayedGlyphs.isEmpty()) {
            return;
        }

        final int PER_ROW = 14;
        final int MAX_ROWS = 5;
        int adjustedRowsPlaced = 0;
        boolean foundForms = false;
        boolean foundAugments = false;
        boolean foundEffects = false;

        List<AbstractSpellPart> sorted = new ArrayList<>(displayedGlyphs);
        sorted.sort(Comparator.comparingInt((AbstractSpellPart p) -> switch (p) {
            case AbstractAugment ignored -> 3;
            default -> p.getTypeIndex();
        }).thenComparing(AbstractSpellPart::getLocaleName));

        sorted = sorted.subList(glyphsPerPage * page, Math.min(sorted.size(), glyphsPerPage * (page + 1)));
        int adjustedXPlaced = 0;

        int yStart = bookTop + 38;
        int baseX = bookLeft + 87;
        final int horizontalSpacing = 18;

        for (AbstractSpellPart part : sorted) {
            if (adjustedXPlaced >= PER_ROW) {
                adjustedRowsPlaced++;
                adjustedXPlaced = 0;
            }

            if (adjustedRowsPlaced >= MAX_ROWS) {
                break;
            }

            if (!foundForms && part instanceof AbstractCastMethod) {
                if (adjustedRowsPlaced >= MAX_ROWS) {
                    break;
                }
                int xOffsetPlaceholder = horizontalSpacing * (adjustedXPlaced % PER_ROW);
                int yPlacePlaceholder = adjustedRowsPlaced * 18 + yStart;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder, StaffGuiTextures.GLYPH_CATEGORY_FORM, Component.translatable("ars_nouveau.spell_book_gui.form"));
                addRenderableWidget(btn);
                categoryButtons.add(btn);
                adjustedXPlaced++;
                if (adjustedXPlaced >= PER_ROW) {
                    adjustedRowsPlaced++;
                    adjustedXPlaced = 0;
                }
                foundForms = true;
            } else if (!foundAugments && part instanceof AbstractAugment) {
                if (adjustedRowsPlaced >= MAX_ROWS) {
                    break;
                }
                int xOffsetPlaceholder = horizontalSpacing * (adjustedXPlaced % PER_ROW);
                int yPlacePlaceholder = adjustedRowsPlaced * 18 + yStart;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder, StaffGuiTextures.GLYPH_CATEGORY_AUGMENT, Component.translatable("ars_nouveau.spell_book_gui.augment"));
                addRenderableWidget(btn);
                categoryButtons.add(btn);
                adjustedXPlaced++;
                if (adjustedXPlaced >= PER_ROW) {
                    adjustedRowsPlaced++;
                    adjustedXPlaced = 0;
                }
                foundAugments = true;
            } else if (!foundEffects && part instanceof AbstractEffect) {
                if (adjustedRowsPlaced >= MAX_ROWS) {
                    break;
                }
                int xOffsetPlaceholder = horizontalSpacing * (adjustedXPlaced % PER_ROW);
                int yPlacePlaceholder = adjustedRowsPlaced * 18 + yStart;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder, StaffGuiTextures.GLYPH_CATEGORY_EFFECT, Component.translatable("ars_nouveau.spell_book_gui.effect"));
                addRenderableWidget(btn);
                categoryButtons.add(btn);
                adjustedXPlaced++;
                if (adjustedXPlaced >= PER_ROW) {
                    adjustedRowsPlaced++;
                    adjustedXPlaced = 0;
                }
                foundEffects = true;
            }

            int xOffset = horizontalSpacing * (adjustedXPlaced % PER_ROW);
            int yPlace = adjustedRowsPlaced * 18 + yStart;

            GlyphButton cell = new GlyphButton(baseX + xOffset, yPlace, part, this::onGlyphClick);
            addRenderableWidget(cell);
            glyphButtons.add(cell);
            adjustedXPlaced++;
        }
    }

    public void onGlyphClick(Button button) {
        GlyphButton glyphButton = (GlyphButton) button;
        if (!glyphButton.validationErrors.isEmpty()) {
            return;
        }
        
        int currentPhaseIndex = currentPhase.ordinal();
        for (int i = 0; i < 10; i++) {
            int cellIndex = currentPhaseIndex * 10 + i;
            CraftingButton cell = craftingCells.get(cellIndex);
            
            if (cell.getAbstractSpellPart() == null) {
                cell.setAbstractSpellPart(glyphButton.abstractSpellPart);
                
                List<AbstractSpellPart> currentSpell = getCurrentPhaseSpell();
                if (i >= currentSpell.size()) {
                    while (currentSpell.size() <= i) {
                        currentSpell.add(null);
                    }
                }
                currentSpell.set(i, glyphButton.abstractSpellPart);
                
                
                validate();
                return;
            }
        }
    }

    private List<AbstractSpellPart> getCurrentPhaseSpell() {
        return phaseSpells.get(currentPhase.ordinal());
    }
    
    private List<SpellValidationError> getValidationErrors() {
        return validationErrors;
    }
    
    private void validate() {
        List<AbstractSpellPart> phaseList = getCurrentPhaseSpell();
        List<AbstractSpellPart> currentSpell = new ArrayList<>();
        for (int i = 0; i < phaseList.size(); i++) {
            AbstractSpellPart part = phaseList.get(i);
            if (part == null) {
                break;
            }
            currentSpell.add(part);
        }
        for (CraftingButton b : craftingCells) {
            b.validationErrors.clear();
        }
        List<SpellValidationError> errors = spellValidator.validate(currentSpell);
        for (SpellValidationError ve : errors) {
            int cellIndex = currentPhase.ordinal() * 10 + ve.getPosition();
            if (cellIndex >= 0 && cellIndex < craftingCells.size()) {
                CraftingButton b = craftingCells.get(cellIndex);
                b.validationErrors.add(ve);
            }
        }
        this.validationErrors = errors;
        AbstractSpellPart lastEffect = null;
        int lastGlyphNoGap = -1;
        for (int i = 0; i < phaseList.size(); i++) {
            AbstractSpellPart part = phaseList.get(i);
            if (part == null) {
                break;
            }
            if (!(part instanceof AbstractAugment)) {
                lastEffect = part;
            }
            lastGlyphNoGap = i;
        }
        List<AbstractSpellPart> slicedSpell = new ArrayList<>();
        for (int i = 0; i <= lastGlyphNoGap; i++) {
            if (i >= 0 && i < phaseList.size() && phaseList.get(i) != null) {
                slicedSpell.add(phaseList.get(i));
            }
        }
        for (GlyphButton glyphButton : glyphButtons) {
            glyphButton.validationErrors.clear();
            glyphButton.augmentingParent = lastEffect;
            AbstractSpellPart toAdd = GlyphRegistry.getSpellpartMap().get(glyphButton.abstractSpellPart.getRegistryName());
            slicedSpell.add(toAdd);
            glyphButton.validationErrors.addAll(
                spellValidator.validate(slicedSpell).stream()
                    .filter(ve -> ve.getPosition() >= slicedSpell.size() - 1).toList()
            );
            slicedSpell.remove(slicedSpell.size() - 1);
        }
    }

    public void resetCraftingCells() {
        for (CraftingButton button : craftingCells) {
            removeWidget(button);
        }
        craftingCells = new ArrayList<>();
        
        // Create 3 rows of 10 crafting cells each
        int startY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y;
        int rowHeight = PHASE_ROW_HEIGHT + 2;
        int cellSpacing = CRAFTING_CELL_SPACING;
        int startX = bookLeft + CRAFTING_CELL_START_X_OFFSET + PHASE_SECTION_SHIFT_X - 8;
        
        for (int phase = 0; phase < 3; phase++) {
            for (int slot = 0; slot < 10; slot++) {
                int x = startX + slot * cellSpacing;
                int y = startY + phase * rowHeight;
                
                StaffCraftingButton cell = new StaffCraftingButton(x, y, this::onCraftingSlotClick, slot);
                addRenderableWidget(cell);
                craftingCells.add(cell);
                
                // Set the spell part if it exists for this phase
                List<AbstractSpellPart> phaseSpell = phaseSpells.get(phase);
                AbstractSpellPart spellPart = slot < phaseSpell.size() ? phaseSpell.get(slot) : null;
                cell.setAbstractSpellPart(spellPart);
                
                // Show all cells by default
                cell.visible = true;
            }
        }
    }

    public void onCraftingSlotClick(Button button) {
        CraftingButton cell = (CraftingButton) button;
        if (cell.getAbstractSpellPart() != null) {
            int cellIndex = craftingCells.indexOf(cell);
            int phase = cellIndex / 10;
            int slot = cellIndex % 10;
            
            List<AbstractSpellPart> phaseSpell = phaseSpells.get(phase);
            if (slot < phaseSpell.size()) {
                phaseSpell.set(slot, null);
            }
            cell.setAbstractSpellPart(null);
            
            validate();
        }
    }

    public void saveSpell() {
        validate();
        if (!validationErrors.isEmpty()) {
            ArsZero.LOGGER.warn("Cannot save spell - validation errors present");
            return;
        }
        
        String spellName = spellNameBox.getValue();
        
        for (int phase = 0; phase < 3; phase++) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.get(phase);
            List<AbstractSpellPart> filteredPhase = phaseSpell.stream()
                .filter(part -> part != null)
                .toList();
            
            Spell spell = new Spell(filteredPhase);
            
            int physicalSlot = selectedSpellSlot * 3 + phase;
            
            com.hollingsworth.arsnouveau.common.network.Networking.sendToServer(new PacketUpdateCaster(spell, physicalSlot, spellName, true));
        }
        
        spellSlots[selectedSpellSlot].spellName = spellName;
        
        boolean isCirclet = bookStack.getItem() instanceof SpellcastingCirclet;
        Networking.sendToServer(new PacketSetMultiPhaseSpellCastingSlot(selectedSpellSlot, isCirclet));
    }

    public void clear() {
        for (int phase = 0; phase < 3; phase++) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.get(phase);
            phaseSpell.clear();
            for (int i = 0; i < 10; i++) {
                phaseSpell.add(null);
            }
            
            int physicalSlot = selectedSpellSlot * 3 + phase;
            Spell emptySpell = new Spell();
            com.hollingsworth.arsnouveau.common.network.Networking.sendToServer(new PacketUpdateCaster(emptySpell, physicalSlot, "", false));
        }
        
        resetCraftingCells();
        validate();
    }

    public void updateNextPageButtons() {
        if (displayedGlyphs.size() < glyphsPerPage) {
            nextButton.visible = false;
            nextButton.active = false;
        } else {
            nextButton.visible = true;
            nextButton.active = true;
        }
    }

    public void onPageIncrease(Button button) {
        if (page + 1 >= getNumPages())
            return;
        page++;
        if (displayedGlyphs.size() < glyphsPerPage * (page + 1)) {
            nextButton.visible = false;
            nextButton.active = false;
        }
        previousButton.active = true;
        previousButton.visible = true;
        layoutAllGlyphs(page);
        validate();
    }

    public void onPageDec(Button button) {
        if (page <= 0) {
            page = 0;
            return;
        }
        page--;
        if (page == 0) {
            previousButton.active = false;
            previousButton.visible = false;
        }

        if (displayedGlyphs.size() > glyphsPerPage * (page + 1)) {
            nextButton.visible = true;
            nextButton.active = true;
        }
        layoutAllGlyphs(page);
        validate();
    }

    public int getNumPages() {
        return (int) Math.ceil((double) displayedGlyphs.size() / glyphsPerPage);
    }

    public void onSearchChanged(String str) {
        if (str.equals(previousString))
            return;
        previousString = str;

        if (!str.isEmpty()) {
            searchBar.setSuggestion("");
            displayedGlyphs = new ArrayList<>();
            
            int tier = 1;
            if (bookStack.getItem() instanceof AbstractSpellStaff staff) {
                tier = staff.getTier().value;
            } else if (bookStack.getItem() instanceof AbstractMultiPhaseCastDevice device) {
                tier = device.getTier().value;
            }
            int finalTier = tier;

            for (AbstractSpellPart spellPart : unlockedSpells) {
                if (spellPart.getLocaleName().toLowerCase().contains(str.toLowerCase()) 
                    && spellPart.getConfigTier().value <= finalTier) {
                    displayedGlyphs.add(spellPart);
                }
            }
            for (net.minecraft.client.gui.components.Renderable w : renderables) {
                if (w instanceof GlyphButton glyphButton) {
                    if (glyphButton.abstractSpellPart.getRegistryName() != null) {
                        AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(glyphButton.abstractSpellPart.getRegistryName());
                        if (part != null) {
                            glyphButton.visible = part.getLocaleName().toLowerCase().contains(str.toLowerCase()) 
                                && part.getConfigTier().value <= finalTier;
                        }
                    }
                }
            }
        } else {
            searchBar.setSuggestion(Component.translatable("ars_nouveau.spell_book_gui.search").getString());
            int tier = 1;
            if (bookStack.getItem() instanceof AbstractSpellStaff staff) {
                tier = staff.getTier().value;
            } else if (bookStack.getItem() instanceof AbstractMultiPhaseCastDevice device) {
                tier = device.getTier().value;
            }
            int finalTier = tier;
            displayedGlyphs = new ArrayList<>(unlockedSpells.stream()
                .filter(part -> part.getConfigTier().value <= finalTier)
                .toList());
            for (net.minecraft.client.gui.components.Renderable w : renderables) {
                if (w instanceof GlyphButton) {
                    ((GlyphButton) w).visible = true;
                }
            }
        }
        updateNextPageButtons();
        this.page = 0;
        previousButton.active = false;
        previousButton.visible = false;
        layoutAllGlyphs(page);
        validate();
    }

    private void clearGlyphButtons(List<GlyphButton> buttons) {
        for (GlyphButton b : buttons) {
            renderables.remove(b);
            children().remove(b);
        }
        buttons.clear();
    }
    
    private GuiImageButton createCategoryIcon(int x, int y, ResourceLocation texture, Component tooltip) {
        GuiImageButton button = new GuiImageButton(x, y, 18, 16, texture, (b) -> {});
        button.soundDisabled = true;
        button.withTooltip(tooltip);
        return button;
    }


    @Override
    public void drawBackgroundElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        ResourceLocation backgroundTexture = getBackgroundTexture();
        if (backgroundTexture != null) {
            graphics.blit(backgroundTexture, 0, 0, 0, 0, STAFF_GUI_WIDTH, STAFF_GUI_HEIGHT, STAFF_GUI_WIDTH, STAFF_GUI_HEIGHT);
        }
        
        int rowX = PHASE_ROW_TEXTURE_X_OFFSET + PHASE_SECTION_SHIFT_X - 4;
        for (int i = 0; i < DevicePhase.values().length; i++) {
            DevicePhase phase = DevicePhase.values()[i];
            ResourceLocation rowTexture = phase == currentPhase ? StaffGuiTextures.SPELL_PHASE_ROW_SELECTED : StaffGuiTextures.SPELL_PHASE_ROW;
            int rowY = PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y + i * (PHASE_ROW_HEIGHT + 2);
            graphics.blit(rowTexture, rowX, rowY, 0, 0, PHASE_ROW_TEXTURE_WIDTH, PHASE_ROW_TEXTURE_HEIGHT, PHASE_ROW_TEXTURE_WIDTH, PHASE_ROW_TEXTURE_HEIGHT);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderManaIndicators(graphics, mouseX, mouseY);
    }
    
    private void renderManaIndicators(GuiGraphics graphics, int mouseX, int mouseY) {
        ArsZero.LOGGER.info("=== MANA INDICATOR RENDER START ===");
        ArsZero.LOGGER.info("bookLeft: {}, bookTop: {}", bookLeft, bookTop);
        ArsZero.LOGGER.info("PHASE_SECTION_SHIFT_X: {}, PHASE_ROW_TEXTURE_X_OFFSET: {}", PHASE_SECTION_SHIFT_X, PHASE_ROW_TEXTURE_X_OFFSET);
        ArsZero.LOGGER.info("PHASE_ROW_TEXTURE_WIDTH: {}", PHASE_ROW_TEXTURE_WIDTH);
        
        int phaseRowStartX = bookLeft + PHASE_ROW_TEXTURE_X_OFFSET + PHASE_SECTION_SHIFT_X - 4;
        int phaseRowEndX = phaseRowStartX + PHASE_ROW_TEXTURE_WIDTH;
        int indicatorX = phaseRowEndX + 4 - 8 - 4 + 1;
        int baseY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y;
        int rowHeight = PHASE_ROW_HEIGHT + 2;
        int indicatorHeight = 14;
        
        ArsZero.LOGGER.info("Phase row start X: {}, end X: {}", phaseRowStartX, phaseRowEndX);
        ArsZero.LOGGER.info("Indicator X: {}, baseY: {}", indicatorX, baseY);
        ArsZero.LOGGER.info("Player: {}", player != null ? player.getName().getString() : "NULL");
        
        ManaIndicator hoveredIndicator = null;
        
        for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.get(phaseIndex);
            int indicatorY = baseY + phaseIndex * rowHeight + (PHASE_ROW_HEIGHT - indicatorHeight) / 2 - 1 + 1;
            
            ArsZero.LOGGER.info("Phase {}: indicatorY={}, spell parts count={}", phaseIndex, indicatorY, phaseSpell.size());
            
            ManaIndicator indicator = new ManaIndicator(indicatorX, indicatorY, phaseSpell);
            indicator.render(graphics, player);
            
            ArsZero.LOGGER.info("Phase {}: After render call", phaseIndex);
            
            if (indicator.isHovered(mouseX, mouseY)) {
                hoveredIndicator = indicator;
                ArsZero.LOGGER.info("Phase {}: HOVERED! mouseX={}, mouseY={}", phaseIndex, mouseX, mouseY);
            }
        }
        
        if (hoveredIndicator != null) {
            hoveredIndicator.renderTooltip(graphics, mouseX, mouseY);
        }
        
        ArsZero.LOGGER.info("=== MANA INDICATOR RENDER END ===");
    }
    
    private void refreshCategoryButtons() {
        for (Button b : categoryButtons) {
            removeWidget(b);
        }
        categoryButtons.clear();
        
        int yOffset = 32;
        int formOffset = 0;
        if (formTextRow >= 1) {
            int x = (formTextRow > 6 ? 154 : 20);
            int y = 5 + 18 * (formTextRow + (formTextRow == 1 ? 0 : 1)) + yOffset;
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_FORM, Component.translatable("ars_nouveau.spell_book_gui.form"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
            formOffset = 1;
        }
        if (effectTextRow >= 1) {
            int x = (effectTextRow > 6 ? 154 : 20);
            int y = 5 + 18 * (effectTextRow % 7 + formOffset) + yOffset;
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_EFFECT, Component.translatable("ars_nouveau.spell_book_gui.effect"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
        }
        if (augmentTextRow >= 1) {
            int x = (augmentTextRow > 6 ? 154 : 20);
            int y = 5 + 18 * (augmentTextRow + formOffset) + yOffset;
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_AUGMENT, Component.translatable("ars_nouveau.spell_book_gui.augment"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
        }
    }

    public DevicePhase getCurrentPhase() {
        return currentPhase;
    }

    @Override
    public void onClose() {
        boolean isCirclet = bookStack.getItem() instanceof SpellcastingCirclet;
        Networking.sendToServer(new PacketSetMultiPhaseSpellCastingSlot(selectedSpellSlot, isCirclet));
        super.onClose();
    }
}