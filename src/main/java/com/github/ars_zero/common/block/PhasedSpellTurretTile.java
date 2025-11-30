package com.github.ars_zero.common.block;

import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.registry.ModBlockEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.common.block.tile.BasicSpellTurretTile;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class PhasedSpellTurretTile extends BasicSpellTurretTile {

    private static final String BEGIN_TAG = "begin_spell";
    private static final String TICK_TAG = "tick_spell";
    private static final String END_TAG = "end_spell";
    private static final int HISTORY_LIMIT = 32;

    private Spell beginSpell = new Spell();
    private Spell tickSpell = new Spell();
    private Spell endSpell = new Spell();

    private boolean casting;
    private int tickCooldown;
    private int tickIntervalCounter;

    private final Deque<PhaseExecution> phaseHistory = new ArrayDeque<>();

    public PhasedSpellTurretTile(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHASED_SPELL_TURRET.get(), pos, state);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean powered = hasRedstoneSignal();
        if (powered) {
            if (!casting) {
                startCasting();
            } else {
                handleTickCasting();
            }
        } else if (casting) {
            finishCasting();
        }
    }

    public void configureSpells(Spell begin, Spell tick, Spell end, UUID owner) {
        beginSpell = sanitizeSpell(begin);
        tickSpell = sanitizeSpell(tick);
        endSpell = sanitizeSpell(end);
        casting = false;
        tickIntervalCounter = 0;
        tickCooldown = calculateTickCooldown(tickSpell);
        phaseHistory.clear();
        setPlayer(owner);
        setChanged();
        updateBlock();
    }

    public List<PhaseExecution> getPhaseHistory() {
        return List.copyOf(phaseHistory);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        saveSpell(tag, BEGIN_TAG, beginSpell);
        saveSpell(tag, TICK_TAG, tickSpell);
        saveSpell(tag, END_TAG, endSpell);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        beginSpell = loadSpell(tag, BEGIN_TAG);
        tickSpell = loadSpell(tag, TICK_TAG);
        endSpell = loadSpell(tag, END_TAG);
        casting = false;
        tickIntervalCounter = 0;
        tickCooldown = calculateTickCooldown(tickSpell);
        phaseHistory.clear();
    }

    private void startCasting() {
        casting = true;
        tickIntervalCounter = 0;
        tickCooldown = calculateTickCooldown(tickSpell);
        castPhase(AbstractMultiPhaseCastDevice.Phase.BEGIN);
    }

    private void handleTickCasting() {
        if (tickSpell == null || tickSpell.isEmpty()) {
            return;
        }
        if (tickIntervalCounter > 0) {
            tickIntervalCounter--;
            return;
        }
        castPhase(AbstractMultiPhaseCastDevice.Phase.TICK);
        tickIntervalCounter = tickCooldown;
    }

    private void finishCasting() {
        casting = false;
        tickIntervalCounter = 0;
        castPhase(AbstractMultiPhaseCastDevice.Phase.END);
    }

    private void castPhase(AbstractMultiPhaseCastDevice.Phase phase) {
        Spell spell = switch (phase) {
            case BEGIN -> beginSpell;
            case TICK -> tickSpell;
            case END -> endSpell;
        };
        if (spell == null || spell.isEmpty()) {
            return;
        }
        spellCaster = (SpellCaster) spellCaster.setSpell(spell, 0);
        super.shootSpell();
        recordPhase(phase, spell);
    }

    private void recordPhase(AbstractMultiPhaseCastDevice.Phase phase, Spell spell) {
        if (phaseHistory.size() >= HISTORY_LIMIT) {
            phaseHistory.removeFirst();
        }
        long gameTime = level != null ? level.getGameTime() : 0L;
        String name = spell.name();
        if (name == null || name.isBlank()) {
            name = phase.name();
        }
        phaseHistory.addLast(new PhaseExecution(phase, name, gameTime));
    }

    private boolean hasRedstoneSignal() {
        if (level == null) {
            return false;
        }
        BlockPos above = worldPosition.above();
        return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(above);
    }

    private static Spell sanitizeSpell(Spell spell) {
        if (spell == null || spell.isEmpty()) {
            return new Spell();
        }
        String json = spell.toJson();
        return json.isEmpty() ? new Spell() : Spell.fromJson(json);
    }

    private static void saveSpell(CompoundTag tag, String key, Spell spell) {
        if (spell == null || spell.isEmpty()) {
            tag.remove(key);
            return;
        }
        Tag data = ANCodecs.encode(Spell.CODEC.codec(), spell);
        if (data != null) {
            tag.put(key, data);
        }
    }

    private static Spell loadSpell(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return new Spell();
        }
        Tag data = tag.get(key);
        Spell loaded = (Spell) ANCodecs.decode(Spell.CODEC.codec(), data);
        return loaded == null ? new Spell() : loaded;
    }

    private static int calculateTickCooldown(Spell spell) {
        if (spell == null || spell.isEmpty()) {
            return 0;
        }
        List<AbstractSpellPart> recipe = new ArrayList<>();
        for (AbstractSpellPart part : spell.recipe()) {
            recipe.add(part);
        }
        if (recipe.isEmpty()) {
            return 0;
        }
        int totalCooldown = 0;
        int index = recipe.size() - 1;
        ResourceLocation delay = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_delay");
        ResourceLocation extend = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_extend_time");
        ResourceLocation durationDown = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_duration_down");
        while (index >= 0) {
            AbstractSpellPart part = recipe.get(index);
            ResourceLocation id = part.getRegistryName();
            if (delay.equals(id)) {
                int extendCount = 0;
                int durationDownCount = 0;
                int j = index + 1;
                while (j < recipe.size()) {
                    AbstractSpellPart augment = recipe.get(j);
                    ResourceLocation augmentId = augment.getRegistryName();
                    if (extend.equals(augmentId)) {
                        extendCount++;
                        j++;
                    } else if (durationDown.equals(augmentId)) {
                        durationDownCount++;
                        j++;
                    } else {
                        break;
                    }
                }
                int delayCooldown = Math.max(0, 1 + extendCount - durationDownCount);
                totalCooldown += delayCooldown;
                index--;
            } else if (extend.equals(id) || durationDown.equals(id)) {
                index--;
            } else {
                break;
            }
        }
        return Math.max(0, totalCooldown);
    }

    public record PhaseExecution(AbstractMultiPhaseCastDevice.Phase phase, String spellName, long gameTime) {
    }
}
