package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen;
import com.hollingsworth.arsnouveau.client.gui.buttons.ClearButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.CreateSpellButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiSpellSlot;
import com.hollingsworth.arsnouveau.client.gui.SearchBar;
import com.hollingsworth.arsnouveau.client.gui.book.EnterTextField;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketSetStaffSlot;
import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.api.spell.ISpellValidator;
import com.hollingsworth.arsnouveau.api.spell.SpellValidationError;
import com.hollingsworth.arsnouveau.common.capability.IPlayerCap;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.common.spell.validation.CombinedSpellValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.GlyphKnownValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.GlyphMaxTierValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.StartingCastMethodSpellValidator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ArsZeroStaffGUI extends SpellSlottedScreen {

    public enum StaffPhase {
        BEGIN, TICK, END
    }

    private StaffPhase currentPhase = StaffPhase.BEGIN;
    
    // Phase selection buttons (16x16 buttons for each row)
    private Button beginPhaseButton;
    private Button tickPhaseButton;
    private Button endPhaseButton;
    
    // Spell slots for the 10 shortcuts on the right (radial menu slots)
    private GuiSpellSlot[] spellSlots = new GuiSpellSlot[10];
    private GuiSpellSlot selectedSlotButton;
    private int selectedSpellSlot = -1;
    
    // Glyph selection
    private List<GlyphButton> glyphButtons = new ArrayList<>();
    private List<CraftingButton> craftingCells = new ArrayList<>();
    private List<AbstractSpellPart> unlockedSpells;
    private List<AbstractSpellPart> displayedGlyphs;
    private int page = 0;
    private int glyphsPerPage = 58;
    
    // Category tracking for glyph organization
    public int formTextRow = 0;
    public int augmentTextRow = 0;
    public int effectTextRow = 0;
    
    private PageButton nextButton;
    private PageButton previousButton;
    
    // UI elements
    private SearchBar searchBar;
    private EnterTextField spellNameBox;
    private String previousString = "";
    
    // Phase-specific spell storage (3 phases, each with 10 crafting cells)
    private List<List<AbstractSpellPart>> phaseSpells = new ArrayList<>();
    
    private ISpellValidator spellValidator;
    private List<SpellValidationError> validationErrors = new LinkedList<>();

    public ArsZeroStaffGUI() {
        super(InteractionHand.MAIN_HAND);
        ArsZero.LOGGER.debug("Creating Ars Zero Staff GUI");
        
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
        this.currentPhase = StaffPhase.BEGIN;
    }

    @Override
    public void init() {
        this.height = this.height + 32;
        super.init();
        
        if (selectedSpellSlot == -1) {
            selectedSpellSlot = caster.getCurrentSlot();
            ArsZero.LOGGER.info("GUI init: loaded selectedSpellSlot = {} from caster.getCurrentSlot()", selectedSpellSlot);
            if (selectedSpellSlot < 0 || selectedSpellSlot >= 10) {
                ArsZero.LOGGER.info("GUI init: slot out of range, resetting to 0");
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
        }
        
        this.spellValidator = new CombinedSpellValidator(
                ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator(),
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
        
        selectPhase(StaffPhase.BEGIN);
        
        resetCraftingCells();
        
        validate();
    }

    private void initSpellSlots() {
        // Create 10 spell slots for the staff (like the spellbook) - these are the radial menu slots
        for (int i = 0; i < 10; i++) {
            // Get name from BEGIN phase of each logical slot
            int beginPhysicalSlot = i * 3 + StaffPhase.BEGIN.ordinal();
            String name = caster.getSpellName(beginPhysicalSlot);
            GuiSpellSlot slot = new GuiSpellSlot(bookLeft + 281, bookTop - 1 + 15 * (i + 1), i, name, (b) -> {
                if (!(b instanceof GuiSpellSlot button) || this.selectedSpellSlot == button.slotNum) {
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
            int beginPhysicalSlot = selectedSpellSlot * 3 + StaffPhase.BEGIN.ordinal();
            spellNameBox.setValue(caster.getSpellName(beginPhysicalSlot));
        }
        
        validate();
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
                StaffPhase.values()[phase], physicalSlot, recipeList.size());
        }
        
        // Update crafting cells to show the loaded spells
        resetCraftingCells();
    }

    private void addPhaseButtons() {
        // Add 16x16 phase buttons for each row of crafting cells
        int startY = bookTop + 196; // Move down by 16 pixels more (180 + 16)
        int rowHeight = 20;
        int buttonSize = 16;
        int buttonX = bookLeft + 20; // Left side of each row
        
        for (int phase = 0; phase < 3; phase++) {
            StaffPhase staffPhase = StaffPhase.values()[phase];
            int y = startY + phase * rowHeight;
            
            Button phaseButton = Button.builder(Component.literal(""), (button) -> {
                selectPhase(staffPhase);
            }).bounds(buttonX, y, buttonSize, buttonSize).build();
            
            phaseButton.setTooltip(Tooltip.create(
                Component.translatable("gui.ars_zero.phase." + staffPhase.name().toLowerCase())
            ));
            
            addRenderableWidget(phaseButton);
            
            // Store reference to the button
            switch (staffPhase) {
                case BEGIN -> beginPhaseButton = phaseButton;
                case TICK -> tickPhaseButton = phaseButton;
                case END -> endPhaseButton = phaseButton;
            }
        }
    }

    private void addSearchBar() {
        String previousSearch = "";
        if (searchBar != null) {
            previousSearch = searchBar.getValue();
        }

        searchBar = new SearchBar(Minecraft.getInstance().font, bookRight - 130, bookTop - 3);
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
        // Add spell name field
        spellNameBox = new EnterTextField(minecraft.font, bookLeft + 16, bookBottom - 13);
        
        // Get spell name from BEGIN phase
        int beginPhysicalSlot = selectedSpellSlot * 3 + StaffPhase.BEGIN.ordinal();
        spellNameBox.setValue(caster.getSpellName(beginPhysicalSlot));
        addRenderableWidget(spellNameBox);

        addRenderableWidget(new CreateSpellButton(bookRight - 74, bookBottom - 13, (b) -> {
            ArsZero.LOGGER.info("Save button clicked!");
            this.saveSpell();
        }, this::getValidationErrors));
        addRenderableWidget(new ClearButton(bookRight - 129, bookBottom - 13, Component.translatable("ars_nouveau.spell_book_gui.clear"), (button) -> clear()));
    }

    private void addLeftSideTabs() {
    }

    private void selectPhase(StaffPhase phase) {
        ArsZero.LOGGER.debug("Selecting phase: {}", phase);
        currentPhase = phase;
        
        beginPhaseButton.active = (phase != StaffPhase.BEGIN);
        tickPhaseButton.active = (phase != StaffPhase.TICK);
        endPhaseButton.active = (phase != StaffPhase.END);
        
        updateCraftingCellVisibility();
        validate();
    }
    
    private void updateCraftingCellVisibility() {
        for (int i = 0; i < craftingCells.size(); i++) {
            CraftingButton cell = craftingCells.get(i);
            cell.visible = true;
        }
    }

    private void addPaginationButtons() {
        this.nextButton = addRenderableWidget(new PageButton(bookRight - 20, bookBottom - 6, true, this::onPageIncrease, true));
        this.previousButton = addRenderableWidget(new PageButton(bookLeft - 5, bookBottom - 6, false, this::onPageDec, true));
        
        updateNextPageButtons();
        previousButton.active = false;
        previousButton.visible = false;
    }

    private void layoutAllGlyphs(int page) {
        clearGlyphButtons(glyphButtons);
        formTextRow = 0;
        augmentTextRow = 0;
        effectTextRow = 0;
        
        if (displayedGlyphs.isEmpty()) {
            return;
        }

        final int PER_ROW = 6;
        final int MAX_ROWS = 6;
        boolean nextPage = false;
        int xStart = nextPage ? bookLeft + 154 : bookLeft + 20;
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
        int totalRowsPlaced = 0;
        int rowOffset = page == 0 ? 2 : 0;

        int yStart = bookTop + 2 + (page != 0 || sorted.getFirst() instanceof AbstractCastMethod ? 18 : 0);

        for (AbstractSpellPart part : sorted) {
            if (!foundForms && part instanceof AbstractCastMethod) {
                foundForms = true;
                adjustedRowsPlaced += 1;
                totalRowsPlaced += 1;
                formTextRow = page != 0 ? 0 : totalRowsPlaced;
                adjustedXPlaced = 0;
            } else if (!foundAugments && part instanceof AbstractAugment) {
                foundAugments = true;
                adjustedRowsPlaced += rowOffset;
                totalRowsPlaced += rowOffset;
                augmentTextRow = page != 0 ? 0 : totalRowsPlaced - 1;
                adjustedXPlaced = 0;
            } else if (!foundEffects && part instanceof AbstractEffect) {
                foundEffects = true;
                adjustedRowsPlaced += rowOffset;
                totalRowsPlaced += rowOffset;
                effectTextRow = page != 0 ? 0 : totalRowsPlaced - 1;
                adjustedXPlaced = 0;
            } else if (adjustedXPlaced >= PER_ROW) {
                adjustedRowsPlaced++;
                totalRowsPlaced++;
                adjustedXPlaced = 0;
            }

            if (adjustedRowsPlaced > MAX_ROWS) {
                if (nextPage) {
                    break;
                }
                nextPage = true;
                adjustedXPlaced = 0;
                adjustedRowsPlaced = (adjustedRowsPlaced - 1) % MAX_ROWS;
            }
            int xOffset = 20 * (adjustedXPlaced % PER_ROW) + (nextPage ? 134 : 0);

            int yPlace = adjustedRowsPlaced * 18 + yStart;

            GlyphButton cell = new GlyphButton(xStart + xOffset, yPlace, part, this::onGlyphClick);
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
                
                ArsZero.LOGGER.debug("Added glyph {} to {} phase at slot {}", 
                    glyphButton.abstractSpellPart.getLocaleName(), currentPhase, i);
                
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
        List<AbstractSpellPart> currentSpell = getCurrentPhaseSpell().stream().filter(part -> part != null).toList();
        
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
        
        for (GlyphButton glyphButton : glyphButtons) {
            glyphButton.validationErrors.clear();
            List<AbstractSpellPart> simulatedSpell = new ArrayList<>(currentSpell);
            simulatedSpell.add(GlyphRegistry.getSpellpartMap().get(glyphButton.abstractSpellPart.getRegistryName()));
            
            glyphButton.validationErrors.addAll(
                    spellValidator.validate(simulatedSpell).stream()
                            .filter(ve -> ve.getPosition() >= currentSpell.size()).toList()
            );
        }
    }

    public void resetCraftingCells() {
        for (CraftingButton button : craftingCells) {
            removeWidget(button);
        }
        craftingCells = new ArrayList<>();
        
        // Create 3 rows of 10 crafting cells each
        int startY = bookTop + 196; // Move down by 16 pixels more (180 + 16)
        int rowHeight = 20;
        int cellSpacing = 24;
        int startX = bookLeft + 40; // Start after the phase buttons
        
        for (int phase = 0; phase < 3; phase++) {
            for (int slot = 0; slot < 10; slot++) {
                int x = startX + slot * cellSpacing;
                int y = startY + phase * rowHeight;
                
                CraftingButton cell = new CraftingButton(x, y, this::onCraftingSlotClick, slot);
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
            ArsZero.LOGGER.debug("Removed glyph from phase {} at slot {}", phase, slot);
            
            validate();
        }
    }

    public void saveSpell() {
        ArsZero.LOGGER.info("saveSpell() called for slot {}", selectedSpellSlot);
        
        validate();
        if (!validationErrors.isEmpty()) {
            ArsZero.LOGGER.warn("Cannot save spell - validation errors present");
            return;
        }
        
        String spellName = spellNameBox.getValue();
        ArsZero.LOGGER.info("Spell name: {}", spellName);
        
        for (int phase = 0; phase < 3; phase++) {
            List<AbstractSpellPart> phaseSpell = phaseSpells.get(phase);
            List<AbstractSpellPart> filteredPhase = phaseSpell.stream()
                .filter(part -> part != null)
                .toList();
            
            Spell spell = new Spell(filteredPhase);
            
            int physicalSlot = selectedSpellSlot * 3 + phase;
            
            ArsZero.LOGGER.info("Sending PacketUpdateCaster for phase {} to slot {} with {} glyphs", 
                StaffPhase.values()[phase], physicalSlot, filteredPhase.size());
            
            com.hollingsworth.arsnouveau.common.network.Networking.sendToServer(new PacketUpdateCaster(spell, physicalSlot, spellName, true));
            
            ArsZero.LOGGER.info("Sent PacketUpdateCaster for phase {}", StaffPhase.values()[phase]);
        }
        
        spellSlots[selectedSpellSlot].spellName = spellName;
        
        Networking.sendToServer(new PacketSetStaffSlot(selectedSpellSlot));
        ArsZero.LOGGER.info("Sent PacketSetStaffSlot with slot {}", selectedSpellSlot);
        
        ArsZero.LOGGER.debug("Saved all 3 phases for logical slot {} (physical slots: {}, {}, {})", 
            selectedSpellSlot, 
            selectedSpellSlot * 3 + 0, 
            selectedSpellSlot * 3 + 1, 
            selectedSpellSlot * 3 + 2);
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
        ArsZero.LOGGER.debug("Cleared all phase spells from slot {}", selectedSpellSlot);
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


    @Override
    public void drawBackgroundElements(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundElements(graphics, mouseX, mouseY, partialTicks);
        
        // Add category labels for glyphs
        renderCategoryLabels(graphics);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
    }
    
    private void renderCategoryLabels(net.minecraft.client.gui.GuiGraphics graphics) {
        // Render category labels for glyphs (Forms, Augment, Effect)
        // Adjust Y position by 32 pixels to account for the taller GUI
        int yOffset = 32;
        int formOffset = 0;
        if (formTextRow >= 1) {
            graphics.drawString(font, Component.translatable("ars_nouveau.spell_book_gui.form").getString(), 
                formTextRow > 6 ? 154 : 20, 5 + 18 * (formTextRow + (formTextRow == 1 ? 0 : 1)) + yOffset, -8355712, false);
            formOffset = 1;
        }

        if (effectTextRow >= 1) {
            graphics.drawString(font, Component.translatable("ars_nouveau.spell_book_gui.effect").getString(), 
                effectTextRow > 6 ? 154 : 20, 5 + 18 * (effectTextRow % 7 + formOffset) + yOffset, -8355712, false);
        }
        if (augmentTextRow >= 1) {
            graphics.drawString(font, Component.translatable("ars_nouveau.spell_book_gui.augment").getString(), 
                augmentTextRow > 6 ? 154 : 20, 5 + 18 * (augmentTextRow + formOffset) + yOffset, -8355712, false);
        }
    }

    public StaffPhase getCurrentPhase() {
        return currentPhase;
    }

    @Override
    public void onClose() {
        Networking.sendToServer(new PacketSetStaffSlot(selectedSpellSlot));
        ArsZero.LOGGER.info("onClose: sent PacketSetStaffSlot with slot {}", selectedSpellSlot);
        super.onClose();
    }
}