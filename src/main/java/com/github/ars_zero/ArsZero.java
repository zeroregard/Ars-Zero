package com.github.ars_zero;

import com.github.ars_zero.client.ArsZeroClient;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.interaction.ArcaneCollisionInteraction;
import com.github.ars_zero.common.entity.interaction.FireWaterInteraction;
import com.github.ars_zero.common.entity.interaction.MergeInteraction;
import com.github.ars_zero.common.entity.interaction.BlightFireInteraction;
import com.github.ars_zero.common.entity.interaction.BlightWaterInteraction;
import com.github.ars_zero.common.entity.interaction.VoxelInteractionRegistry;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.event.AnchorEffectEvents;
import com.github.ars_zero.common.event.GravitySuppressionEvents;
import com.github.ars_zero.common.event.ZeroGravityMobEffectEvents;
import com.github.ars_zero.common.event.discount.AirPowerCostReductionEvents;
import com.github.ars_zero.common.event.discount.AnimaPowerCostReductionEvents;
import com.github.ars_zero.common.event.discount.EarthPowerCostReductionEvents;
import com.github.ars_zero.common.event.discount.FirePowerCostReductionEvents;
import com.github.ars_zero.common.event.discount.WaterPowerCostReductionEvents;
import com.github.ars_zero.event.CurioCastingHandler;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.registry.ModAttachments;
import com.github.ars_zero.registry.ModBlockEntities;
import com.github.ars_zero.registry.ModBlocks;
import com.github.ars_zero.registry.ModCreativeTabs;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModFluids;
import com.github.ars_zero.registry.ModItems;
import com.github.ars_zero.registry.ModGlyphs;
import com.github.ars_zero.registry.ModMobEffects;
import com.github.ars_zero.registry.ModParticleTimelines;
import com.github.ars_zero.registry.ModParticles;
import com.github.ars_zero.registry.ModRecipes;
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArsZero.MOD_ID)
public class ArsZero {
    public static final String MOD_ID = "ars_zero";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ArsZero(IEventBus modEventBus, ModContainer modContainer) {
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModGlyphs.registerGlyphs();
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModParticleTimelines.init(modEventBus);

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);

        modEventBus.addListener(Networking::register);
        modEventBus.addListener(this::gatherData);

        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            event.enqueueWork(() -> {
                ModItems.registerSpellCasters();
                registerVoxelInteractions();
                ModParticleTimelines.configureTimelineOptions();
                registerTurretBehaviors();
                registerCauldronInteractions();
            });
        });

        NeoForge.EVENT_BUS.register(WaterPowerCostReductionEvents.class);
        NeoForge.EVENT_BUS.register(FirePowerCostReductionEvents.class);
        NeoForge.EVENT_BUS.register(AirPowerCostReductionEvents.class);
        NeoForge.EVENT_BUS.register(EarthPowerCostReductionEvents.class);
        NeoForge.EVENT_BUS.register(AnimaPowerCostReductionEvents.class);
        NeoForge.EVENT_BUS.register(ZeroGravityMobEffectEvents.class);
        NeoForge.EVENT_BUS.register(GravitySuppressionEvents.class);
        NeoForge.EVENT_BUS.register(CurioCastingHandler.class);
        NeoForge.EVENT_BUS.register(AnchorEffectEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            ArsZeroClient.init(modEventBus);
        }
    }

    private static void registerTurretBehaviors() {
        com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.TURRET_BEHAVIOR_MAP.put(
                com.github.ars_zero.registry.ModGlyphs.NEAR_FORM,
                new com.hollingsworth.arsnouveau.api.spell.ITurretBehavior() {
                    @Override
                    public void onCast(com.hollingsworth.arsnouveau.api.spell.SpellResolver resolver,
                            net.minecraft.server.level.ServerLevel world,
                            net.minecraft.core.BlockPos pos,
                            net.minecraft.world.entity.player.Player fakePlayer,
                            net.minecraft.core.Position iposition,
                            net.minecraft.core.Direction direction) {
                        net.minecraft.core.Direction facingDir = world.getBlockState(pos)
                                .getValue(com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.FACING);
                        double baseDistance = 1.0;
                        double distance = baseDistance + resolver.getCastStats().getAmpMultiplier() * 0.5;
                        net.minecraft.world.phys.Vec3 eyePos = new net.minecraft.world.phys.Vec3(iposition.x(),
                                iposition.y(), iposition.z());
                        net.minecraft.world.phys.Vec3 lookVec = new net.minecraft.world.phys.Vec3(
                                facingDir.getStepX(),
                                facingDir.getStepY(),
                                facingDir.getStepZ()).normalize();
                        net.minecraft.world.phys.Vec3 targetPos = eyePos.add(lookVec.scale(distance));
                        net.minecraft.core.Direction hitDirection = net.minecraft.core.Direction.getNearest(lookVec.x,
                                lookVec.y, lookVec.z);
                        net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(targetPos);
                        net.minecraft.world.phys.BlockHitResult result = new net.minecraft.world.phys.BlockHitResult(
                                targetPos, hitDirection, blockPos, false);
                        resolver.onResolveEffect(world, result);
                    }
                });

        com.hollingsworth.arsnouveau.common.block.BasicSpellTurret.TURRET_BEHAVIOR_MAP.put(
                com.hollingsworth.arsnouveau.common.spell.method.MethodSelf.INSTANCE,
                new com.hollingsworth.arsnouveau.api.spell.ITurretBehavior() {
                    @Override
                    public void onCast(com.hollingsworth.arsnouveau.api.spell.SpellResolver resolver,
                            net.minecraft.server.level.ServerLevel world,
                            net.minecraft.core.BlockPos pos,
                            net.minecraft.world.entity.player.Player fakePlayer,
                            net.minecraft.core.Position iposition,
                            net.minecraft.core.Direction direction) {
                        fakePlayer.setPos(iposition.x(), iposition.y(), iposition.z());
                        resolver.onResolveEffect(world, new net.minecraft.world.phys.EntityHitResult(fakePlayer));
                    }
                });
    }

    private static void registerVoxelInteractions() {
        MergeInteraction mergeInteraction = new MergeInteraction();
        ArcaneCollisionInteraction arcaneInteraction = new ArcaneCollisionInteraction();

        VoxelInteractionRegistry.register(
                FireVoxelEntity.class,
                WaterVoxelEntity.class,
                new FireWaterInteraction());
        VoxelInteractionRegistry.register(
                com.github.ars_zero.common.entity.WindVoxelEntity.class,
                FireVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.WindFireInteraction());
        VoxelInteractionRegistry.register(
                com.github.ars_zero.common.entity.WindVoxelEntity.class,
                WaterVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.WindWaterInteraction());
        VoxelInteractionRegistry.register(
                com.github.ars_zero.common.entity.WindVoxelEntity.class,
                StoneVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.WindStoneInteraction());

        VoxelInteractionRegistry.register(
                FireVoxelEntity.class,
                FireVoxelEntity.class,
                mergeInteraction);

        VoxelInteractionRegistry.register(
                WaterVoxelEntity.class,
                WaterVoxelEntity.class,
                mergeInteraction);

        VoxelInteractionRegistry.register(
                com.github.ars_zero.common.entity.WindVoxelEntity.class,
                com.github.ars_zero.common.entity.WindVoxelEntity.class,
                mergeInteraction);

        VoxelInteractionRegistry.register(
                StoneVoxelEntity.class,
                StoneVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.StoneStoneInteraction());

        VoxelInteractionRegistry.register(
                StoneVoxelEntity.class,
                FireVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.StoneFireInteraction());

        VoxelInteractionRegistry.register(
                StoneVoxelEntity.class,
                WaterVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.StoneWaterInteraction());

        VoxelInteractionRegistry.register(
                ArcaneVoxelEntity.class,
                ArcaneVoxelEntity.class,
                arcaneInteraction);

        VoxelInteractionRegistry.register(
                ArcaneVoxelEntity.class,
                FireVoxelEntity.class,
                arcaneInteraction);

        VoxelInteractionRegistry.register(
                ArcaneVoxelEntity.class,
                WaterVoxelEntity.class,
                arcaneInteraction);

        VoxelInteractionRegistry.register(
                ArcaneVoxelEntity.class,
                StoneVoxelEntity.class,
                arcaneInteraction);

        VoxelInteractionRegistry.register(
                ArcaneVoxelEntity.class,
                IceVoxelEntity.class,
                arcaneInteraction);

        VoxelInteractionRegistry.register(
                IceVoxelEntity.class,
                IceVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.IceIceInteraction());

        VoxelInteractionRegistry.register(
                IceVoxelEntity.class,
                FireVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.IceFireInteraction());

        VoxelInteractionRegistry.register(
                IceVoxelEntity.class,
                WaterVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.IceWaterInteraction());

        VoxelInteractionRegistry.register(
                IceVoxelEntity.class,
                StoneVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.IceStoneInteraction());

        VoxelInteractionRegistry.register(
                com.github.ars_zero.common.entity.WindVoxelEntity.class,
                IceVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.IceWindInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                LightningVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.LightningLightningInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                FireVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.LightningFireInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                WaterVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.LightningWaterInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                StoneVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.LightningStoneInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                IceVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.LightningIceInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                com.github.ars_zero.common.entity.WindVoxelEntity.class,
                new com.github.ars_zero.common.entity.interaction.LightningWindInteraction());

        VoxelInteractionRegistry.register(
                BlightVoxelEntity.class,
                BlightVoxelEntity.class,
                mergeInteraction);

        VoxelInteractionRegistry.register(
                BlightVoxelEntity.class,
                FireVoxelEntity.class,
                new BlightFireInteraction());

        VoxelInteractionRegistry.register(
                BlightVoxelEntity.class,
                WaterVoxelEntity.class,
                new BlightWaterInteraction());

        VoxelInteractionRegistry.register(
                ArcaneVoxelEntity.class,
                LightningVoxelEntity.class,
                arcaneInteraction);

        VoxelInteractionRegistry.register(
                ArcaneVoxelEntity.class,
                BlightVoxelEntity.class,
                arcaneInteraction);
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public void gatherData(net.neoforged.neoforge.data.event.GatherDataEvent event) {
        var generator = event.getGenerator();

        if (event.includeServer()) {
            generator.addProvider(true, new com.github.ars_zero.common.datagen.DyeRecipeDatagen(generator));
            generator.addProvider(true, new com.github.ars_zero.common.datagen.StaffRecipeDatagen(generator));
            generator.addProvider(true, new com.github.ars_zero.common.datagen.GlyphRecipeDatagen(generator));
        }
    }

    private static void registerCauldronInteractions() {
        CauldronInteraction.InteractionMap empty = CauldronInteraction.EMPTY;
        empty.map().put(
                ModFluids.BLIGHT_FLUID_BUCKET.get(),
                (state, level, pos, player, hand, stack) -> {
                    if (!level.isClientSide) {
                        Item item = stack.getItem();
                        player.setItemInHand(hand,
                                ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                        player.awardStat(Stats.FILL_CAULDRON);
                        player.awardStat(Stats.ITEM_USED.get(item));
                        level.setBlockAndUpdate(pos, ModBlocks.BLIGHT_CAULDRON.get().defaultBlockState()
                                .setValue(com.github.ars_zero.common.block.BlightCauldronBlock.LEVEL, 3));
                        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                });
    }
}
