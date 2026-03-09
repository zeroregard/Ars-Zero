package com.github.ars_zero.common.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * Ossuary beacon — anchor point for necromancers. No block entity or tick logic;
 * the necromancer's AI goal detects this block within range to anchor its ritual behaviour.
 */
public class OssuaryBeaconBlock extends Block {

    public OssuaryBeaconBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.5f, 6.0f)
                .pushReaction(PushReaction.BLOCK)
                .requiresCorrectToolForDrops());
    }
}
