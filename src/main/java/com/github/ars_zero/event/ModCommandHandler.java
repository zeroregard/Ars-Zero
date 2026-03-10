package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ArsZero.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModCommandHandler {

    private static final DynamicCommandExceptionType ERROR_NO_STRUCTURE =
        new DynamicCommandExceptionType(id -> Component.literal("No structure '" + id + "' found within 100 chunks."));

    private static final DynamicCommandExceptionType ERROR_INVALID_BIOME =
        new DynamicCommandExceptionType(id -> Component.literal("Unknown biome: " + id));

    private static final DynamicCommandExceptionType ERROR_NO_BIOME =
        new DynamicCommandExceptionType(id -> Component.literal("No biome '" + id + "' found within 6400 blocks."));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("tplocate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("structure")
                    .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                        .executes(ctx -> locateStructure(
                            ctx.getSource(),
                            ResourceKeyArgument.getStructure(ctx, "structure")
                        ))
                    )
                )
                .then(Commands.literal("biome")
                    .then(Commands.argument("biome", ResourceKeyArgument.key(Registries.BIOME))
                        .executes(ctx -> locateBiome(ctx.getSource(), ctx))
                    )
                )
        );
    }

    private static int locateStructure(CommandSourceStack source, Holder.Reference<Structure> structureHolder)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        String id = structureHolder.key().location().toString();

        source.sendSuccess(() -> Component.literal("Searching for structure " + id + "..."), false);

        Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
            .findNearestMapStructure(level, HolderSet.direct(structureHolder), player.blockPosition(), 100, false);

        if (result == null) throw ERROR_NO_STRUCTURE.create(id);

        BlockPos found = result.getFirst();
        player.teleportTo(found.getX() + 0.5, found.getY(), found.getZ() + 0.5);
        source.sendSuccess(() -> Component.literal(
            "Found " + id + " at " + found.toShortString() + ". Teleporting!"
        ), false);
        return 1;
    }

    private static int locateBiome(CommandSourceStack source,
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        ResourceKey<?> rawKey = ctx.getArgument("biome", ResourceKey.class);
        @SuppressWarnings("unchecked")
        ResourceKey<Biome> biomeKey = (ResourceKey<Biome>) rawKey.cast(Registries.BIOME)
            .orElseThrow(() -> ERROR_INVALID_BIOME.create(rawKey.location()));

        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolder(biomeKey)
            .orElseThrow(() -> ERROR_INVALID_BIOME.create(biomeKey.location()));

        String id = biomeKey.location().toString();
        source.sendSuccess(() -> Component.literal("Searching for biome " + id + "..."), false);

        Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(
            h -> h.is(biomeKey), player.blockPosition(), 6400, 32, 64
        );

        if (result == null) throw ERROR_NO_BIOME.create(id);

        BlockPos found = result.getFirst();
        player.teleportTo(found.getX() + 0.5, found.getY(), found.getZ() + 0.5);
        source.sendSuccess(() -> Component.literal(
            "Found " + id + " at " + found.toShortString() + ". Teleporting!"
        ), false);
        return 1;
    }
}
