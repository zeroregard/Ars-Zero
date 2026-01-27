package com.github.ars_zero.client.gui;

import com.github.ars_zero.common.casting.CastingStyle;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketUpdateCastingStyle;
import com.hollingsworth.arsnouveau.api.documentation.DocAssets;
import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.client.gui.HeaderWidget;
import com.hollingsworth.arsnouveau.client.gui.book.BaseBook;
import com.github.ars_zero.client.gui.buttons.CheckboxButton;
import com.github.ars_zero.client.gui.buttons.RadioButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiSpellSlot;
import com.hollingsworth.arsnouveau.client.gui.buttons.ColorPresetButton;
import com.hollingsworth.arsnouveau.client.gui.BookSlider;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CastingStyleScreen extends BaseBook {
    private static final List<String> BORDER_BONES = List.of(
        "alphabet", "circle_big", "circle_small", "circle_big_thick", "triangle_big"
    );
    private static final List<String> SYMBOL_ORDER = List.of(
        "pentagram_big", "pentagram_small", "triangle_small",
        "school_fire", "school_water", "school_earth", "school_air",
        "school_abjuration", "school_anima", "school_conjuration", "school_manipulation"
    );
    private static final String SYMBOL_NONE = "__none__";

    private static final Map<String, String> BONE_NAME_MAP = Map.of(
        "alphabet", "Alphabet",
        "circle_big", "Circle - Outer",
        "circle_small", "Circle - Inner",
        "circle_big_thick", "Circle - Solid",
        "triangle_big", "Triangle",
        "pentagram_small", "Pentagram - Small",
        "pentagram_big", "Pentagram - Big",
        "triangle_small", "Triangle"
    );

    private CastingStyle style;
    private AbstractMultiPhaseCastDeviceScreen previousScreen;
    private int hotkeySlot;
    private ItemStack staffStack;
    private InteractionHand hand;
    private boolean isForCirclet;

    private static final int MAX_PAGE = 1;

    private int currentPage = 0;
    private GuiImageButton leftArrow;
    private GuiImageButton rightArrow;

    private CheckboxButton enabledButton;
    private CheckboxButton symbolAutoButton;
    private List<CheckboxButton> outlineButtons = new ArrayList<>();
    private Button symbolSelectButton;
    private List<ColorPresetButton> colorPresetButtons = new ArrayList<>();
    private BookSlider speedSlider;
    private List<RadioButton> placementButtons = new ArrayList<>();
    private List<GuiSpellSlot> hotkeySlots = new ArrayList<>();
    private Map<Integer, List<HeaderWidget>> headersByPage = new HashMap<>();

    public CastingStyleScreen(AbstractMultiPhaseCastDeviceScreen previousScreen, int hotkeySlot, ItemStack stack, InteractionHand stackHand) {
        super();
        this.previousScreen = previousScreen;
        this.hotkeySlot = hotkeySlot;
        this.staffStack = stack;
        this.hand = stackHand;
        this.isForCirclet = stack != null && stack.getItem() instanceof com.github.ars_zero.common.item.SpellcastingCirclet;
        
        this.style = loadCastingStyle(stack, hotkeySlot);
    }

    private CastingStyle loadCastingStyle(ItemStack stack, int slot) {
        if (stack == null || stack.isEmpty()) {
            return new CastingStyle();
        }
        
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CastingStyle();
        }
        
        CompoundTag tag = customData.copyTag();
        String key = "ars_zero_casting_style_" + slot;
        if (tag.contains(key)) {
            return CastingStyle.load(tag.getCompound(key));
        }
        
        return new CastingStyle();
    }

    @Override
    public void init() {
        super.init();

        addBackButton(previousScreen, b -> {});
        addSaveButton((b) -> saveCastingStyle());

        int nextPageYOffset = bookBottom - 20;
        rightArrow = new GuiImageButton(bookRight - DocAssets.ARROW_RIGHT.width() - 1, nextPageYOffset, DocAssets.ARROW_RIGHT, (b) -> {
            if (currentPage < MAX_PAGE) {
                currentPage++;
                updatePageVisibility();
            }
        });
        leftArrow = new GuiImageButton(bookLeft + 1, nextPageYOffset, DocAssets.ARROW_LEFT, (b) -> {
            if (currentPage > 0) {
                currentPage--;
                updatePageVisibility();
            }
        });
        addRenderableWidget(leftArrow);
        addRenderableWidget(rightArrow);

        initPage0();
        initPage1();
        updatePageVisibility();
        initHotkeySlots();
    }

    private void initPage0() {
        int leftX = bookLeft + LEFT_PAGE_OFFSET;
        int rightX = bookLeft + RIGHT_PAGE_OFFSET;
        int y = bookTop + PAGE_TOP_OFFSET;

        enabledButton = new CheckboxButton(leftX + 10, y + 20, (button) -> {
            style.setEnabled(!style.isEnabled());
            if (button instanceof CheckboxButton checkboxButton) {
                checkboxButton.isSelected = style.isEnabled();
            }
        });
        enabledButton.isSelected = style.isEnabled();
        enabledButton.withTooltip(Component.translatable("ars_zero.gui.casting_style.enabled"));
        addRenderableWidget(enabledButton);

        speedSlider = new BookSlider(leftX + 10, y + 52, 100, 20, Component.empty(), Component.empty(), 0, 1, speedToSlider(style.getSpeed()), 0.05, 2, false, (value) -> {
            style.setSpeed(sliderToSpeed(value.doubleValue()));
        });
        addRenderableWidget(speedSlider);

        HeaderWidget placementHeader = new HeaderWidget(leftX, y + 85, ONE_PAGE_WIDTH, 20, Component.translatable("ars_zero.gui.casting_style.placement"));
        headersByPage.computeIfAbsent(0, k -> new ArrayList<>()).add(placementHeader);
        addRenderableWidget(placementHeader);

        createPlacementButton(leftX + 10, y + 110, CastingStyle.Placement.FEET, Component.translatable("ars_zero.gui.casting_style.placement.feet"));
        createPlacementButton(leftX + 10, y + 130, CastingStyle.Placement.NEAR, Component.translatable("ars_zero.gui.casting_style.placement.near"));

        if (placementButtons.size() >= 2) {
            placementButtons.get(0).isSelected = style.getPlacement() == CastingStyle.Placement.FEET;
            placementButtons.get(1).isSelected = style.getPlacement() == CastingStyle.Placement.NEAR;
        }

        HeaderWidget colorHeader = new HeaderWidget(rightX, y, ONE_PAGE_WIDTH, 20, Component.translatable("ars_zero.gui.casting_style.color"));
        headersByPage.computeIfAbsent(0, k -> new ArrayList<>()).add(colorHeader);
        addRenderableWidget(colorHeader);
        
        int xOffset = rightX + 7;
        int yOffset = y + 18;
        
        int numPerRow = 6;
        int size = ParticleColor.PRESET_COLORS.size();
        int buttonStartY = yOffset + DocAssets.SPELLSTYLE_COLOR_PREVIEW.height() + 5;
        for (int i = 0; i < size; i++) {
            ParticleColor presetColor = ParticleColor.PRESET_COLORS.get(i);
            ColorPresetButton button = new ColorPresetButton(xOffset + (i % numPerRow) * 18, buttonStartY + (i / numPerRow) * 18, presetColor, (b) -> {
                style.setColor(presetColor.getColor());
                updateColorPreview();
            });
            button.selected = presetColor.getColor() == style.getColor();
            colorPresetButtons.add(button);
            addRenderableWidget(button);
        }

    }

    private void initPage1() {
        int leftX = bookLeft + LEFT_PAGE_OFFSET;
        int rightX = bookLeft + RIGHT_PAGE_OFFSET;
        int y = bookTop + PAGE_TOP_OFFSET;

        HeaderWidget outlinesHeader = new HeaderWidget(leftX, y, ONE_PAGE_WIDTH, 20, Component.translatable("ars_zero.gui.casting_style.outlines"));
        headersByPage.computeIfAbsent(1, k -> new ArrayList<>()).add(outlinesHeader);
        addRenderableWidget(outlinesHeader);

        HeaderWidget symbolHeader = new HeaderWidget(rightX, y, ONE_PAGE_WIDTH, 20, Component.translatable("ars_zero.gui.casting_style.symbol"));
        headersByPage.computeIfAbsent(1, k -> new ArrayList<>()).add(symbolHeader);
        addRenderableWidget(symbolHeader);

        symbolAutoButton = new CheckboxButton(rightX + 10, y + 22, (button) -> {
            style.setSymbolAuto(!style.isSymbolAuto());
            if (button instanceof CheckboxButton cb) {
                cb.isSelected = style.isSymbolAuto();
            }
            updateSymbolSelectButtonState();
        });
        symbolAutoButton.isSelected = style.isSymbolAuto();
        symbolAutoButton.withTooltip(Component.translatable("ars_zero.gui.casting_style.symbol_auto.tooltip"));
        addRenderableWidget(symbolAutoButton);

        int outlineY = y + 22;
        for (int i = 0; i < BORDER_BONES.size(); i++) {
            String boneName = BORDER_BONES.get(i);
            int boneYPos = outlineY + i * 20;
            final String finalBoneName = boneName;
            CheckboxButton btn = new CheckboxButton(leftX + 10, boneYPos, (b) -> {
                Set<String> active = new HashSet<>(style.getActiveBones());
                if (active.contains(finalBoneName)) {
                    active.remove(finalBoneName);
                } else {
                    active.add(finalBoneName);
                }
                style.setActiveBones(active);
                if (b instanceof CheckboxButton cb) {
                    cb.isSelected = style.getActiveBones().contains(finalBoneName);
                }
            });
            btn.isSelected = style.getActiveBones().contains(boneName);
            btn.withTooltip(Component.literal(formatBoneName(boneName)));
            outlineButtons.add(btn);
            addRenderableWidget(btn);
        }

        List<String> symbolOptions = new ArrayList<>();
        symbolOptions.add(SYMBOL_NONE);
        symbolOptions.addAll(SYMBOL_ORDER);
        symbolSelectButton = Button.builder(Component.literal(getSymbolButtonLabel()), b -> openSymbolSelect())
            .bounds(rightX + 10, y + 44, ONE_PAGE_WIDTH - 20, 20)
            .build();
        addRenderableWidget(symbolSelectButton);
        updateSymbolSelectButtonState();
    }

    private String getSymbolButtonLabel() {
        String sel = style != null ? style.getSelectedSymbolBone() : null;
        if (sel == null || !SYMBOL_ORDER.contains(sel)) {
            return net.minecraft.client.resources.language.I18n.get("ars_zero.gui.casting_style.symbol.none") + " \u25bc";
        }
        return formatBoneName(sel) + " \u25bc";
    }

    private void openSymbolSelect() {
        List<String> opts = new ArrayList<>();
        opts.add(SYMBOL_NONE);
        opts.addAll(SYMBOL_ORDER);
        Minecraft.getInstance().setScreen(new SymbolSelectScreen(
            this,
            style,
            opts,
            v -> SYMBOL_NONE.equals(v)
                ? Component.translatable("ars_zero.gui.casting_style.symbol.none")
                : Component.literal(formatBoneName(v)),
            (s, v) -> s.setSelectedSymbolBone(SYMBOL_NONE.equals(v) ? null : v)
        ));
    }

    void refreshWidgets() {
        if (enabledButton != null) {
            enabledButton.isSelected = style.isEnabled();
        }
        if (symbolAutoButton != null) {
            symbolAutoButton.isSelected = style.isSymbolAuto();
        }
        for (int i = 0; i < outlineButtons.size() && i < BORDER_BONES.size(); i++) {
            outlineButtons.get(i).isSelected = style.getActiveBones().contains(BORDER_BONES.get(i));
        }
        if (symbolSelectButton != null) {
            symbolSelectButton.setMessage(Component.literal(getSymbolButtonLabel()));
        }
        updateColorPreview();
        if (speedSlider != null && !speedSlider.isFocused()) {
            double currentSliderValue = speedSlider.getValue();
            double expectedSlider = speedToSlider(style.getSpeed());
            if (Math.abs(currentSliderValue - expectedSlider) > 0.02) {
                speedSlider.setValue(expectedSlider);
            }
        }
        if (placementButtons.size() >= 2) {
            placementButtons.get(0).isSelected = style.getPlacement() == CastingStyle.Placement.FEET;
            placementButtons.get(1).isSelected = style.getPlacement() == CastingStyle.Placement.NEAR;
        }
        updatePageVisibility();
    }

    private void updateSymbolSelectButtonState() {
        if (symbolSelectButton == null) {
            return;
        }
        boolean auto = style != null && style.isSymbolAuto();
        symbolSelectButton.visible = isOnPage(1) && !auto;
        symbolSelectButton.active = isOnPage(1) && !auto;
    }

    private void createPlacementButton(int x, int y, CastingStyle.Placement placement, Component tooltip) {
        RadioButton button = new RadioButton(x, y, (b) -> {
            style.setPlacement(placement);
            selectRadioButton(placementButtons, (RadioButton) b);
        });
        button.withTooltip(tooltip);
        placementButtons.add(button);
        addRenderableWidget(button);
    }

    private void selectRadioButton(List<RadioButton> group, RadioButton selected) {
        for (RadioButton button : group) {
            button.isSelected = (button == selected);
        }
    }

    private String formatBoneName(String boneName) {
        if (boneName == null || boneName.isEmpty()) {
            return boneName;
        }

        if (BONE_NAME_MAP.containsKey(boneName)) {
            return BONE_NAME_MAP.get(boneName);
        }

        if (boneName.startsWith("school_")) {
            String schoolName = boneName.substring(7);
            return capitalizeFirst(schoolName);
        }

        if ("six_eyes".equals(boneName)) {
            return "Six Eyes";
        }

        String[] parts = boneName.split("_");
        if (parts.length == 1) {
            return capitalizeFirst(parts[0]);
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(" - ");
            }
            result.append(capitalizeFirst(parts[i]));
        }
        return result.toString();
    }

    private static double speedToSlider(float speed) {
        return (Math.max(0.5f, Math.min(10f, speed)) - 0.5) / 9.5;
    }

    private static float sliderToSpeed(double slider) {
        return (float) (0.5 + Math.max(0, Math.min(1, slider)) * 9.5);
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void updateColorPreview() {
        int currentColor = style.getColor();
        ParticleColor currentParticleColor = ParticleColor.fromInt(currentColor);
        
        for (ColorPresetButton button : colorPresetButtons) {
            button.selected = button.particleColor.getColor() == currentColor;
        }
    }

    private boolean isOnPage(int page) {
        return currentPage == page;
    }

    private void updatePageVisibility() {
        if (enabledButton != null) {
            enabledButton.visible = isOnPage(0);
            enabledButton.active = isOnPage(0);
        }
        if (symbolAutoButton != null) {
            symbolAutoButton.visible = isOnPage(1);
            symbolAutoButton.active = isOnPage(1);
        }
        for (RadioButton button : placementButtons) {
            button.visible = isOnPage(0);
            button.active = isOnPage(0);
        }

        for (ColorPresetButton button : colorPresetButtons) {
            button.visible = isOnPage(0);
            button.active = isOnPage(0);
        }
        if (speedSlider != null) {
            speedSlider.visible = isOnPage(0);
            speedSlider.active = isOnPage(0);
        }

        for (CheckboxButton outlineButton : outlineButtons) {
            outlineButton.visible = isOnPage(1);
            outlineButton.active = isOnPage(1);
        }
        updateSymbolSelectButtonState();

        updateHeaderVisibility();

        leftArrow.visible = currentPage > 0;
        leftArrow.active = currentPage > 0;
        rightArrow.visible = currentPage < MAX_PAGE;
        rightArrow.active = currentPage < MAX_PAGE;
    }

    private void updateHeaderVisibility() {
        for (Map.Entry<Integer, List<HeaderWidget>> entry : headersByPage.entrySet()) {
            int page = entry.getKey();
            boolean shouldShow = isOnPage(page);
            for (HeaderWidget header : entry.getValue()) {
                header.visible = shouldShow;
                header.active = shouldShow;
            }
        }
    }

    private void initHotkeySlots() {
        if (staffStack == null || staffStack.isEmpty()) {
            return;
        }
        
        var caster = com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(staffStack);
        if (caster == null) {
            return;
        }
        
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
                
                var player = Minecraft.getInstance().player;
                if (player == null) {
                    return;
                }
                
                ItemStack freshStack = ItemStack.EMPTY;
                
                if (staffStack != null && !staffStack.isEmpty() && staffStack.getItem() instanceof com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice) {
                    freshStack = staffStack;
                } else if (hand != null) {
                    freshStack = player.getItemInHand(hand);
                    if (!(freshStack.getItem() instanceof com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice)) {
                        freshStack = ItemStack.EMPTY;
                    }
                } else {
                    ItemStack mainStack = player.getMainHandItem();
                    if (mainStack.getItem() instanceof com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice) {
                        freshStack = mainStack;
                    } else {
                        ItemStack offStack = player.getOffhandItem();
                        if (offStack.getItem() instanceof com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice) {
                            freshStack = offStack;
                        }
                    }
                }
                
                if (freshStack.isEmpty() && isForCirclet) {
                    freshStack = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                        .findEquippedCurio(
                            equipped -> equipped.getItem() instanceof com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice,
                            player
                        )
                        .map(result -> result.getRight())
                        .orElse(ItemStack.EMPTY);
                }
                
                if (freshStack.isEmpty() || !(freshStack.getItem() instanceof com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice)) {
                    return;
                }
                
                staffStack = freshStack;
                style = loadCastingStyle(freshStack, hotkeySlot);
                
                refreshWidgets();
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

    private void saveCastingStyle() {
        Networking.sendToServer(new PacketUpdateCastingStyle(hotkeySlot, style, isForCirclet));
    }

    public static void openScreen(AbstractMultiPhaseCastDeviceScreen parentScreen, int hotkeySlot, ItemStack stack, InteractionHand stackHand) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        
        Minecraft.getInstance().setScreen(new CastingStyleScreen(parentScreen, hotkeySlot, stack, stackHand));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (isOnPage(0)) {
            DocClientUtils.drawHeader(Component.translatable("ars_zero.gui.casting_style"), graphics, bookLeft + LEFT_PAGE_OFFSET, bookTop + PAGE_TOP_OFFSET, ONE_PAGE_WIDTH, mouseX, mouseY, partialTicks);
            renderPage0(graphics);
        } else if (isOnPage(1)) {
            renderPage1(graphics);
        }
    }

    private void renderPage0(GuiGraphics graphics) {
        int leftX = bookLeft + LEFT_PAGE_OFFSET;
        int rightX = bookLeft + RIGHT_PAGE_OFFSET;
        int y = bookTop + PAGE_TOP_OFFSET;

        graphics.drawString(Minecraft.getInstance().font, Component.translatable("ars_zero.gui.casting_style.enabled"), leftX + 30, y + 23, 0x808080, false);
        if (speedSlider != null) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("ars_zero.gui.casting_style.speed").getString() + " " + String.format("%.1f", style.getSpeed()), leftX + 30, y + 42, 0x808080, false);
        }
        if (placementButtons.size() >= 2) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("ars_zero.gui.casting_style.placement.feet"), leftX + 30, y + 113, 0x808080, false);
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("ars_zero.gui.casting_style.placement.near"), leftX + 30, y + 133, 0x808080, false);
        }

        int xOffset = rightX + 7;
        int yOffset = y + 18;
        DocClientUtils.blit(graphics, DocAssets.SPELLSTYLE_COLOR_PREVIEW, xOffset, yOffset);
        int currentColor = style.getColor();
        com.hollingsworth.arsnouveau.client.gui.Color color = new com.hollingsworth.arsnouveau.client.gui.Color(currentColor, false);
        int previewX = xOffset + 2;
        int previewY = yOffset + 3;
        int previewWidth = DocAssets.SPELLSTYLE_COLOR_PREVIEW.width() - 4;
        int previewHeight = DocAssets.SPELLSTYLE_COLOR_PREVIEW.height() - 6;
        graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, color.getRGB());
        graphics.fill(previewX + 1, previewY + 1, previewX + previewWidth - 1, previewY + previewHeight - 1, color.getRGB());
    }

    private void renderPage1(GuiGraphics graphics) {
        int leftX = bookLeft + LEFT_PAGE_OFFSET;
        int rightX = bookLeft + RIGHT_PAGE_OFFSET;
        int y = bookTop + PAGE_TOP_OFFSET;
        int outlineY = y + 22;

        for (int i = 0; i < BORDER_BONES.size(); i++) {
            int boneYPos = outlineY + i * 20;
            graphics.drawString(Minecraft.getInstance().font, Component.literal(formatBoneName(BORDER_BONES.get(i))), leftX + 30, boneYPos + 4, 0x808080, false);
        }

        graphics.drawString(Minecraft.getInstance().font, Component.translatable("ars_zero.gui.casting_style.symbol_auto"), rightX + 30, y + 26, 0x808080, false);
        if (style != null && style.isSymbolAuto()) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("ars_zero.gui.casting_style.symbol.auto"), rightX + 30, y + 48, 0x808080, false);
        }
    }
}
