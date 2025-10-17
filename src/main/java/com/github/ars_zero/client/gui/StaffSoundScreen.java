package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.network.PacketSetStaffSound;
import com.hollingsworth.arsnouveau.api.registry.SpellSoundRegistry;
import com.hollingsworth.arsnouveau.api.sound.ConfiguredSpellSound;
import com.hollingsworth.arsnouveau.api.sound.SpellSound;
import com.hollingsworth.arsnouveau.client.gui.BookSlider;
import com.hollingsworth.arsnouveau.client.gui.book.BaseBook;
import net.minecraft.client.gui.screens.Screen;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.SoundButton;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.setup.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

public class StaffSoundScreen extends BaseBook {

    public enum SoundPhase {
        BEGIN, TICK, END
    }

    public InteractionHand stackHand;
    public SoundPhase currentPhase = SoundPhase.BEGIN;

    public ConfiguredSpellSound beginSound = ConfiguredSpellSound.EMPTY;
    public ConfiguredSpellSound tickSound = ConfiguredSpellSound.EMPTY;
    public ConfiguredSpellSound endSound = ConfiguredSpellSound.EMPTY;

    public ResourceLocation tickLoopingSoundId = null;

    public BookSlider volumeSlider;
    public BookSlider pitchSlider;

    public double volume;
    public double pitch;
    public SpellSound selectedSound;
    public SoundButton selectedButton;
    public Button selectedLoopButton;

    public Button beginPhaseButton;
    public Button tickPhaseButton;
    public Button endPhaseButton;

    private List<LoopingSoundInfo> loopingSounds = new ArrayList<>();
    private Screen parentScreen;

    public StaffSoundScreen(ConfiguredSpellSound beginSound, ConfiguredSpellSound tickSound, ConfiguredSpellSound endSound, ResourceLocation tickLoopingSound, InteractionHand stackHand, Screen parent) {
        super();
        this.beginSound = beginSound;
        this.tickSound = tickSound;
        this.endSound = endSound;
        this.tickLoopingSoundId = tickLoopingSound;
        this.stackHand = stackHand;
        this.parentScreen = parent;
        
        selectPhase(SoundPhase.BEGIN);
        
        initLoopingSounds();
    }

    private void initLoopingSounds() {
        loopingSounds.add(new LoopingSoundInfo(SoundEvents.FIRE_AMBIENT.getLocation(), "Fire"));
        loopingSounds.add(new LoopingSoundInfo(SoundEvents.LAVA_AMBIENT.getLocation(), "Lava"));
        loopingSounds.add(new LoopingSoundInfo(SoundEvents.PORTAL_AMBIENT.getLocation(), "Portal"));
        loopingSounds.add(new LoopingSoundInfo(SoundEvents.BEACON_AMBIENT.getLocation(), "Beacon"));
        loopingSounds.add(new LoopingSoundInfo(SoundEvents.CONDUIT_AMBIENT.getLocation(), "Conduit"));
    }

    private void selectPhase(SoundPhase phase) {
        this.currentPhase = phase;
        
        switch (phase) {
            case BEGIN -> {
                this.selectedSound = beginSound.getSound();
                this.volume = beginSound.getVolume() * 100;
                this.pitch = beginSound.getPitch() * 100;
            }
            case TICK -> {
                if (tickLoopingSoundId != null) {
                    this.selectedSound = null;
                } else {
                    this.selectedSound = tickSound.getSound();
                }
                this.volume = tickSound.getVolume() * 100;
                this.pitch = tickSound.getPitch() * 100;
            }
            case END -> {
                this.selectedSound = endSound.getSound();
                this.volume = endSound.getVolume() * 100;
                this.pitch = endSound.getPitch() * 100;
            }
        }
        
        if (volumeSlider != null) {
            volumeSlider.setValue(volume);
            pitchSlider.setValue(pitch);
        }
        
        if (selectedButton != null) {
            selectedButton.sound = selectedSound != null ? selectedSound : SoundRegistry.EMPTY_SPELL_SOUND;
        }
    }

    @Override
    public void init() {
        super.init();

        volumeSlider = buildSlider(bookLeft + 28, bookTop + 89, Component.translatable("ars_nouveau.sounds.volume"), Component.empty(), volume);
        pitchSlider = buildSlider(bookLeft + 28, bookTop + 129, Component.translatable("ars_nouveau.sounds.pitch"), Component.empty(), pitch);

        addRenderableWidget(volumeSlider);
        addRenderableWidget(pitchSlider);
        
        // Back button
        addRenderableWidget(new GuiImageButton(bookLeft - 15, bookTop + 22, 0, 0, 16, 16, 16, 16, "textures/gui/clear_icon.png", (b) -> {
            Minecraft.getInstance().setScreen(parentScreen);
        }));
        
        // Save and Test buttons positioned properly
        addRenderableWidget(new GuiImageButton(bookLeft + 25, bookBottom - 25, 0, 0, 37, 12, 37, 12, "textures/gui/save_icon.png", this::onSaveClick));
        GuiImageButton testButton = new GuiImageButton(bookLeft + 90, bookBottom - 25, 0, 0, 37, 12, 37, 12, "textures/gui/sound_test_icon.png", this::onTestClick);
        testButton.soundDisabled = true;
        addRenderableWidget(testButton);

        selectedButton = new SoundButton(bookLeft + 69, bookTop + 171, selectedSound != null ? selectedSound : SoundRegistry.EMPTY_SPELL_SOUND, (b) -> {
            if (currentPhase != SoundPhase.TICK) {
                ((SoundButton) b).sound = SoundRegistry.EMPTY_SPELL_SOUND;
                selectedSound = SoundRegistry.EMPTY_SPELL_SOUND;
            }
        });
        addRenderableWidget(selectedButton);

        addPhaseButtons();
        addSoundSelectors();
    }

    private void addPhaseButtons() {
        int buttonY = bookTop + 22;
        int buttonSpacing = 25;

        beginPhaseButton = Button.builder(Component.literal("B"), (b) -> {
            selectPhase(SoundPhase.BEGIN);
            updatePhaseButtons();
            rebuildWidgets();
        }).bounds(bookLeft + 20, buttonY, 20, 20).build();
        beginPhaseButton.setTooltip(Tooltip.create(Component.literal("Select BEGIN phase sound")));

        tickPhaseButton = Button.builder(Component.literal("T"), (b) -> {
            selectPhase(SoundPhase.TICK);
            updatePhaseButtons();
            rebuildWidgets();
        }).bounds(bookLeft + 20 + buttonSpacing, buttonY, 20, 20).build();
        tickPhaseButton.setTooltip(Tooltip.create(Component.literal("Select TICK phase sound (looping)")));

        endPhaseButton = Button.builder(Component.literal("E"), (b) -> {
            selectPhase(SoundPhase.END);
            updatePhaseButtons();
            rebuildWidgets();
        }).bounds(bookLeft + 20 + buttonSpacing * 2, buttonY, 20, 20).build();
        endPhaseButton.setTooltip(Tooltip.create(Component.literal("Select END phase sound")));

        addRenderableWidget(beginPhaseButton);
        addRenderableWidget(tickPhaseButton);
        addRenderableWidget(endPhaseButton);

        updatePhaseButtons();
    }

    private void updatePhaseButtons() {
        beginPhaseButton.active = currentPhase != SoundPhase.BEGIN;
        tickPhaseButton.active = currentPhase != SoundPhase.TICK;
        endPhaseButton.active = currentPhase != SoundPhase.END;
    }

    private void addSoundSelectors() {
        if (currentPhase == SoundPhase.TICK) {
            addLoopingSoundButtons();
        } else {
            addSpellSoundButtons();
        }
    }

    private void addLoopingSoundButtons() {
        int xStart = bookLeft + 154;
        int yStart = bookTop + 52;
        int buttonWidth = 80;
        int buttonHeight = 20;
        int spacing = 2;

        for (int i = 0; i < loopingSounds.size(); i++) {
            LoopingSoundInfo soundInfo = loopingSounds.get(i);
            int yPos = yStart + i * (buttonHeight + spacing);

            Button soundButton = Button.builder(Component.literal(soundInfo.name), (b) -> {
                tickLoopingSoundId = soundInfo.soundId;
                selectedSound = null;
                if (selectedLoopButton != null) {
                    selectedLoopButton.active = true;
                }
                selectedLoopButton = b;
                b.active = false;
            }).bounds(xStart, yPos, buttonWidth, buttonHeight).build();

            if (tickLoopingSoundId != null && tickLoopingSoundId.equals(soundInfo.soundId)) {
                soundButton.active = false;
                selectedLoopButton = soundButton;
            }

            addRenderableWidget(soundButton);
        }
    }

    private void addSpellSoundButtons() {
        final int PER_ROW = 4;
        final int MAX_ROWS = 4;
        int xStart = bookLeft + 154;
        int yStart = bookTop + 52;
        List<SpellSound> sounds = SpellSoundRegistry.getSpellSounds();
        
        // Limit to first 16 sounds to prevent overflow
        int maxSounds = Math.min(sounds.size(), 16);
        
        for (int i = 0; i < maxSounds; i++) {
            SpellSound part = sounds.get(i);
            int row = i / PER_ROW;
            int col = i % PER_ROW;
            
            if (row >= MAX_ROWS) break;
            
            int xOffset = col * 20;
            int yPlace = row * 18 + yStart;

            SoundButton cell = new SoundButton(xStart + xOffset, yPlace, part, this::onSoundClick);
            addRenderableWidget(cell);
        }
    }

    public void onSoundClick(Button button) {
        if (button instanceof SoundButton soundButton && currentPhase != SoundPhase.TICK) {
            selectedSound = soundButton.sound;
            selectedButton.sound = selectedSound;
            tickLoopingSoundId = null;
        }
    }

    public void onTestClick(Button button) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        
        BlockPos pos = localPlayer.getOnPos().above(2);
        float vol = (float) volumeSlider.getValue() / 100f;
        float pit = (float) pitchSlider.getValue() / 100f;

        if (currentPhase == SoundPhase.TICK && tickLoopingSoundId != null) {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(tickLoopingSoundId);
            localPlayer.level().playLocalSound(pos.getX(), pos.getY(), pos.getZ(), soundEvent, SoundSource.PLAYERS, vol, pit, false);
        } else if (selectedSound != null && selectedSound != SoundRegistry.EMPTY_SPELL_SOUND) {
            localPlayer.level().playLocalSound(pos.getX(), pos.getY(), pos.getZ(), selectedSound.getSoundEvent().value(), SoundSource.PLAYERS, vol, pit, false);
        }
    }

    public void onSaveClick(Button button) {
        float vol = (float) volumeSlider.getValue() / 100f;
        float pit = (float) pitchSlider.getValue() / 100f;

        switch (currentPhase) {
            case BEGIN -> beginSound = selectedSound == null || selectedSound == SoundRegistry.EMPTY_SPELL_SOUND 
                ? ConfiguredSpellSound.EMPTY 
                : new ConfiguredSpellSound(selectedSound, vol, pit);
            case TICK -> {
                if (tickLoopingSoundId != null) {
                    tickSound = new ConfiguredSpellSound(SoundRegistry.EMPTY_SPELL_SOUND, vol, pit);
                } else {
                    tickSound = selectedSound == null || selectedSound == SoundRegistry.EMPTY_SPELL_SOUND 
                        ? ConfiguredSpellSound.EMPTY 
                        : new ConfiguredSpellSound(selectedSound, vol, pit);
                }
            }
            case END -> endSound = selectedSound == null || selectedSound == SoundRegistry.EMPTY_SPELL_SOUND 
                ? ConfiguredSpellSound.EMPTY 
                : new ConfiguredSpellSound(selectedSound, vol, pit);
        }

        com.github.ars_zero.common.network.Networking.sendToServer(
            new PacketSetStaffSound(beginSound, tickSound, endSound, tickLoopingSoundId, stackHand == InteractionHand.MAIN_HAND)
        );
    }

    @Override
    public void drawBackgroundElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundElements(graphics, mouseX, mouseY, partialTicks);
        int color = -8355712;
        
        graphics.drawString(font, Component.literal("Staff Sound Configuration").getString(), bookLeft + 51, bookTop + 8, color, false);
        
        String phaseText = switch (currentPhase) {
            case BEGIN -> "BEGIN Phase (Single Fire)";
            case TICK -> "TICK Phase (Looping)";
            case END -> "END Phase (Single Fire)";
        };
        graphics.drawString(font, phaseText, bookLeft + 25, bookTop + 50, color, false);
        
        graphics.drawString(font, "Save", bookLeft + 37, bookBottom - 18, color, false);
        graphics.drawString(font, "Test", bookLeft + 102, bookBottom - 18, color, false);

        if (currentPhase == SoundPhase.TICK) {
            graphics.drawString(font, "Looping Sounds:", bookLeft + 154, bookTop + 40, color, false);
        } else {
            graphics.drawString(font, "Spell Sounds:", bookLeft + 154, bookTop + 40, color, false);
        }
    }

    private static class LoopingSoundInfo {
        public final ResourceLocation soundId;
        public final String name;

        public LoopingSoundInfo(ResourceLocation soundId, String name) {
            this.soundId = soundId;
            this.name = name;
        }
    }
}

