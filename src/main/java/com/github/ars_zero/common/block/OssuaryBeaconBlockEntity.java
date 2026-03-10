package com.github.ars_zero.common.block;

import com.github.ars_zero.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks up to 2 necromancers actively ritualising at this beacon.
 * Persisted in NBT so the slot count survives server restarts.
 */
public class OssuaryBeaconBlockEntity extends BlockEntity {

    private static final int MAX_RITUALISTS = 2;
    private final Set<UUID> ritualists = new HashSet<>();

    public OssuaryBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OSSUARY_BEACON.get(), pos, state);
    }

    /**
     * Attempt to register a necromancer as a user of this beacon.
     * @return true if the mob is now registered (either it was already, or a slot opened).
     */
    public boolean tryRegister(UUID uuid) {
        if (ritualists.contains(uuid)) return true;
        if (ritualists.size() >= MAX_RITUALISTS) return false;
        ritualists.add(uuid);
        setChanged();
        return true;
    }

    public void unregister(UUID uuid) {
        if (ritualists.remove(uuid)) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (UUID id : ritualists) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            list.add(entry);
        }
        tag.put("Ritualists", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ritualists.clear();
        ListTag list = tag.getList("Ritualists", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ritualists.add(list.getCompound(i).getUUID("id"));
        }
    }
}
