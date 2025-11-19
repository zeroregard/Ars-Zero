package com.github.ars_zero.client.gui;

import com.hollingsworth.arsnouveau.api.documentation.DocAssets;
import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.api.particle.configurations.ParticleConfigWidgetProvider;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimeline;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimelineType;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineMap;
import com.hollingsworth.arsnouveau.api.registry.ParticleTimelineRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.HeaderWidget;
import com.hollingsworth.arsnouveau.client.gui.book.BaseBook;
import com.hollingsworth.arsnouveau.client.gui.book.PropWidgetList;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiSpellSlot;
import com.hollingsworth.arsnouveau.client.gui.buttons.PropertyButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.SelectableButton;
import com.hollingsworth.arsnouveau.client.gui.documentation.DocEntryButton;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import com.hollingsworth.nuggets.client.gui.GuiHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StaffParticleScreen extends BaseBook {
    TimelineMap.MutableTimelineMap timelineMap;

    public IParticleTimelineType<?> selectedTimeline = null;

    List<AbstractWidget> rightPageWidgets = new ArrayList<>();
    List<PropertyButton> leftPageButtons = new ArrayList<>();
    List<GuiSpellSlot> hotkeySlots = new ArrayList<>();

    ParticleConfigWidgetProvider propertyWidgetProvider;
    DocEntryButton timelineButton;
    int rowOffset = 0;
    boolean hasMoreElements = false;
    boolean hasPreviousElements = false;
    public static IParticleTimelineType<?> LAST_SELECTED_PART = null;
    public static int lastOpenedHash;
    public static StaffParticleScreen lastScreen;

    AbstractMultiPhaseCastDeviceScreen previousScreen;
    GuiImageButton upButton;
    GuiImageButton downButton;
    boolean allExpanded = false;
    PropWidgetList propWidgetList;
    
    int hotkeySlot;
    ItemStack staffStack;
    InteractionHand hand;

    public StaffParticleScreen(AbstractMultiPhaseCastDeviceScreen previousScreen, int hotkeySlot, ItemStack stack, InteractionHand stackHand) {
        super();
        this.previousScreen = previousScreen;
        this.hotkeySlot = hotkeySlot;
        this.staffStack = stack;
        this.hand = stackHand;
        
        int beginPhysicalSlot = hotkeySlot * 3 + 0;
        var caster = com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(stack);
        this.timelineMap = caster.getParticles(beginPhysicalSlot).mutable();
        selectedTimeline = LAST_SELECTED_PART == null ? findTimelineFromSlot(caster, beginPhysicalSlot) : LAST_SELECTED_PART;
        LAST_SELECTED_PART = selectedTimeline;
    }

    public IParticleTimelineType<?> findTimelineFromSlot(com.hollingsworth.arsnouveau.api.spell.AbstractCaster<?> caster, int physicalSlot) {
        IParticleTimelineType<?> timeline = null;
        for (AbstractSpellPart spellPart : caster.getSpell(physicalSlot).recipe()) {
            var allTimelines = ParticleTimelineRegistry.PARTICLE_TIMELINE_REGISTRY.entrySet();
            for (var entry : allTimelines) {
                if (entry.getValue().getSpellPart() == spellPart) {
                    timeline = entry.getValue();
                }
            }
            if (timeline != null) {
                break;
            }
        }
        if (timeline == null) {
            timeline = ParticleTimelineRegistry.PROJECTILE_TIMELINE.get();
        }
        return timeline;
    }

    @Override
    public void init() {
        super.init();

        propWidgetList = new PropWidgetList(bookLeft + LEFT_PAGE_OFFSET + 13, bookLeft + RIGHT_PAGE_OFFSET, bookTop + PAGE_TOP_OFFSET, this::onPropertySelected, this::onDependenciesChanged, propWidgetList);

        upButton = new GuiImageButton(bookLeft + LEFT_PAGE_OFFSET + 87, bookBottom - 30, DocAssets.BUTTON_UP, (button) -> {
            rowOffset = Math.max(rowOffset - 1, 0);
            layoutLeftPage();
        }).withHoverImage(DocAssets.BUTTON_UP_HOVER);
        downButton = new GuiImageButton(bookLeft + LEFT_PAGE_OFFSET + 103, bookBottom - 30, DocAssets.BUTTON_DOWN, (button) -> {
            rowOffset = rowOffset + 1;
            layoutLeftPage();
        }).withHoverImage(DocAssets.BUTTON_DOWN_HOVER);

        addRenderableWidget(upButton);
        addRenderableWidget(downButton);

        addBackButton(previousScreen, b -> {});
        addSaveButton((b) -> saveParticleConfig());
        
        timelineButton = addRenderableWidget(new DocEntryButton(bookLeft + LEFT_PAGE_OFFSET, bookTop + 36, selectedTimeline.getSpellPart().glyphItem.getDefaultInstance(), Component.translatable(selectedTimeline.getSpellPart().getLocaleName()), (b) -> onTimelineSelectorHit()));

        timelineButton.isSelected = true;

        initLeftSideButtons();

        SelectableButton expandButton = new SelectableButton(bookLeft + LEFT_PAGE_OFFSET + 12, bookBottom - 30, DocAssets.EXPAND_ICON, DocAssets.COLLAPSE_ICON, (button) -> {
            allExpanded = !allExpanded;
            if (button instanceof SelectableButton selectableButton) {
                selectableButton.isSelected = allExpanded;
            }
            layoutLeftPage();
        });
        expandButton.withTooltip(Component.translatable("ars_nouveau.expand_button"));
        expandButton.isSelected = allExpanded;

        addRenderableWidget(expandButton);
        
        initHotkeySlots();

        PropertyButton lastClickedButton = propWidgetList.getSelectedButton();
        if (lastClickedButton != null) {
            lastClickedButton.onPress();
        } else {
            addTimelineSelectionWidgets();
        }
    }
    
    private void initHotkeySlots() {
        var caster = com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(staffStack);
        for (int i = 0; i < 10; i++) {
            int beginPhysicalSlot = i * 3 + 0;
            String name = caster.getSpellName(beginPhysicalSlot);
            int slotIndex = i;
            GuiSpellSlot slot = new GuiSpellSlot(bookLeft + 281, bookTop - 1 + 15 * (i + 1), i, name, (b) -> {
                if (!(b instanceof GuiSpellSlot clickedSlot)) {
                    return;
                }
                
                if (hotkeySlot == slotIndex) {
                    return;
                }
                
                for (GuiSpellSlot s : hotkeySlots) {
                    s.isSelected = false;
                }
                clickedSlot.isSelected = true;
                
                hotkeySlot = slotIndex;
                
                var player = net.minecraft.client.Minecraft.getInstance().player;
                var freshStack = player.getItemInHand(hand);
                staffStack = freshStack;
                
                var freshCaster = com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(freshStack);
                int newBeginPhysicalSlot = hotkeySlot * 3 + 0;
                timelineMap = freshCaster.getParticles(newBeginPhysicalSlot).mutable();
                selectedTimeline = findTimelineFromSlot(freshCaster, newBeginPhysicalSlot);
                LAST_SELECTED_PART = selectedTimeline;
                rowOffset = 0;
                
                timelineButton.title = Component.translatable(selectedTimeline.getSpellPart().getLocaleName());
                timelineButton.renderStack = selectedTimeline.getSpellPart().glyphItem.getDefaultInstance();
                
                clearRightPage();
                clearList(leftPageButtons);
                initLeftSideButtons();
                addTimelineSelectionWidgets();
            });

            if (i == hotkeySlot) {
                slot.isSelected = true;
            } else {
                slot.isSelected = false;
            }
            hotkeySlots.add(slot);
            addRenderableWidget(slot);
        }
    }
    
    private void saveParticleConfig() {
        StaffParticleScreen.lastOpenedHash = timelineMap.immutable().hashCode();
        
        com.github.ars_zero.common.network.Networking.sendToServer(
            new com.github.ars_zero.common.network.PacketUpdateStaffParticleTimeline(
                hotkeySlot, 
                timelineMap.immutable(), 
                this.hand == InteractionHand.MAIN_HAND
            )
        );
    }

    public void onDependenciesChanged(PropertyButton propButton) {
        clearList(leftPageButtons);
        
        IParticleTimeline<?> timeline = timelineMap.getOrCreate(selectedTimeline);
        leftPageButtons = buildPropertyButtonTree(timeline.getProperties(), 0);
        
        for (PropertyButton widget : leftPageButtons) {
            addRenderableWidget(widget);
        }
        
        layoutLeftPage();
    }

    public void onTimelineSelectorHit() {
        timelineButton.isSelected = true;
        propWidgetList.resetSelected();
        addTimelineSelectionWidgets();
    }

    public static void openScreen(AbstractMultiPhaseCastDeviceScreen parentScreen, int hotkeySlot, ItemStack stack, InteractionHand stackHand) {
        var caster = com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(stack);
        int beginPhysicalSlot = hotkeySlot * 3 + 0;
        int hash = caster.getParticles(beginPhysicalSlot).hashCode();
        if (LAST_SELECTED_PART == null || StaffParticleScreen.lastOpenedHash != hash || StaffParticleScreen.lastScreen == null) {
            LAST_SELECTED_PART = null;
            StaffParticleScreen.lastOpenedHash = hash;
            Minecraft.getInstance().setScreen(new StaffParticleScreen(parentScreen, hotkeySlot, stack, stackHand));
        } else {
            StaffParticleScreen screen = StaffParticleScreen.lastScreen;
            if (screen.hotkeySlot != hotkeySlot) {
                screen.hotkeySlot = hotkeySlot;
                screen.timelineMap = caster.getParticles(beginPhysicalSlot).mutable();
                screen.selectedTimeline = screen.findTimelineFromSlot(caster, beginPhysicalSlot);
            }
            Minecraft.getInstance().setScreen(screen);
        }
    }


    @Override
    public void onClose() {
        super.onClose();
        StaffParticleScreen.lastScreen = this;
    }

    @Override
    public void removed() {
        super.removed();
        StaffParticleScreen.lastScreen = this;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {

        if (propertyWidgetProvider != null && GuiHelpers.isMouseInRelativeRange((int) pMouseX, (int) pMouseY, propertyWidgetProvider.x,
                propertyWidgetProvider.y, propertyWidgetProvider.width, propertyWidgetProvider.height)) {
            if (propertyWidgetProvider.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY)) {
                return true;
            }
        }
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        if (pScrollY < 0 && hasMoreElements) {
            rowOffset = rowOffset + 1;
            layoutLeftPage();
            manager.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        } else if (pScrollY > 0 && hasPreviousElements) {
            rowOffset = rowOffset - 1;
            layoutLeftPage();
            manager.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }

        return true;
    }

    public void initLeftSideButtons() {
        clearList(leftPageButtons);

        IParticleTimeline<?> timeline = timelineMap.getOrCreate(selectedTimeline);
        propWidgetList.init(timeline.getProperties());
        leftPageButtons = buildPropertyButtonTree(timeline.getProperties(), 0);
        
        for (PropertyButton widget : leftPageButtons) {
            addRenderableWidget(widget);
        }

        layoutLeftPage();
    }
    
    private List<PropertyButton> buildPropertyButtonTree(List<com.hollingsworth.arsnouveau.api.particle.configurations.properties.BaseProperty<?>> props, int depth) {
        List<PropertyButton> buttons = new ArrayList<>();
        if (depth > 3) {
            return buttons;
        }
        
        DocAssets.BlitInfo texture = DocAssets.DOUBLE_NESTED_ENTRY_BUTTON;
        DocAssets.BlitInfo selectedTexture = DocAssets.DOUBLE_NESTED_ENTRY_BUTTON_SELECTED;
        switch (depth) {
            case 0 -> {
                texture = DocAssets.NESTED_ENTRY_BUTTON;
                selectedTexture = DocAssets.NESTED_ENTRY_BUTTON_SELECTED;
            }
            case 1 -> {
                texture = DocAssets.DOUBLE_NESTED_ENTRY_BUTTON;
                selectedTexture = DocAssets.DOUBLE_NESTED_ENTRY_BUTTON_SELECTED;
            }
            case 2 -> {
                texture = DocAssets.TRIPLE_NESTED_ENTRY_BUTTON;
                selectedTexture = DocAssets.TRIPLE_NESTED_ENTRY_BUTTON_SELECTED;
            }
        }
        
        for (var property : props) {
            var widgetProvider = property.buildWidgets(bookLeft + RIGHT_PAGE_OFFSET, bookTop + PAGE_TOP_OFFSET, ONE_PAGE_WIDTH, ONE_PAGE_HEIGHT);
            PropertyButton propertyButton = new PropertyButton(
                bookLeft + LEFT_PAGE_OFFSET + depth * 13, 
                0, 
                texture,
                selectedTexture, 
                property, 
                widgetProvider,
                depth, 
                b -> {
                    PropertyButton pb = (PropertyButton) b;
                    for (PropertyButton button : leftPageButtons) {
                        button.setExpanded(false);
                        button.isSelected = false;
                    }
                    pb.setExpanded(true);
                    pb.isSelected = true;
                    onPropertySelected(pb);
                }
            );
            buttons.add(propertyButton);
            List<PropertyButton> childrenButtons = buildPropertyButtonTree(property.subProperties(), depth + 1);
            propertyButton.setChildren(new ArrayList<>(childrenButtons));
            buttons.addAll(childrenButtons);
        }
        
        for (int i = 0; i < buttons.size(); i++) {
            PropertyButton propButton = buttons.get(i);
            propButton.index = i;
            propButton.property.setChangedListener(() -> {
                propButton.setChildren(buildPropertyButtonTree(propButton.property.subProperties(), propButton.nestLevel + 1));
                onDependenciesChanged(propButton);
            });
        }
        
        return buttons;
    }

    public void layoutLeftPage() {
        List<PropertyButton> allPropButtons = leftPageButtons;
        List<AbstractWidget> expandedWidgets = allPropButtons.stream().filter(widget -> {
            if (!(widget instanceof PropertyButton propertyButton)) {
                return true;
            }
            return allExpanded || propertyButton.isExpanded() || propertyButton.nestLevel == 0;
        }).collect(Collectors.toList());

        if (rowOffset >= expandedWidgets.size()) {
            rowOffset = 0;
        }

        int propIndex = 0;
        for (AbstractWidget widget : allPropButtons) {
            widget.active = false;
            widget.visible = false;
            if (widget instanceof PropertyButton button) {
                button.index = propIndex;
                button.showMarkers = !allExpanded;
                propIndex++;
            }
        }
        List<AbstractWidget> slicedWidgets = expandedWidgets.subList(rowOffset, expandedWidgets.size());
        int LEFT_PAGE_SLICE = 7;
        for (int i = 0; i < Math.min(slicedWidgets.size(), LEFT_PAGE_SLICE); i++) {
            AbstractWidget widget = slicedWidgets.get(i);
            widget.setY(bookTop + 51 + 15 * i);
            widget.active = true;
            widget.visible = true;
        }
        hasMoreElements = rowOffset + LEFT_PAGE_SLICE < expandedWidgets.size();
        hasPreviousElements = rowOffset > 0;

        upButton.visible = hasPreviousElements;
        upButton.active = hasPreviousElements;
        downButton.active = hasMoreElements;
        downButton.visible = hasMoreElements;
    }

    public void onPropertySelected(PropertyButton propertyButton) {
        clearRightPage();
        timelineButton.isSelected = false;
        propertyWidgetProvider = propertyButton.property.buildWidgets(bookLeft + RIGHT_PAGE_OFFSET, bookTop + PAGE_TOP_OFFSET, ONE_PAGE_WIDTH, ONE_PAGE_HEIGHT);

        List<AbstractWidget> propertyWidgets = new ArrayList<>();
        propertyWidgetProvider.addWidgets(propertyWidgets);

        for (AbstractWidget widget : propertyWidgets) {
            addRightPageWidget(widget);
        }
        layoutLeftPage();
    }

    public void addTimelineSelectionWidgets() {
        clearRightPage();
        rightPageWidgets.add(addRenderableWidget(new HeaderWidget(bookLeft + RIGHT_PAGE_OFFSET, bookTop + PAGE_TOP_OFFSET, ONE_PAGE_WIDTH, 20, Component.translatable("ars_nouveau.particle_timelines"))));
        var timelineList = new ArrayList<>(ParticleTimelineRegistry.PARTICLE_TIMELINE_REGISTRY.entrySet());
        timelineList.sort((o1, o2) -> CreativeTabRegistry.COMPARE_SPELL_TYPE_NAME.compare(o1.getValue().getSpellPart(), o2.getValue().getSpellPart()));
        for (int i = 0; i < timelineList.size(); i++) {
            var entry = timelineList.get(i);
            var widget = new GlyphButton(bookLeft + RIGHT_PAGE_OFFSET + 2 + 20 * (i % 6), bookTop + 40 + 20 * (i / 6), entry.getValue().getSpellPart(), (button) -> {
                selectedTimeline = entry.getValue();
                rowOffset = 0;
                LAST_SELECTED_PART = selectedTimeline;
                AbstractSpellPart spellPart = selectedTimeline.getSpellPart();
                timelineButton.title = Component.translatable(spellPart.getLocaleName());
                timelineButton.renderStack = (spellPart.glyphItem.getDefaultInstance());
                clearList(leftPageButtons);
                propWidgetList.resetSelected();
                initLeftSideButtons();
            });
            rightPageWidgets.add(widget);
            addRenderableWidget(widget);
        }
    }

    private void clearRightPage() {
        clearList(rightPageWidgets);
        propertyWidgetProvider = null;
    }

    private void clearList(List<? extends AbstractWidget> list) {
        for (AbstractWidget widget : list) {
            this.removeWidget(widget);
        }
        list.clear();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        DocClientUtils.drawHeader(Component.translatable("ars_nouveau.spell_styles"), graphics, bookLeft + LEFT_PAGE_OFFSET, bookTop + PAGE_TOP_OFFSET, ONE_PAGE_WIDTH, mouseX, mouseY, partialTicks);
        if (propertyWidgetProvider != null) {
            propertyWidgetProvider.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void drawBackgroundElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundElements(graphics, mouseX, mouseY, partialTicks);
        if (propertyWidgetProvider != null) {
            propertyWidgetProvider.renderBg(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (propertyWidgetProvider != null) {
            propertyWidgetProvider.tick();
        }
    }

    public void addRightPageWidget(AbstractWidget widget) {
        rightPageWidgets.add(widget);
        addRenderableWidget(widget);
    }
    
    public void onStaffUpdated(ItemStack updatedStack) {
        this.staffStack = updatedStack;
        int beginPhysicalSlot = hotkeySlot * 3 + 0;
        var caster = com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(updatedStack);
        if (caster != null) {
            this.timelineMap = caster.getParticles(beginPhysicalSlot).mutable();
        }
    }
}

