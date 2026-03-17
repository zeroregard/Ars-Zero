package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Manually-placed room_connector piece that bridges the bottom of the staircase
 * to the jigsaw dungeon. Always placed exactly once at dungeonY.
 */
public class NecropolisConnectorPiece extends TemplateStructurePiece {

    public static final ResourceLocation TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath("ars_zero", "necropolis/room_connector");

    public NecropolisConnectorPiece(StructureTemplateManager mgr, BlockPos pos) {
        super(
                ModWorldgen.NECROPOLIS_CONNECTOR_PIECE.get(),
                0,
                mgr,
                TEMPLATE_ID,
                TEMPLATE_ID.toString(),
                makeSettings(),
                pos
        );
    }

    public NecropolisConnectorPiece(StructureTemplateManager mgr, CompoundTag tag) {
        super(ModWorldgen.NECROPOLIS_CONNECTOR_PIECE.get(), tag, mgr, loc -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings().setRotation(Rotation.NONE).setIgnoreEntities(false);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        // No data markers
    }
}
