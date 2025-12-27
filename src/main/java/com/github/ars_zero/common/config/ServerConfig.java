package com.github.ars_zero.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    
    public static ModConfigSpec SERVER_CONFIG;
    public static ModConfigSpec.BooleanValue ALLOW_NON_OP_ANCHOR_ON_PLAYERS;
    public static ModConfigSpec.BooleanValue ALLOW_BLOCK_GROUP_CREATION;
    
    static {
        ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
        
        SERVER_BUILDER.comment("Anchor Effect Settings").push("anchor_effect");
        ALLOW_NON_OP_ANCHOR_ON_PLAYERS = SERVER_BUILDER.comment(
                "Allow non-OP players to use Anchor effect on other players.",
                "When set to false (default), only OP players can anchor other players.",
                "When set to true, any player can anchor other players."
        ).define("allowNonOpAnchorOnPlayers", false);
        SERVER_BUILDER.pop();
        
        SERVER_BUILDER.comment("Select/Anchor Block Group Settings").push("select_anchor");
        ALLOW_BLOCK_GROUP_CREATION = SERVER_BUILDER.comment(
                "EXPERIMENTAL: Allow Select and Anchor effects to create block group entities.",
                "When set to false (default), block group entities cannot be created.",
                "When set to true, Select and Anchor can create block group entities for block translation.",
                "This feature is experimental and may cause performance issues or unexpected behavior."
        ).define("allowBlockGroupCreation", false);
        SERVER_BUILDER.pop();
        
        SERVER_CONFIG = SERVER_BUILDER.build();
    }
}

