package com.github.ars_zero.setup.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {

    public static ModConfigSpec SERVER_CONFIG;
    public static ModConfigSpec.BooleanValue COMPRESSION_BYPASS_DAMAGE_COOLDOWN;

    static {
        ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
        
        SERVER_BUILDER.comment("Compression Mechanics").push("compression");
        COMPRESSION_BYPASS_DAMAGE_COOLDOWN = SERVER_BUILDER
                .comment("When enabled, compressed voxels at 60%+ compression ignore entity damage cooldown (invulnerability frames), allowing rapid-fire damage")
                .define("bypassDamageCooldown", true);
        SERVER_BUILDER.pop();
        
        SERVER_CONFIG = SERVER_BUILDER.build();
    }
}

