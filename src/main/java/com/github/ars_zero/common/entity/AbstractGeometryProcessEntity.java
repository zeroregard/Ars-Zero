package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.common.glyph.geometrize.EffectGeometrize;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketCancelEntity;
import com.github.ars_zero.common.network.PacketMoveEntity;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.common.shape.ShapePipeline;
import com.github.ars_zero.common.util.GeometryPlayerPreferences;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractGeometryProcessEntity extends AbstractConvergenceEntity
        implements IGeometryProcessEntity, IAltScrollable, IDepthScrollable {

    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEPTH = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BUILDING = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PAUSED = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CASTER_UUID = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MARKER_POS = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> DATA_MARKER_POS = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<CompoundTag> DATA_GEOMETRY_DESCRIPTION = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<BlockPos> DATA_USER_OFFSET = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_TARGET_BLOCK = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_BASE_POSITION = SynchedEntityData
            .defineId(AbstractGeometryProcessEntity.class, EntityDataSerializers.BLOCK_POS);

    private static final int DEFAULT_SIZE = 5;
    private static final int MIN_SIZE = 1;
    protected static final float BASE_BLOCKS_PER_TICK = 0.5f;

    @Nullable
    private UUID casterUuid = null;
    @Nullable
    private BlockPos markerPos = null;
    private GeometryDescription geometryDescription = GeometryDescription.DEFAULT;
    private BlockPos userOffset = BlockPos.ZERO;
    private Vec3 basePosition = Vec3.ZERO;
    private Vec3 lastAppliedPosition = Vec3.ZERO;
    private int depth = 1;

    @Nullable
    protected SpellContext spellContext = null;
    @Nullable
    protected SpellResolver spellResolver = null;

    protected boolean building = false;
    protected boolean paused = false;
    protected final List<BlockPos> processQueue = new ArrayList<>();
    protected int processIndex = 0;
    protected float blockAccumulator = 0.0f;
    private int soundCooldown = 0;
    private static final int SOUND_COOLDOWN_TICKS = 4;

    private float clientSmoothedYaw = 0f;

    // Static key state tracking - shared across all entities to prevent
    // double-input
    private static boolean clientLastEscapeState = false;
    private static boolean clientLastUpArrowState = false;
    private static boolean clientLastDownArrowState = false;
    private static boolean clientLastLeftArrowState = false;
    private static boolean clientLastRightArrowState = false;
    private static long clientLastMoveTime = 0;
    private static final long MOVE_COOLDOWN_MS = 150; // 150ms between moves

    @Nullable
    private BlockPos cachedPreviewCenter = null;
    private int cachedPreviewSize = -1;
    private int cachedPreviewDepth = -1;
    @Nullable
    private GeometryDescription cachedPreviewDescription = null;
    private Map<BlockPos, BlockStatus> cachedBlockStatuses = new HashMap<>();

    public AbstractGeometryProcessEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 1, state -> {
            if (!isBuilding()) {
                return PlayState.STOP;
            }
            state.getController().setAnimation(RawAnimation.begin().thenPlay("tending_master"));
            if (isPaused()) {
                state.getController().setAnimationSpeed(0.0);
            } else {
                state.getController().setAnimationSpeed(1.0);
            }
            return PlayState.CONTINUE;
        }));
    }

    public void setCasterUUID(@Nullable UUID casterUuid) {
        this.casterUuid = casterUuid;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_CASTER_UUID, Optional.ofNullable(casterUuid));
        }
    }

    public void setCaster(@Nullable LivingEntity caster) {
        if (caster == null)
            return;
        setCasterUUID(caster.getUUID());
    }

    @Override
    @Nullable
    public UUID getCasterUUID() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_CASTER_UUID).orElse(null);
        }
        return this.casterUuid;
    }

    public void setMarkerPos(@Nullable BlockPos markerPos) {
        this.markerPos = markerPos;
        if (!this.level().isClientSide) {
            boolean has = markerPos != null;
            this.entityData.set(DATA_HAS_MARKER_POS, has);
            if (has) {
                this.entityData.set(DATA_MARKER_POS, markerPos);
            }
        }
    }

    @Nullable
    public BlockPos getMarkerPos() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_HAS_MARKER_POS) ? this.entityData.get(DATA_MARKER_POS) : null;
        }
        return this.markerPos;
    }

    public void setUserOffset(BlockPos offset) {
        this.userOffset = offset;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_USER_OFFSET, offset);
        }
    }

    public BlockPos getUserOffset() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_USER_OFFSET);
        }
        return this.userOffset;
    }

    public void addUserOffset(BlockPos delta) {
        setUserOffset(this.getUserOffset().offset(delta));
    }

    /**
     * Get the effective center position including user offset.
     * During preview: follows entity position (Anchor can move it)
     * During building: uses fixed basePosition set when building started
     */
    public BlockPos getEffectiveCenter() {
        BlockPos baseCenter;
        if (isBuilding()) {
            baseCenter = getBasePositionBlock();
        } else {
            baseCenter = BlockPos.containing(this.position());
        }
        BlockPos offset = getUserOffset();
        return baseCenter.offset(offset);
    }

    public void setBasePosition(Vec3 pos) {
        this.basePosition = pos;
        this.lastAppliedPosition = pos;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_BASE_POSITION, BlockPos.containing(pos));
        }
    }

    public BlockPos getBasePositionBlock() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_BASE_POSITION);
        }
        return BlockPos.containing(this.basePosition);
    }

    @Override
    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    public void setSize(int size) {
        int maxSize = getMaxSize();
        int newSize = Math.max(MIN_SIZE, Math.min(size, maxSize));
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_SIZE, newSize);
            savePreferredSize(newSize);
        }
    }

    private void savePreferredSize(int size) {
        if (this.level() instanceof ServerLevel serverLevel && this.casterUuid != null) {
            Player player = serverLevel.getServer().getPlayerList().getPlayer(this.casterUuid);
            if (player != null) {
                GeometryPlayerPreferences.setPreferredSize(player, size);
            }
        }
    }

    @Override
    public int getDepth() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_DEPTH);
        }
        return this.depth;
    }

    public void setDepth(int depth) {
        int maxSize = getMaxSize();
        int clampedDepth = Math.max(-maxSize, Math.min(depth, maxSize));
        this.depth = clampedDepth;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_DEPTH, clampedDepth);
            savePreferredDepth(clampedDepth);
        }
    }

    private void savePreferredDepth(int depth) {
        if (this.level() instanceof ServerLevel serverLevel && this.casterUuid != null) {
            Player player = serverLevel.getServer().getPlayerList().getPlayer(this.casterUuid);
            if (player != null) {
                GeometryPlayerPreferences.setPreferredDepth(player, depth);
            }
        }
    }

    public void adjustDepthStep(int direction) {
        if (direction == 0)
            return;
        int current = getDepth();
        int next = current + direction;
        setDepth(next);
    }

    public void setSpellContext(@Nullable SpellContext context, @Nullable SpellResolver resolver) {
        this.spellContext = context;
        this.spellResolver = resolver;
    }

    @Nullable
    public SpellContext getSpellContext() {
        return this.spellContext;
    }

    @Nullable
    public SpellResolver getSpellResolver() {
        return this.spellResolver;
    }

    protected int getMaxSize() {
        return Math.max(MIN_SIZE, EffectGeometrize.INSTANCE.getMaxSize());
    }

    public boolean isBuilding() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_BUILDING);
        }
        return this.building;
    }

    public boolean isPaused() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_PAUSED);
        }
        return this.paused;
    }

    protected void setBuilding(boolean building) {
        boolean wasBuilding = this.building;
        this.building = building;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_BUILDING, building);
            if (wasBuilding != building) {
                ArsZero.LOGGER.info("[Geometry] {} setBuilding: {} -> {}", this.getClass().getSimpleName(), wasBuilding,
                        building);
            }
        }
    }

    protected void setPaused(boolean paused) {
        this.paused = paused;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_PAUSED, paused);
        }
    }

    @Nullable
    public BlockPos getTargetBlock() {
        BlockPos target = this.entityData.get(DATA_TARGET_BLOCK);
        return target.equals(BlockPos.ZERO) ? null : target;
    }

    protected void updateTargetBlock() {
        if (!this.level().isClientSide && this.building && this.processIndex < this.processQueue.size()) {
            this.entityData.set(DATA_TARGET_BLOCK, this.processQueue.get(this.processIndex));
        }
    }

    public float getSmoothedYaw(float targetYaw) {
        float diff = targetYaw - this.clientSmoothedYaw;
        while (diff > 180f)
            diff -= 360f;
        while (diff < -180f)
            diff += 360f;
        this.clientSmoothedYaw += diff * 0.1f;
        while (this.clientSmoothedYaw > 180f)
            this.clientSmoothedYaw -= 360f;
        while (this.clientSmoothedYaw < -180f)
            this.clientSmoothedYaw += 360f;
        return this.clientSmoothedYaw;
    }

    @Override
    public void setGeometryDescription(GeometryDescription description) {
        GeometryDescription desc = description != null ? description : GeometryDescription.DEFAULT;
        this.geometryDescription = desc;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_GEOMETRY_DESCRIPTION, desc.toTag());
        }
    }

    @Override
    public GeometryDescription getGeometryDescription() {
        if (this.level().isClientSide) {
            CompoundTag tag = this.entityData.get(DATA_GEOMETRY_DESCRIPTION);
            return tag != null && !tag.isEmpty() ? GeometryDescription.fromTag(tag) : GeometryDescription.DEFAULT;
        }
        return this.geometryDescription;
    }

    @Override
    public Level getProcessLevel() {
        return this.level();
    }

    @Override
    public boolean isProcessing() {
        return isBuilding();
    }

    @Override
    public void cancelProcess() {
        this.discard();
    }

    @Override
    public void startProcess() {
        ArsZero.LOGGER.info("[Geometry] {} startProcess() called. building={}", this.getClass().getSimpleName(),
                this.building);
        if (this.building) {
            ArsZero.LOGGER.warn("[Geometry] {} startProcess() called but already building, returning",
                    this.getClass().getSimpleName());
            return;
        }

        ArsZero.LOGGER.info("[Geometry] {} Starting process. Size={}, Depth={}, Queue size will be generated",
                this.getClass().getSimpleName(), getSize(), getDepth());

        setBuilding(true);
        setPaused(false);
        this.processQueue.clear();
        this.processIndex = 0;
        this.blockAccumulator = 0.0f;

        setBasePosition(this.position());

        BlockPos center = getEffectiveCenter();
        this.processQueue.addAll(ShapePipeline.generate(center, getSize(), geometryDescription, getDepth()));

        ArsZero.LOGGER.info("[Geometry] {} Generated {} blocks to process", this.getClass().getSimpleName(),
                this.processQueue.size());

        if (shouldReverseProcessOrder()) {
            java.util.Collections.reverse(this.processQueue);
            ArsZero.LOGGER.info("[Geometry] {} Reversed process order", this.getClass().getSimpleName());
        }

        if (this.level() instanceof ServerLevel serverLevel && this.casterUuid != null) {
            Player caster = serverLevel.getServer().getPlayerList().getPlayer(this.casterUuid);
            if (caster != null) {
                double angle = this.random.nextDouble() * 2 * Math.PI;
                double offsetX = Math.cos(angle);
                double offsetZ = Math.sin(angle);
                this.setPos(caster.position().x + offsetX, caster.position().y, caster.position().z + offsetZ);
                ArsZero.LOGGER.info("[Geometry] {} Moved to caster position with offset",
                        this.getClass().getSimpleName());
            } else {
                ArsZero.LOGGER.warn("[Geometry] {} Caster UUID set but player not found: {}",
                        this.getClass().getSimpleName(), this.casterUuid);
            }
        } else {
            ArsZero.LOGGER.warn("[Geometry] {} No caster UUID or not ServerLevel", this.getClass().getSimpleName());
        }

        this.noPhysics = false;
        updateBoundingBox();

        ArsZero.LOGGER.info("[Geometry] {} Process started successfully. Building={}, Paused={}",
                this.getClass().getSimpleName(), this.building, this.paused);

        onProcessStarted();
    }

    protected boolean shouldReverseProcessOrder() {
        return false;
    }

    protected void onProcessStarted() {
    }

    protected void onPauseToggled(boolean paused) {
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide)
            return InteractionResult.SUCCESS;

        UUID caster = getCasterUUID();
        if (caster == null || !caster.equals(player.getUUID()))
            return InteractionResult.PASS;

        if (!isBuilding()) {
            startProcess();
            return InteractionResult.CONSUME;
        }

        setPaused(!isPaused());
        onPauseToggled(isPaused());
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide)
            return true;
        if (!(source.getEntity() instanceof Player player))
            return false;

        UUID caster = getCasterUUID();
        if (caster == null || !caster.equals(player.getUUID()))
            return false;

        if (!isBuilding()) {
            this.discard();
            return true;
        }

        this.discard();
        return true;
    }

    @Override
    public boolean shouldStart() {
        boolean superResult = super.shouldStart();
        boolean notBuilding = !isBuilding();
        boolean result = notBuilding && superResult;
        ArsZero.LOGGER.info("[Geometry] {} shouldStart(): super={}, notBuilding={}, result={}",
                this.getClass().getSimpleName(), superResult, notBuilding, result);
        return result;
    }

    @Override
    protected void onLifespanReached() {
        ArsZero.LOGGER.info("[Geometry] {} onLifespanReached() called. isClientSide={}, building={}",
                this.getClass().getSimpleName(), this.level().isClientSide, this.building);
        if (this.level().isClientSide || this.building)
            return;
        ArsZero.LOGGER.info("[Geometry] {} calling startProcess()", this.getClass().getSimpleName());
        startProcess();
    }

    private static final double FOLLOW_DISTANCE = 2;
    private static final double LERP_SPEED = 0.03;

    @Override
    public void tick() {
        boolean shouldHaveNoPhysics = !isBuilding();
        if (this.noPhysics != shouldHaveNoPhysics) {
            this.noPhysics = shouldHaveNoPhysics;
            updateBoundingBox();
        }

        super.tick();

        if (this.building && this.casterUuid != null) {
            lerpTowardsCaster();
        }

        if (this.level().isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            checkForCancelInput();
            checkForMovementInput();
            applyClientPositionOffset();
        }

        if (!this.level().isClientSide && this.building && !this.paused) {
            tickProcess();
        }
    }

    private void lerpTowardsCaster() {
        if (this.level() instanceof ServerLevel serverLevel && this.casterUuid != null) {
            Player caster = serverLevel.getServer().getPlayerList().getPlayer(this.casterUuid);
            if (caster != null) {
                Vec3 casterPos = caster.position().add(0, 1.0, 0);
                Vec3 entityPos = this.position();
                Vec3 toEntity = entityPos.subtract(casterPos);
                double distance = toEntity.length();

                if (distance > FOLLOW_DISTANCE) {
                    Vec3 targetPos = casterPos.add(toEntity.normalize().scale(FOLLOW_DISTANCE));
                    Vec3 newPos = entityPos.add(targetPos.subtract(entityPos).scale(LERP_SPEED));
                    this.setPos(newPos.x, newPos.y, newPos.z);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void applyClientPositionOffset() {
        // Only apply user offset visually on client - don't fight with Anchor or server
        // position
        // The actual offset is stored server-side and applied there
        // This method is now a no-op to prevent jitter with Anchor
    }

    protected void tickProcess() {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        updateTargetBlock();

        if (this.processIndex >= this.processQueue.size()) {
            this.discard();
            return;
        }

        if (this.paused)
            return;

        float rate = getBlocksPerTick();
        this.blockAccumulator += rate;

        int blocksToProcess = (int) this.blockAccumulator;
        if (blocksToProcess <= 0)
            return;

        int oldIndex = this.processIndex;
        int processed = processBlocks(serverLevel, blocksToProcess);

        if (processed > 0 && soundCooldown <= 0) {
            BlockPos firstPos = this.processQueue.get(oldIndex);
            playProcessSound(serverLevel, firstPos, processed);
            soundCooldown = SOUND_COOLDOWN_TICKS;
        }
        if (soundCooldown > 0) {
            soundCooldown--;
        }

        this.blockAccumulator -= processed;
    }

    protected float getBlocksPerTick() {
        return BASE_BLOCKS_PER_TICK;
    }

    protected int processBlocks(ServerLevel level, int maxCount) {
        int count = 0;
        while (this.processIndex < this.processQueue.size() && count < maxCount) {
            BlockPos pos = this.processQueue.get(this.processIndex);
            ProcessResult result = processBlock(level, pos);
            switch (result) {
                case PROCESSED -> {
                    count++;
                    this.processIndex++;
                }
                case SKIPPED -> {
                    this.processIndex++;
                }
                case WAITING_FOR_MANA -> {
                    return count;
                }
            }
        }
        return count;
    }

    public enum ProcessResult {
        PROCESSED,
        SKIPPED,
        WAITING_FOR_MANA
    }

    protected abstract ProcessResult processBlock(ServerLevel level, BlockPos pos);

    protected void playProcessSound(ServerLevel level, BlockPos pos, int blocksProcessed) {
        BlockState state = level.getBlockState(pos);
        SoundType soundType = state.getSoundType();
        float basePitch = soundType.getPitch() * 0.8f;
        float pitchIncrease = Math.min(blocksProcessed * 0.02f, 0.5f);
        level.playSound(null, pos, getProcessSound(soundType), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0f) / 2.0f, basePitch + pitchIncrease);
    }

    protected abstract net.minecraft.sounds.SoundEvent getProcessSound(SoundType soundType);

    private void updateBoundingBox() {
        Vec3 pos = this.position();
        if (isBuilding()) {
            double halfSize = 0.375;
            double height = 0.75;
            double yOffset = 0.5;
            this.setBoundingBox(new AABB(pos.x - halfSize, pos.y + yOffset, pos.z - halfSize, pos.x + halfSize,
                    pos.y + yOffset + height, pos.z + halfSize));
        } else {
            this.setBoundingBox(new AABB(pos.x - 0.25, pos.y, pos.z - 0.25, pos.x + 0.25, pos.y + 0.5, pos.z + 0.25));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void checkForCancelInput() {
        if (isBuilding())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        UUID casterUuid = getCasterUUID();
        if (casterUuid == null || !casterUuid.equals(mc.player.getUUID()))
            return;

        boolean shouldCancel = mc.screen instanceof AbstractMultiPhaseCastDeviceScreen;

        long window = mc.getWindow().getWindow();
        boolean escapePressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
        if (escapePressed && !this.clientLastEscapeState && mc.screen == null) {
            shouldCancel = true;
        }
        this.clientLastEscapeState = escapePressed;

        if (shouldCancel) {
            Networking.sendToServer(new PacketCancelEntity(this.getId()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void checkForMovementInput() {
        if (isBuilding())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null)
            return;

        UUID casterUuid = getCasterUUID();
        if (casterUuid == null || !casterUuid.equals(mc.player.getUUID()))
            return;

        long window = mc.getWindow().getWindow();
        boolean upPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean downPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS;
        boolean leftPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS;
        boolean rightPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS;

        long now = System.currentTimeMillis();
        boolean canMove = (now - clientLastMoveTime) >= MOVE_COOLDOWN_MS;

        Vec3 lookDirection = mc.player.getLookAngle();

        // Edge detection: only trigger on key down (pressed now, wasn't pressed before)
        if (upPressed && !clientLastUpArrowState && canMove) {
            Networking
                    .sendToServer(new PacketMoveEntity(this.getId(), PacketMoveEntity.MoveDirection.UP, lookDirection));
            clientLastMoveTime = now;
        }
        if (downPressed && !clientLastDownArrowState && canMove) {
            Networking.sendToServer(
                    new PacketMoveEntity(this.getId(), PacketMoveEntity.MoveDirection.DOWN, lookDirection));
            clientLastMoveTime = now;
        }
        if (leftPressed && !clientLastLeftArrowState && canMove) {
            Networking.sendToServer(
                    new PacketMoveEntity(this.getId(), PacketMoveEntity.MoveDirection.LEFT, lookDirection));
            clientLastMoveTime = now;
        }
        if (rightPressed && !clientLastRightArrowState && canMove) {
            Networking.sendToServer(
                    new PacketMoveEntity(this.getId(), PacketMoveEntity.MoveDirection.RIGHT, lookDirection));
            clientLastMoveTime = now;
        }

        clientLastUpArrowState = upPressed;
        clientLastDownArrowState = downPressed;
        clientLastLeftArrowState = leftPressed;
        clientLastRightArrowState = rightPressed;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Map<BlockPos, BlockStatus> getBlockStatuses() {
        BlockPos currentCenter = getEffectiveCenter();
        int currentSize = this.getSize();
        int currentDepth = this.getDepth();
        GeometryDescription currentDescription = this.getGeometryDescription();

        if (this.cachedPreviewCenter != null
                && this.cachedPreviewCenter.equals(currentCenter)
                && this.cachedPreviewSize == currentSize
                && this.cachedPreviewDepth == currentDepth
                && this.cachedPreviewDescription != null
                && this.cachedPreviewDescription.baseShape() == currentDescription.baseShape()
                && this.cachedPreviewDescription.fillMode() == currentDescription.fillMode()
                && this.cachedPreviewDescription.projectionMode() == currentDescription.projectionMode()) {
            return this.cachedBlockStatuses;
        }

        List<BlockPos> positions = generatePositions(currentCenter);
        Map<BlockPos, BlockStatus> statuses = new HashMap<>(positions.size());
        java.util.Set<BlockPos> positionSet = new HashSet<>(positions);

        for (BlockPos pos : positions) {
            statuses.put(pos, computeStatusForBlock(this.level(), pos, positionSet));
        }

        this.cachedBlockStatuses = statuses;
        this.cachedPreviewCenter = currentCenter;
        this.cachedPreviewSize = currentSize;
        this.cachedPreviewDepth = currentDepth;
        this.cachedPreviewDescription = currentDescription;
        return this.cachedBlockStatuses;
    }

    @Override
    public boolean isInvisible() {
        return false;
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        UUID caster = getCasterUUID();
        if (caster == null || player == null)
            return true;
        return !caster.equals(player.getUUID());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.5f;
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        if (isBuilding()) {
            return false;
        }
        return super.canCollideWith(entity);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isBuilding();
    }

    @Override
    public void push(double x, double y, double z) {
        if (isBuilding()) {
            super.push(x, y, z);
        }
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
        if (isBuilding()) {
            super.push(entity);
        }
    }

    @Override
    public void move(net.minecraft.world.entity.MoverType moverType, Vec3 movement) {
        if (!isBuilding()) {
            // Skip all collision - just set position directly
            this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        } else {
            super.move(moverType, movement);
        }
    }

    @Override
    protected void checkInsideBlocks() {
        // Don't check for being inside blocks when not building
        if (isBuilding()) {
            super.checkInsideBlocks();
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        Vec3 pos = this.position();
        if (isBuilding()) {
            double halfSize = 0.375;
            double height = 0.75;
            double yOffset = 0.7;
            return new AABB(pos.x - halfSize, pos.y + yOffset, pos.z - halfSize, pos.x + halfSize,
                    pos.y + yOffset + height, pos.z + halfSize);
        }
        return new AABB(pos.x - 0.25, pos.y + 0.2, pos.z - 0.25, pos.x + 0.25, pos.y + 0.7, pos.z + 0.25);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        if (isBuilding()) {
            return net.minecraft.world.entity.EntityDimensions.fixed(0.75f, 0.75f);
        }
        return net.minecraft.world.entity.EntityDimensions.fixed(0.5f, 0.5f);
    }

    @Override
    public void handleAltScroll(double scrollDelta) {
        if (getLifespan() <= 0 || isBuilding())
            return;
        int direction = scrollDelta > 0 ? 1 : (scrollDelta < 0 ? -1 : 0);
        if (direction == 0)
            return;
        setSize(getSize() + direction);
    }

    @Override
    public void handleDepthScroll(double scrollDelta) {
        if (getLifespan() <= 0 || isBuilding())
            return;
        if (!getGeometryDescription().isFlattened())
            return;
        int direction = scrollDelta > 0 ? 1 : -1;
        adjustDepthStep(direction);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SIZE, DEFAULT_SIZE);
        builder.define(DATA_DEPTH, 1);
        builder.define(DATA_BUILDING, false);
        builder.define(DATA_PAUSED, false);
        builder.define(DATA_CASTER_UUID, Optional.empty());
        builder.define(DATA_HAS_MARKER_POS, false);
        builder.define(DATA_MARKER_POS, BlockPos.ZERO);
        builder.define(DATA_GEOMETRY_DESCRIPTION, GeometryDescription.DEFAULT.toTag());
        builder.define(DATA_USER_OFFSET, BlockPos.ZERO);
        builder.define(DATA_TARGET_BLOCK, BlockPos.ZERO);
        builder.define(DATA_BASE_POSITION, BlockPos.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("size")) {
            this.entityData.set(DATA_SIZE, compound.getInt("size"));
        }
        if (compound.contains("depth")) {
            this.depth = compound.getInt("depth");
            this.entityData.set(DATA_DEPTH, this.depth);
        }
        if (compound.contains("building")) {
            this.building = compound.getBoolean("building");
            this.entityData.set(DATA_BUILDING, this.building);
        }
        if (compound.contains("paused")) {
            this.paused = compound.getBoolean("paused");
            this.entityData.set(DATA_PAUSED, this.paused);
        }
        if (compound.contains("process_index")) {
            this.processIndex = compound.getInt("process_index");
        }
        if (compound.contains("block_accumulator")) {
            this.blockAccumulator = compound.getFloat("block_accumulator");
        }
        if (compound.hasUUID("caster_uuid")) {
            this.casterUuid = compound.getUUID("caster_uuid");
            this.entityData.set(DATA_CASTER_UUID, Optional.of(this.casterUuid));
        }
        if (compound.contains("marker_pos")) {
            CompoundTag markerTag = compound.getCompound("marker_pos");
            this.markerPos = new BlockPos(markerTag.getInt("x"), markerTag.getInt("y"), markerTag.getInt("z"));
            this.entityData.set(DATA_HAS_MARKER_POS, true);
            this.entityData.set(DATA_MARKER_POS, this.markerPos);
        }
        if (compound.contains("shape_description")) {
            this.geometryDescription = GeometryDescription.fromTag(compound.getCompound("shape_description"));
            this.entityData.set(DATA_GEOMETRY_DESCRIPTION, this.geometryDescription.toTag());
        }
        if (compound.contains("user_offset")) {
            this.userOffset = BlockPos.of(compound.getLong("user_offset"));
            this.entityData.set(DATA_USER_OFFSET, this.userOffset);
        }
        if (compound.contains("base_position")) {
            CompoundTag basePosTag = compound.getCompound("base_position");
            this.basePosition = new Vec3(basePosTag.getDouble("x"), basePosTag.getDouble("y"),
                    basePosTag.getDouble("z"));
            this.entityData.set(DATA_BASE_POSITION, BlockPos.containing(this.basePosition));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("size", getSize());
        compound.putInt("depth", this.depth);
        compound.putBoolean("building", this.building);
        compound.putBoolean("paused", this.paused);
        compound.putInt("process_index", this.processIndex);
        compound.putFloat("block_accumulator", this.blockAccumulator);
        if (this.casterUuid != null) {
            compound.putUUID("caster_uuid", this.casterUuid);
        }
        if (this.markerPos != null) {
            CompoundTag markerTag = new CompoundTag();
            markerTag.putInt("x", this.markerPos.getX());
            markerTag.putInt("y", this.markerPos.getY());
            markerTag.putInt("z", this.markerPos.getZ());
            compound.put("marker_pos", markerTag);
        }
        compound.put("shape_description", this.geometryDescription.toTag());
        compound.putLong("user_offset", this.userOffset.asLong());
        CompoundTag basePosTag = new CompoundTag();
        basePosTag.putDouble("x", this.basePosition.x);
        basePosTag.putDouble("y", this.basePosition.y);
        basePosTag.putDouble("z", this.basePosition.z);
        compound.put("base_position", basePosTag);
    }
}
