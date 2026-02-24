package com.github.ars_zero.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {

    public static ModConfigSpec SERVER_CONFIG;
    public static ModConfigSpec.BooleanValue ALLOW_NON_OP_ANCHOR_ON_PLAYERS;
    public static ModConfigSpec.IntValue LARGE_EXPLOSION_MAX_BLOCKS_PER_TICK;
    public static ModConfigSpec.BooleanValue ALLOW_BLOCK_GROUP_CREATION;
    public static ModConfigSpec.IntValue DEFAULT_MULTIPHASE_DEVICE_TICK_DELAY;
    /** Weight for blight forest biome when using Terrablender. ~25% of archwood forest when set to 1 and archwood weight is 3. */
    public static ModConfigSpec.IntValue BLIGHT_FOREST_WEIGHT;

    static {
        ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

        SERVER_BUILDER.comment("Anchor Effect Settings").push("anchor_effect");
        ALLOW_NON_OP_ANCHOR_ON_PLAYERS = SERVER_BUILDER.comment(
                "Allow non-OP players to use Anchor effect on other players.",
                "When set to false (default), only OP players can anchor other players.",
                "When set to true, any player can anchor other players.").define("allowNonOpAnchorOnPlayers", false);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.comment("Large Explosion Settings").push("large_explosion");
        LARGE_EXPLOSION_MAX_BLOCKS_PER_TICK = SERVER_BUILDER.comment(
                "Hard per-tick block destruction budget for large explosions.")
                .defineInRange("maxBlocksPerTick", 256, 1, 1000000);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.comment("Select/Anchor Block Group Settings").push("select_anchor");
        ALLOW_BLOCK_GROUP_CREATION = SERVER_BUILDER.comment(
                "EXPERIMENTAL: Allow Select and Anchor effects to create block group entities.",
                "When set to false (default), block group entities cannot be created.",
                "When set to true, Select and Anchor can create block group entities for block translation.",
                "This feature is experimental and may cause performance issues or unexpected behavior.")
                .define("allowBlockGroupCreation", false);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.comment("Multiphase cast device settings").push("multiphase");
        DEFAULT_MULTIPHASE_DEVICE_TICK_DELAY = SERVER_BUILDER.comment(
                "Default tick delay (in ticks) for multiphase device slots. Minimum 1 (20 times per second).")
                .defineInRange("defaultTickDelay", 10, 1, 20);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.comment("Blight forest biome (Terrablender). Set to 0 to disable.").push("blight_forest");
        BLIGHT_FOREST_WEIGHT = SERVER_BUILDER.comment(
                "Region weight for blight forest. Use 1 for ~25%% when Ars Nouveau archwood forest weight is 3.")
                .defineInRange("weight", 1, 0, Integer.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
    }
}
