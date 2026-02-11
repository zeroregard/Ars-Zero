package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.buttons.ManaIndicator;
import com.github.ars_zero.client.gui.buttons.StaffArrowButton;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.IFilter;
import com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen;
import com.alexthw.sauce.api.IPropagator;
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
import com.github.ars_zero.client.gui.validators.SpellPhaseValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.StartingCastMethodSpellValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.BaseSpellValidationError;
import com.hollingsworth.arsnouveau.api.registry.FamiliarRegistry;
import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.client.gui.book.GuiFamiliarScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import java.util.stream.Collectors;

public abstract class AbstractMultiPhaseCastDeviceScreen extends SpellSlottedScreen implements StaffSlotClipboardHost {

    private SpellPhase currentPhase = SpellPhase.BEGIN;

    private GuiImageButton beginPhaseButton;
    private GuiImageButton tickPhaseButton;
    private GuiImageButton endPhaseButton;

    // Spell slots for the 10 shortcuts on the right (radial menu slots)
    private StaffSpellSlot[] spellSlots = new StaffSpellSlot[10];
    private StaffSpellSlot selectedSlotButton;
    private int selectedSpellSlot = -1;
    private StaffSlotClipboardSupport slotClipboardSupport;

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
    public int filterTextRow = 0;
    public int propagateFilterTextRow = 0;

    private StaffArrowButton nextButton;
    private StaffArrowButton previousButton;

    // UI elements
    private SearchBar searchBar;
    private EnterTextField spellNameBox;
    private String previousString = "";
    private List<Button> categoryButtons = new ArrayList<>();

    private SpellPhaseSlots phaseSpells;
    private final int[] slotDelays = new int[10];

    private ISpellValidator baseSpellValidator;
    private List<SpellValidationError> validationErrors = new LinkedList<>();
    protected ItemStack deviceStack;
    private InteractionHand guiHand;

    protected abstract ResourceLocation getBackgroundTexture();

    @Override
    public void onBookstackUpdated(ItemStack stack) {
        super.onBookstackUpdated(stack);
        this.deviceStack = stack;
        loadSlotDelayValues();
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

        // Initialize unlocked spells - we'll do this in init() when playerCap is
        // available
        this.unlockedSpells = new ArrayList<>();
        this.displayedGlyphs = new ArrayList<>();

        this.phaseSpells = new SpellPhaseSlots(10);

        this.currentPhase = SpellPhase.BEGIN;
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
            refreshDeviceStack();
            if (caster != null) {
                selectedSpellSlot = caster.getCurrentSlot();
                if (selectedSpellSlot < 0 || selectedSpellSlot >= 10) {
                    selectedSpellSlot = 0;
                }
            } else {
                selectedSpellSlot = 0;
            }
        }

        IPlayerCap playerCapData = CapabilityRegistry.getPlayerDataCap(player);
        List<AbstractSpellPart> parts = playerCapData == null ? new ArrayList<>()
                : new ArrayList<>(playerCapData.getKnownGlyphs().stream()
                        .filter(AbstractSpellPart::shouldShowInSpellBook).toList());
        parts.addAll(GlyphRegistry.getDefaultStartingSpells());

        int tier = 1;
        boolean isCreativeStaff = false;
        if (bookStack.getItem() instanceof AbstractSpellStaff staff) {
            tier = staff.getTier().value;
            if (staff.getTier() == SpellTier.CREATIVE) {
                isCreativeStaff = true;
                parts = new ArrayList<>(GlyphRegistry.getSpellpartMap().values().stream()
                        .filter(AbstractSpellPart::shouldShowInSpellBook).toList());
            }
        } else if (bookStack.getItem() instanceof AbstractMultiPhaseCastDevice device) {
            tier = device.getTier().value;
            if (device.getTier() == SpellTier.CREATIVE) {
                isCreativeStaff = true;
                parts = new ArrayList<>(GlyphRegistry.getSpellpartMap().values().stream()
                        .filter(AbstractSpellPart::shouldShowInSpellBook).toList());
            }
        }

        this.baseSpellValidator = createBaseSpellValidator(tier, playerCapData, isCreativeStaff);

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

        selectPhase(SpellPhase.BEGIN);

        validate();

        if (slotClipboardSupport == null) {
            slotClipboardSupport = new StaffSlotClipboardSupport(this);
        }
        slotClipboardSupport.onInit();
    }

    private void initSpellSlots() {
        int slotStartX = bookLeft + 355;
        int slotStartY = bookTop + 37;
        int slotSpacing = 16;

        for (int i = 0; i < 10; i++) {
            int beginPhysicalSlot = i * 3 + SpellPhase.BEGIN.ordinal();
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
            int beginPhysicalSlot = selectedSpellSlot * 3 + SpellPhase.BEGIN.ordinal();
            spellNameBox.setValue(caster.getSpellName(beginPhysicalSlot));
        }

        validate();
    }

    private void loadSpellFromSlot() {
        for (SpellPhase phase : SpellPhase.values()) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.getPhaseList(phase);
            phaseSpell.clear();
            for (int i = 0; i < 10; i++) {
                phaseSpell.add(null);
            }
        }

        for (SpellPhase phase : SpellPhase.values()) {
            int physicalSlot = selectedSpellSlot * 3 + phase.ordinal();
            Spell spell = caster.getSpell(physicalSlot);

            List<AbstractSpellPart> phaseSpell = phaseSpells.getPhaseList(phase);

            List<AbstractSpellPart> recipeList = new ArrayList<>();
            for (AbstractSpellPart part : spell.recipe()) {
                recipeList.add(part);
            }

            for (int i = 0; i < recipeList.size() && i < 10; i++) {
                phaseSpell.set(i, recipeList.get(i));
            }
        }

        resetCraftingCells();
    }

    private void addPhaseButtons() {
        int startY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y + 2;
        int rowHeight = PHASE_ROW_HEIGHT + 2;
        int buttonSize = 16;
        int buttonX = bookLeft + PHASE_SECTION_SHIFT_X + 11;

        GuiImageButton beginButton = new GuiImageButton(buttonX, startY, buttonSize, buttonSize,
                StaffGuiTextures.ICON_START, (button) -> selectPhase(SpellPhase.BEGIN));
        beginButton.withTooltip(Component.literal("Begin Phase"));
        beginPhaseButton = beginButton;
        addRenderableWidget(beginButton);

        GuiImageButton tickButton = new GuiImageButton(buttonX, startY + rowHeight, buttonSize, buttonSize,
                StaffGuiTextures.ICON_TICK, (button) -> selectPhase(SpellPhase.TICK));
        tickPhaseButton = tickButton;
        addRenderableWidget(tickButton);

        GuiImageButton endButton = new GuiImageButton(buttonX, startY + rowHeight * 2, buttonSize, buttonSize,
                StaffGuiTextures.ICON_END, (button) -> selectPhase(SpellPhase.END));
        endButton.withTooltip(Component.literal("End Phase"));
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

        int beginPhysicalSlot = selectedSpellSlot * 3 + SpellPhase.BEGIN.ordinal();
        spellNameBox.setValue(caster.getSpellName(beginPhysicalSlot));
        addRenderableWidget(spellNameBox);

        addRenderableWidget(new CreateSpellButton(bookRight - 84, bookBottom - 29, (b) -> {
            this.saveSpell();
        }, this::getValidationErrors));
        addRenderableWidget(new ClearButton(bookRight - 137, bookBottom - 29,
                Component.translatable("ars_nouveau.spell_book_gui.clear"), (button) -> clear()));
    }

    private void addLeftSideTabs() {
        addRenderableWidget(
                new GuiImageButton(bookLeft + 54, bookTop + 32, 16, 16, StaffGuiTextures.STYLE_ICON, (b) -> {
                    openParticleScreen();
                }).withTooltip(Component.translatable("ars_nouveau.gui.spell_style")));

        addRenderableWidget(
                new GuiImageButton(bookLeft + 54, bookTop + 53, 16, 16, StaffGuiTextures.ICON_DISCORD, (b) -> {
                    try {
                        Util.getPlatform().openUri(
                                new URI("https://discord.com/channels/743298050222587978/1432008462929236071"));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }).withTooltip(Component.translatable("ars_nouveau.gui.discord")));

        addRenderableWidget(
                new GuiImageButton(bookLeft + 54, bookTop + 73, 16, 16, StaffGuiTextures.ICON_FAMILIARS, (b) -> {
                    Collection<ResourceLocation> familiarHolders = new ArrayList<>();
                    IPlayerCap cap = CapabilityRegistry.getPlayerDataCap(player);
                    if (cap != null) {
                        familiarHolders = cap.getUnlockedFamiliars().stream()
                                .map(s -> s.familiarHolder.getRegistryName()).collect(Collectors.toList());
                    }
                    Collection<ResourceLocation> finalFamiliarHolders = familiarHolders;
                    Minecraft.getInstance()
                            .setScreen(new GuiFamiliarScreen(FamiliarRegistry.getFamiliarHolderMap().values().stream()
                                    .filter(f -> finalFamiliarHolders.contains(f.getRegistryName()))
                                    .collect(Collectors.toList()), this));
                }).withTooltip(Component.translatable("ars_nouveau.gui.familiar")));

        addRenderableWidget(new GuiImageButton(bookLeft + 54, bookTop + 93, 16, 16, StaffGuiTextures.ICON_DOCS, (b) -> {
            DocClientUtils.openBook();
        }).withTooltip(Component.translatable("ars_nouveau.gui.notebook")));

        addAffinityButton();
        
        addRenderableWidget(
                new GuiImageButton(bookLeft + 54, bookTop + 133, 16, 16, StaffGuiTextures.CASTING_STYLE_ICON, (b) -> {
                    CastingStyleScreen.openScreen(this, selectedSpellSlot, deviceStack, this.guiHand);
                }).withTooltip(Component.translatable("ars_zero.gui.casting_style")));
    }

    private void addAffinityButton() {
        boolean affinityLoaded = ModList.get().isLoaded("ars_affinity");

        GuiImageButton affinityButton = new GuiImageButton(bookLeft + 54, bookTop + 113, 16, 16,
                StaffGuiTextures.ICON_AFFINITY, (b) -> {
                    if (!affinityLoaded) {
                        try {
                            Util.getPlatform()
                                    .openUri(new URI("https://www.curseforge.com/minecraft/mc-mods/ars-affinity"));
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                        return;
                    }

                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player != null) {
                        try {
                            Class<?> affinityScreenClass = Class
                                    .forName("com.github.ars_affinity.client.screen.AffinityScreen");
                            Object affinityScreen = affinityScreenClass.getConstructor(
                                    net.minecraft.world.entity.player.Player.class,
                                    boolean.class,
                                    net.minecraft.client.gui.screens.Screen.class)
                                    .newInstance(minecraft.player, true, this);
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
                    Component.literal(" - install Ars Affinities.")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withUnderlined(true)));
        }
        affinityButton.withTooltip(tooltip);
        addRenderableWidget(affinityButton);
    }

    private void openParticleScreen() {
        MultiphaseDeviceStylesScreen.openScreen(this, selectedSpellSlot, deviceStack, this.guiHand);
    }

    private void selectPhase(SpellPhase phase) {
        currentPhase = phase;

        beginPhaseButton.active = (phase != SpellPhase.BEGIN);
        tickPhaseButton.active = (phase != SpellPhase.TICK);
        endPhaseButton.active = (phase != SpellPhase.END);

        beginPhaseButton.image = (phase == SpellPhase.BEGIN) ? StaffGuiTextures.ICON_START_SELECTED
                : StaffGuiTextures.ICON_START;
        tickPhaseButton.image = (phase == SpellPhase.TICK) ? StaffGuiTextures.ICON_TICK_SELECTED
                : StaffGuiTextures.ICON_TICK;
        endPhaseButton.image = (phase == SpellPhase.END) ? StaffGuiTextures.ICON_END_SELECTED
                : StaffGuiTextures.ICON_END;

        updateCraftingCellVisibility();
        validate();
    }

    private void updateCraftingCellVisibility() {
        for (int i = 0; i < craftingCells.size(); i++) {
            CraftingButton cell = craftingCells.get(i);
            cell.visible = true;
        }
    }

    private void onDelayValueChanged(int value) {
        if (selectedSpellSlot < 0 || selectedSpellSlot >= slotDelays.length) {
            return;
        }
        int clamped = Mth.clamp(value, 1, 20);
        slotDelays[selectedSpellSlot] = clamped;
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

    private void refreshDeviceStack() {
        if (player == null) {
            return;
        }

        ItemStack freshStack = ItemStack.EMPTY;
        boolean isCirclet = bookStack != null && bookStack.getItem() instanceof SpellcastingCirclet;

        if (guiHand != null) {
            freshStack = player.getItemInHand(guiHand);
            if (!(freshStack.getItem() instanceof AbstractMultiPhaseCastDevice)) {
                freshStack = ItemStack.EMPTY;
            }
        } else {
            ItemStack mainStack = player.getMainHandItem();
            if (mainStack.getItem() instanceof AbstractMultiPhaseCastDevice) {
                freshStack = mainStack;
            } else {
                ItemStack offStack = player.getOffhandItem();
                if (offStack.getItem() instanceof AbstractMultiPhaseCastDevice) {
                    freshStack = offStack;
                }
            }
        }

        if (freshStack.isEmpty() && isCirclet) {
            freshStack = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findEquippedCurio(
                            equipped -> equipped.getItem() instanceof AbstractMultiPhaseCastDevice,
                            player)
                    .map(result -> result.getRight())
                    .orElse(ItemStack.EMPTY);
        }

        if (!freshStack.isEmpty() && freshStack.getItem() instanceof AbstractMultiPhaseCastDevice) {
            deviceStack = freshStack;
            caster = SpellCasterRegistry.from(freshStack);
            onBookstackUpdated(freshStack);
        }
    }

    private void addPaginationButtons() {
        this.nextButton = addRenderableWidget(
                new StaffArrowButton(bookRight - 33, bookBottom - 155, true, this::onPageIncrease));
        this.previousButton = addRenderableWidget(
                new StaffArrowButton(bookLeft + 75, bookBottom - 155, false, this::onPageDec));

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
        filterTextRow = 0;
        propagateFilterTextRow = 0;

        if (displayedGlyphs.isEmpty()) {
            return;
        }

        final int PER_ROW = 14;
        final int MAX_ROWS = 5;
        int adjustedRowsPlaced = 0;
        boolean foundForms = false;
        boolean foundAugments = false;
        boolean foundFilters = false;
        boolean foundPropagateFilters = false;
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
                formTextRow = adjustedRowsPlaced;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder,
                        StaffGuiTextures.GLYPH_CATEGORY_FORM,
                        Component.translatable("ars_nouveau.form_icon_tooltip"));
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
                augmentTextRow = adjustedRowsPlaced;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder,
                        StaffGuiTextures.GLYPH_CATEGORY_AUGMENT,
                        Component.translatable("ars_nouveau.augment_icon_tooltip"));
                addRenderableWidget(btn);
                categoryButtons.add(btn);
                adjustedXPlaced++;
                if (adjustedXPlaced >= PER_ROW) {
                    adjustedRowsPlaced++;
                    adjustedXPlaced = 0;
                }
                foundAugments = true;
            } else if (!foundPropagateFilters && part instanceof IPropagator) {
                if (adjustedRowsPlaced >= MAX_ROWS) {
                    break;
                }
                int xOffsetPlaceholder = horizontalSpacing * (adjustedXPlaced % PER_ROW);
                int yPlacePlaceholder = adjustedRowsPlaced * 18 + yStart;
                propagateFilterTextRow = adjustedRowsPlaced;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder,
                        StaffGuiTextures.GLYPH_CATEGORY_PROPAGATE_FILTER,
                        Component.translatable("ars_nouveau.subform_icon_tooltip"));
                addRenderableWidget(btn);
                categoryButtons.add(btn);
                adjustedXPlaced++;
                if (adjustedXPlaced >= PER_ROW) {
                    adjustedRowsPlaced++;
                    adjustedXPlaced = 0;
                }
                foundPropagateFilters = true;
            } else if (!foundFilters && part instanceof IFilter) {
                if (adjustedRowsPlaced >= MAX_ROWS) {
                    break;
                }
                int xOffsetPlaceholder = horizontalSpacing * (adjustedXPlaced % PER_ROW);
                int yPlacePlaceholder = adjustedRowsPlaced * 18 + yStart;
                filterTextRow = adjustedRowsPlaced;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder,
                        StaffGuiTextures.GLYPH_CATEGORY_FILTER,
                        Component.translatable("ars_nouveau.spell_book_gui.filter"));
                addRenderableWidget(btn);
                categoryButtons.add(btn);
                adjustedXPlaced++;
                if (adjustedXPlaced >= PER_ROW) {
                    adjustedRowsPlaced++;
                    adjustedXPlaced = 0;
                }
                foundFilters = true;
            } else if (!foundEffects && part instanceof AbstractEffect && !(part instanceof IFilter)
                    && !(part instanceof IPropagator)) {
                if (adjustedRowsPlaced >= MAX_ROWS) {
                    break;
                }
                int xOffsetPlaceholder = horizontalSpacing * (adjustedXPlaced % PER_ROW);
                int yPlacePlaceholder = adjustedRowsPlaced * 18 + yStart;
                effectTextRow = adjustedRowsPlaced;
                GuiImageButton btn = createCategoryIcon(baseX + xOffsetPlaceholder, yPlacePlaceholder,
                        StaffGuiTextures.GLYPH_CATEGORY_EFFECT,
                        Component.translatable("ars_nouveau.effect_icon_tooltip"));
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

        List<AbstractSpellPart> currentSpell = getCurrentPhaseSpell();
        boolean isSpellEmpty = currentSpell.stream().allMatch(part -> part == null);

        if (isSpellEmpty && !(glyphButton.abstractSpellPart instanceof AbstractCastMethod)) {
            return;
        }

        int currentPhaseIndex = currentPhase.ordinal();
        for (int i = 0; i < 10; i++) {
            int cellIndex = currentPhaseIndex * 10 + i;
            CraftingButton cell = craftingCells.get(cellIndex);

            if (cell.getAbstractSpellPart() == null) {
                cell.setAbstractSpellPart(glyphButton.abstractSpellPart);

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
        return phaseSpells.getPhaseList(currentPhase);
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
        SpellCompositeContext.getInstance().setCurrentSpell(currentSpell);
        for (CraftingButton b : craftingCells) {
            b.validationErrors.clear();
        }
        List<SpellValidationError> errors = getSpellValidatorForCurrentPhase().validate(currentSpell);
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
        boolean isSpellEmpty = currentSpell.isEmpty();
        ISpellValidator validator = getSpellValidatorForCurrentPhase();
        for (GlyphButton glyphButton : glyphButtons) {
            glyphButton.validationErrors.clear();
            glyphButton.augmentingParent = lastEffect;
            
            if (isSpellEmpty && !(glyphButton.abstractSpellPart instanceof AbstractCastMethod)) {
                glyphButton.validationErrors.add(new BaseSpellValidationError(
                        0,
                        glyphButton.abstractSpellPart,
                        "starting_cast_method"
                ));
            }
            
            AbstractSpellPart toAdd = GlyphRegistry.getSpellpartMap()
                    .get(glyphButton.abstractSpellPart.getRegistryName());
            slicedSpell.add(toAdd);
            glyphButton.validationErrors.addAll(
                    validator.validate(slicedSpell).stream()
                            .filter(ve -> ve.getPosition() >= slicedSpell.size() - 1).toList());
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

        int phaseIndex = 0;
        for (SpellPhase phase : SpellPhase.values()) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.getPhaseList(phase);
            for (int slot = 0; slot < 10; slot++) {
                int x = startX + slot * cellSpacing;
                int y = startY + phaseIndex * rowHeight;

                StaffCraftingButton cell = new StaffCraftingButton(x, y, this::onCraftingSlotClick, slot);
                addRenderableWidget(cell);
                craftingCells.add(cell);

                AbstractSpellPart spellPart = slot < phaseSpell.size() ? phaseSpell.get(slot) : null;
                cell.setAbstractSpellPart(spellPart);

                cell.visible = true;
            }
            phaseIndex++;
        }
    }

    public void onCraftingSlotClick(Button button) {
        CraftingButton cell = (CraftingButton) button;
        if (cell.getAbstractSpellPart() != null) {
            int cellIndex = craftingCells.indexOf(cell);
            int phaseIndex = cellIndex / 10;
            int slot = cellIndex % 10;

            SpellPhase phase = SpellPhase.values()[phaseIndex];
            List<AbstractSpellPart> phaseSpell = phaseSpells.getPhaseList(phase);
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

        for (SpellPhase phase : SpellPhase.values()) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.getPhaseList(phase);
            List<AbstractSpellPart> filteredPhase = phaseSpell.stream()
                    .filter(part -> part != null)
                    .toList();

            Spell spell = new Spell(filteredPhase);

            int physicalSlot = selectedSpellSlot * 3 + phase.ordinal();

            com.hollingsworth.arsnouveau.common.network.Networking
                    .sendToServer(new PacketUpdateCaster(spell, physicalSlot, spellName, true));
        }

        spellSlots[selectedSpellSlot].spellName = spellName;

        boolean isCirclet = bookStack.getItem() instanceof SpellcastingCirclet;
        Networking.sendToServer(new PacketSetMultiPhaseSpellCastingSlot(selectedSpellSlot, isCirclet));
    }

    public void clear() {
        for (SpellPhase phase : SpellPhase.values()) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.getPhaseList(phase);
            phaseSpell.clear();
            for (int i = 0; i < 10; i++) {
                phaseSpell.add(null);
            }

            int physicalSlot = selectedSpellSlot * 3 + phase.ordinal();
            Spell emptySpell = new Spell();
            com.hollingsworth.arsnouveau.common.network.Networking
                    .sendToServer(new PacketUpdateCaster(emptySpell, physicalSlot, "", false));
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
                        AbstractSpellPart part = GlyphRegistry.getSpellpartMap()
                                .get(glyphButton.abstractSpellPart.getRegistryName());
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
        GuiImageButton button = new GuiImageButton(x, y, 18, 16, texture, (b) -> {
        });
        button.soundDisabled = true;
        button.withTooltip(tooltip);
        return button;
    }

    @Override
    public void drawBackgroundElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        ResourceLocation backgroundTexture = getBackgroundTexture();
        if (backgroundTexture != null) {
            graphics.blit(backgroundTexture, 0, 0, 0, 0, STAFF_GUI_WIDTH, STAFF_GUI_HEIGHT, STAFF_GUI_WIDTH,
                    STAFF_GUI_HEIGHT);
        }

        int rowX = PHASE_ROW_TEXTURE_X_OFFSET + PHASE_SECTION_SHIFT_X - 4;
        for (int i = 0; i < SpellPhase.values().length; i++) {
            SpellPhase phase = SpellPhase.values()[i];
            ResourceLocation rowTexture = phase == currentPhase ? StaffGuiTextures.SPELL_PHASE_ROW_SELECTED
                    : StaffGuiTextures.SPELL_PHASE_ROW;
            int rowY = PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y + i * (PHASE_ROW_HEIGHT + 2);
            graphics.blit(rowTexture, rowX, rowY, 0, 0, PHASE_ROW_TEXTURE_WIDTH, PHASE_ROW_TEXTURE_HEIGHT,
                    PHASE_ROW_TEXTURE_WIDTH, PHASE_ROW_TEXTURE_HEIGHT);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderManaIndicators(graphics, mouseX, mouseY);
        renderTickPhaseTooltip(graphics, mouseX, mouseY);
        if (slotClipboardSupport != null) {
            slotClipboardSupport.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (slotClipboardSupport != null && slotClipboardSupport.mouseClicked(mouseX, mouseY, button, spellSlots)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasControlDown() && getFocused() != searchBar && getFocused() != spellNameBox) {
            if (isCopy(keyCode)) {
                if (slotClipboardSupport != null && selectedSpellSlot >= 0 && selectedSpellSlot < 10) {
                    slotClipboardSupport.copySlot(selectedSpellSlot);
                    return true;
                }
            } else if (isPaste(keyCode)) {
                if (slotClipboardSupport != null && selectedSpellSlot >= 0 && selectedSpellSlot < 10) {
                    slotClipboardSupport.pasteSlot(selectedSpellSlot);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public AbstractCaster<?> getHostCaster() {
        return caster;
    }

    @Override
    public ItemStack getHostDeviceStack() {
        return deviceStack;
    }

    @Override
    public InteractionHand getHostGuiHand() {
        return guiHand;
    }

    @Override
    public boolean isHostCircletDevice() {
        return isCircletDevice();
    }

    @Override
    public int getHostSelectedSpellSlot() {
        return selectedSpellSlot;
    }

    @Override
    public int getHostStoredDelayValueForSlot(int logicalSlot) {
        if (logicalSlot < 0 || logicalSlot >= slotDelays.length) {
            return 1;
        }
        return slotDelays[logicalSlot];
    }

    @Override
    public void setHostStoredDelayValueForSlot(int logicalSlot, int delay) {
        if (logicalSlot < 0 || logicalSlot >= slotDelays.length) {
            return;
        }
        int clamped = Mth.clamp(delay, 1, 20);
        slotDelays[logicalSlot] = clamped;
        if (deviceStack != null && !deviceStack.isEmpty()) {
            AbstractMultiPhaseCastDevice.setSlotTickDelay(deviceStack, logicalSlot, clamped);
        }
    }

    @Override
    public void setHostSlotSpellName(int logicalSlot, String name) {
        if (logicalSlot < 0 || logicalSlot >= spellSlots.length || spellSlots[logicalSlot] == null) {
            return;
        }
        spellSlots[logicalSlot].spellName = name;
    }

    @Override
    public void setHostSpellNameBoxValue(String value) {
        if (spellNameBox != null) {
            spellNameBox.setValue(value);
        }
    }

    @Override
    public SpellPhaseSlots getHostPhaseSpells() {
        return phaseSpells;
    }

    @Override
    public void hostResetCraftingCells() {
        resetCraftingCells();
    }

    @Override
    public void hostValidate() {
        validate();
    }

    @Override
    public SpellPhase getHostCurrentPhase() {
        return currentPhase;
    }

    private void renderTickPhaseTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tickPhaseButton != null) {
            int buttonX = tickPhaseButton.getX();
            int buttonY = tickPhaseButton.getY();
            int buttonWidth = tickPhaseButton.getWidth();
            int buttonHeight = tickPhaseButton.getHeight();

            if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                    mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
                int delay = getStoredDelayValueForSelectedSlot();
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(Component.literal("Tick Phase"));
                tooltipLines.add(Component.literal("Delay: " + delay + " tick ").append(
                        Component.literal("(Scroll to change)")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))));
                graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltipLines, mouseX, mouseY);
            }
        }
    }

    private void renderManaIndicators(GuiGraphics graphics, int mouseX, int mouseY) {
        int phaseRowStartX = bookLeft + PHASE_ROW_TEXTURE_X_OFFSET + PHASE_SECTION_SHIFT_X - 4;
        int phaseRowEndX = phaseRowStartX + PHASE_ROW_TEXTURE_WIDTH;
        int indicatorX = phaseRowEndX + 4 - 8 - 4 + 1;
        int baseY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y;
        int rowHeight = PHASE_ROW_HEIGHT + 2;
        int indicatorHeight = 14;

        ManaIndicator hoveredIndicator = null;

        int phaseIndex = 0;
        for (SpellPhase phase : SpellPhase.values()) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.getPhaseList(phase);
            int indicatorY = baseY + phaseIndex * rowHeight + (PHASE_ROW_HEIGHT - indicatorHeight) / 2 - 1 + 1;

            ManaIndicator indicator = new ManaIndicator(indicatorX, indicatorY, phaseSpell);
            indicator.render(graphics, player);

            if (indicator.isHovered(mouseX, mouseY)) {
                hoveredIndicator = indicator;
            }
            phaseIndex++;
        }

        if (hoveredIndicator != null) {
            hoveredIndicator.renderTooltip(graphics, mouseX, mouseY);
        }
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
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_FORM,
                    Component.translatable("ars_nouveau.spell_book_gui.form"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
            formOffset = 1;
        }
        if (propagateFilterTextRow >= 1) {
            int x = (propagateFilterTextRow > 6 ? 154 : 20);
            int y = 5 + 18 * (propagateFilterTextRow % 7 + formOffset) + yOffset;
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_PROPAGATE_FILTER,
                    Component.translatable("ars_nouveau.subform_icon_tooltip"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
            formOffset++;
        }
        if (filterTextRow >= 1) {
            int x = (filterTextRow > 6 ? 154 : 20);
            int y = 5 + 18 * (filterTextRow % 7 + formOffset) + yOffset;
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_FILTER,
                    Component.translatable("ars_nouveau.spell_book_gui.filter"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
            formOffset++;
        }
        if (effectTextRow >= 1) {
            int x = (effectTextRow > 6 ? 154 : 20);
            int y = 5 + 18 * (effectTextRow % 7 + formOffset) + yOffset;
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_EFFECT,
                    Component.translatable("ars_nouveau.spell_book_gui.effect"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
        }
        if (augmentTextRow >= 1) {
            int x = (augmentTextRow > 6 ? 154 : 20);
            int y = 5 + 18 * (augmentTextRow + formOffset) + yOffset;
            GuiImageButton btn = createCategoryIcon(x, y, StaffGuiTextures.GLYPH_CATEGORY_AUGMENT,
                    Component.translatable("ars_nouveau.spell_book_gui.augment"));
            addRenderableWidget(btn);
            categoryButtons.add(btn);
        }
    }

    private CombinedSpellValidator createBaseSpellValidator(int tier, IPlayerCap playerCapData,
            boolean isCreativeStaff) {
        return new CombinedSpellValidator(
                ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator(),
                new ActionAugmentationPolicyValidator(),
                new GlyphMaxTierValidator(tier),
                new GlyphKnownValidator(player.isCreative() || isCreativeStaff ? null : playerCapData),
                new StartingCastMethodSpellValidator());
    }

    private ISpellValidator getSpellValidatorForCurrentPhase() {
        return new CombinedSpellValidator(
                baseSpellValidator,
                new SpellPhaseValidator(currentPhase));
    }

    public SpellPhase getCurrentPhase() {
        return currentPhase;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tickPhaseButton != null) {
            int buttonX = tickPhaseButton.getX();
            int buttonY = tickPhaseButton.getY();
            int buttonWidth = tickPhaseButton.getWidth();
            int buttonHeight = tickPhaseButton.getHeight();

            if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                    mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
                int currentDelay = getStoredDelayValueForSelectedSlot();
                int newDelay = currentDelay;
                if (scrollY > 0) {
                    newDelay = Math.min(20, currentDelay + 1);
                } else if (scrollY < 0) {
                    newDelay = Math.max(1, currentDelay - 1);
                }
                if (newDelay != currentDelay) {
                    onDelayValueChanged(newDelay);
                    return true;
                }
            }
        }

        if (scrollY != 0 && nextButton != null && previousButton != null) {
            if (scrollY < 0) {
                if (page + 1 < getNumPages()) {
                    page++;
                    if (displayedGlyphs.size() < glyphsPerPage * (page + 1)) {
                        nextButton.visible = false;
                        nextButton.active = false;
                    }
                    previousButton.active = true;
                    previousButton.visible = true;
                    layoutAllGlyphs(page);
                    validate();
                    return true;
                }
            } else {
                if (page > 0) {
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
                    return true;
                }
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        boolean isCirclet = bookStack.getItem() instanceof SpellcastingCirclet;
        Networking.sendToServer(new PacketSetMultiPhaseSpellCastingSlot(selectedSpellSlot, isCirclet));
        super.onClose();
    }
}