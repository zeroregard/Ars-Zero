package com.github.ars_zero;

import com.github.ars_zero.client.ArsZeroClient;
import com.github.ars_zero.common.block.BlightCauldronBlock;
import com.github.ars_zero.common.datagen.DyeRecipeDatagen;
import com.github.ars_zero.common.datagen.GlyphRecipeDatagen;
import com.github.ars_zero.common.datagen.StaffRecipeDatagen;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.github.ars_zero.common.entity.interaction.ArcaneCollisionInteraction;
import com.github.ars_zero.common.entity.interaction.FireWaterInteraction;
import com.github.ars_zero.common.entity.interaction.IceFireInteraction;
import com.github.ars_zero.common.entity.interaction.IceIceInteraction;
import com.github.ars_zero.common.entity.interaction.IceStoneInteraction;
import com.github.ars_zero.common.entity.interaction.IceWaterInteraction;
import com.github.ars_zero.common.entity.interaction.IceWindInteraction;
import com.github.ars_zero.common.entity.interaction.LightningFireInteraction;
import com.github.ars_zero.common.entity.interaction.LightningIceInteraction;
import com.github.ars_zero.common.entity.interaction.LightningLightningInteraction;
import com.github.ars_zero.common.entity.interaction.LightningStoneInteraction;
import com.github.ars_zero.common.entity.interaction.LightningWaterInteraction;
import com.github.ars_zero.common.entity.interaction.LightningWindInteraction;
import com.github.ars_zero.common.entity.interaction.MergeInteraction;
import com.github.ars_zero.common.entity.interaction.BlightFireInteraction;
import com.github.ars_zero.common.entity.interaction.BlightWaterInteraction;
import com.github.ars_zero.common.entity.interaction.StoneFireInteraction;
import com.github.ars_zero.common.entity.interaction.StoneStoneInteraction;
import com.github.ars_zero.common.entity.interaction.StoneWaterInteraction;
import com.github.ars_zero.common.entity.interaction.VoxelInteractionRegistry;
import com.github.ars_zero.common.entity.interaction.WindFireInteraction;
import com.github.ars_zero.common.entity.interaction.WindStoneInteraction;
import com.github.ars_zero.common.entity.interaction.WindWaterInteraction;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.event.AnchorEffectEvents;
import com.github.ars_zero.common.event.GravitySuppressionEvents;
import com.github.ars_zero.common.event.WitherImmunityEffectEvents;
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
import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.common.capability.ManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.api.loot.DungeonLootTables;
import com.hollingsworth.arsnouveau.api.spell.ITurretBehavior;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.phys.BlockHitResult;
import com.alexthw.sauce.registry.ModRegistry;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
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
        modEventBus.addListener(ArsZero::onEntityAttributeCreation);
        modEventBus.addListener(ArsZero::onRegisterSpawnPlacements);
        modEventBus.addListener(ArsZero::registerCapabilities);

        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            event.enqueueWork(() -> {
                ModItems.registerSpellCasters();
                registerVoxelInteractions();
                ModParticleTimelines.configureTimelineOptions();
                registerTurretBehaviors();
                registerCauldronInteractions();
                registerArsNouveauDungeonLoot();
                ArsNouveauAPI.getInstance().getEnchantingRecipeTypes().add(ModRecipes.PROTECTION_UPGRADE_TYPE.get());
                ModGlyphs.addOptionalAugmentCompatibility();
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
        NeoForge.EVENT_BUS.register(WitherImmunityEffectEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            ArsZeroClient.init(modEventBus);
        }
    }

    private static void registerArsNouveauDungeonLoot() {
        DungeonLootTables.RARE_LOOT.add(() -> new ItemStack(ModItems.STAFF_TELEKINESIS.get(), 1));
    }

    private static void registerTurretBehaviors() {
        BasicSpellTurret.TURRET_BEHAVIOR_MAP.put(
                ModGlyphs.NEAR_FORM,
                new ITurretBehavior() {
                    @Override
                    public void onCast(SpellResolver resolver,
                            ServerLevel world,
                            BlockPos pos,
                            Player fakePlayer,
                            Position iposition,
                            Direction direction) {
                        Direction facingDir = world.getBlockState(pos)
                                .getValue(BasicSpellTurret.FACING);
                        double baseDistance = 1.0;
                        double distance = baseDistance + resolver.getCastStats().getAmpMultiplier() * 0.5;
                        Vec3 eyePos = new Vec3(iposition.x(),
                                iposition.y(), iposition.z());
                        Vec3 lookVec = new Vec3(
                                facingDir.getStepX(),
                                facingDir.getStepY(),
                                facingDir.getStepZ()).normalize();
                        Vec3 targetPos = eyePos.add(lookVec.scale(distance));
                        Direction hitDirection = Direction.getNearest(lookVec.x,
                                lookVec.y, lookVec.z);
                        BlockPos blockPos = BlockPos.containing(targetPos);
                        BlockHitResult result = new BlockHitResult(
                                targetPos, hitDirection, blockPos, false);
                        resolver.onResolveEffect(world, result);
                    }
                });

        BasicSpellTurret.TURRET_BEHAVIOR_MAP.put(
                MethodSelf.INSTANCE,
                new ITurretBehavior() {
                    @Override
                    public void onCast(SpellResolver resolver,
                            ServerLevel world,
                            BlockPos pos,
                            Player fakePlayer,
                            Position iposition,
                            Direction direction) {
                        fakePlayer.setPos(iposition.x(), iposition.y(), iposition.z());
                        resolver.onResolveEffect(world, new EntityHitResult(fakePlayer));
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
                WindVoxelEntity.class,
                FireVoxelEntity.class,
                new WindFireInteraction());
        VoxelInteractionRegistry.register(
                WindVoxelEntity.class,
                WaterVoxelEntity.class,
                new WindWaterInteraction());
        VoxelInteractionRegistry.register(
                WindVoxelEntity.class,
                StoneVoxelEntity.class,
                new WindStoneInteraction());

        VoxelInteractionRegistry.register(
                FireVoxelEntity.class,
                FireVoxelEntity.class,
                mergeInteraction);

        VoxelInteractionRegistry.register(
                WaterVoxelEntity.class,
                WaterVoxelEntity.class,
                mergeInteraction);

        VoxelInteractionRegistry.register(
                WindVoxelEntity.class,
                WindVoxelEntity.class,
                mergeInteraction);

        VoxelInteractionRegistry.register(
                StoneVoxelEntity.class,
                StoneVoxelEntity.class,
                new StoneStoneInteraction());

        VoxelInteractionRegistry.register(
                StoneVoxelEntity.class,
                FireVoxelEntity.class,
                new StoneFireInteraction());

        VoxelInteractionRegistry.register(
                StoneVoxelEntity.class,
                WaterVoxelEntity.class,
                new StoneWaterInteraction());

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
                new IceIceInteraction());

        VoxelInteractionRegistry.register(
                IceVoxelEntity.class,
                FireVoxelEntity.class,
                new IceFireInteraction());

        VoxelInteractionRegistry.register(
                IceVoxelEntity.class,
                WaterVoxelEntity.class,
                new IceWaterInteraction());

        VoxelInteractionRegistry.register(
                IceVoxelEntity.class,
                StoneVoxelEntity.class,
                new IceStoneInteraction());

        VoxelInteractionRegistry.register(
                WindVoxelEntity.class,
                IceVoxelEntity.class,
                new IceWindInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                LightningVoxelEntity.class,
                new LightningLightningInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                FireVoxelEntity.class,
                new LightningFireInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                WaterVoxelEntity.class,
                new LightningWaterInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                StoneVoxelEntity.class,
                new LightningStoneInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                IceVoxelEntity.class,
                new LightningIceInteraction());

        VoxelInteractionRegistry.register(
                LightningVoxelEntity.class,
                WindVoxelEntity.class,
                new LightningWindInteraction());

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

    private static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        AttributeSupplier.Builder builder = AbstractSkeleton.createAttributes();
        builder.add(ModRegistry.NECROMANCY_POWER, 5.0);
        event.put(ModEntities.MAGE_SKELETON.get(), builder.build());
    }

    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.MAGE_SKELETON.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(CapabilityRegistry.MANA_CAPABILITY, ModEntities.MAGE_SKELETON.get(), (entity, ctx) -> new ManaCap(entity));
    }

    public void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();

        if (event.includeServer()) {
            generator.addProvider(true, new DyeRecipeDatagen(generator));
            generator.addProvider(true, new StaffRecipeDatagen(generator));
            generator.addProvider(true, new GlyphRecipeDatagen(generator));
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
                                .setValue(BlightCauldronBlock.LEVEL, 3));
                        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                });
    }
}
