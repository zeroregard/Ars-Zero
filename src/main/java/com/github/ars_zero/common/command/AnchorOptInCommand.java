package com.github.ars_zero.common.command;

import com.github.ars_zero.common.util.AnchorProtectionUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AnchorOptInCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("anchoropt")
            .then(Commands.literal("toggle")
                .executes(context -> toggleOptIn(context, context.getSource().getPlayerOrException()))
            )
            .then(Commands.literal("set")
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(context -> setOptIn(context, context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "value")))
                )
            )
            .then(Commands.literal("status")
                .executes(context -> showStatus(context, context.getSource().getPlayerOrException()))
            )
        );
    }
    
    private static int toggleOptIn(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        boolean currentStatus = AnchorProtectionUtil.isOptedIn(player);
        boolean newStatus = !currentStatus;
        AnchorProtectionUtil.setOptedIn(player, newStatus);
        
        Component message = Component.literal("Anchor opt-in: " + (newStatus ? "ENABLED" : "DISABLED"));
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }
    
    private static int setOptIn(CommandContext<CommandSourceStack> context, ServerPlayer player, boolean value) {
        AnchorProtectionUtil.setOptedIn(player, value);
        
        Component message = Component.literal("Anchor opt-in: " + (value ? "ENABLED" : "DISABLED"));
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        boolean isOptedIn = AnchorProtectionUtil.isOptedIn(player);
        Component message = Component.literal("Anchor opt-in status: " + (isOptedIn ? "ENABLED" : "DISABLED"));
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }
}

