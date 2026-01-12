package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

public record PacketMoveEntity(int entityId, MoveDirection direction, Vec3 playerLookDirection)
    implements CustomPacketPayload {
  public static final CustomPacketPayload.Type<PacketMoveEntity> TYPE = new CustomPacketPayload.Type<>(
      ResourceLocation.fromNamespaceAndPath(ArsZero.MOD_ID, "move_entity"));

  public static final StreamCodec<FriendlyByteBuf, PacketMoveEntity> CODEC = StreamCodec.of(
      PacketMoveEntity::write,
      PacketMoveEntity::read);

  public static void write(FriendlyByteBuf buf, PacketMoveEntity packet) {
    buf.writeInt(packet.entityId);
    buf.writeEnum(packet.direction);
    buf.writeDouble(packet.playerLookDirection.x);
    buf.writeDouble(packet.playerLookDirection.y);
    buf.writeDouble(packet.playerLookDirection.z);
  }

  public static PacketMoveEntity read(FriendlyByteBuf buf) {
    int entityId = buf.readInt();
    MoveDirection direction = buf.readEnum(MoveDirection.class);
    double x = buf.readDouble();
    double y = buf.readDouble();
    double z = buf.readDouble();
    return new PacketMoveEntity(entityId, direction, new Vec3(x, y, z));
  }

  public static void handle(PacketMoveEntity packet, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer serverPlayer)) {
        return;
      }

      Entity entity = serverPlayer.level().getEntity(packet.entityId);
      if (entity == null) {
        return;
      }

      Vec3 offset = calculateOffset(packet.direction, packet.playerLookDirection);
      BlockPos blockOffset = new BlockPos(
          (int) Math.round(offset.x),
          (int) Math.round(offset.y),
          (int) Math.round(offset.z));

      if (entity instanceof AbstractGeometryProcessEntity geometryEntity) {
        geometryEntity.addUserOffset(blockOffset);

        Optional<ItemStack> casterTool = Stream.of(
            serverPlayer.getMainHandItem(),
            serverPlayer.getOffhandItem()).filter(stack -> stack.getItem() instanceof AbstractMultiPhaseCastDevice)
            .findFirst();

        if (casterTool.isPresent()) {
          MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(serverPlayer,
              casterTool.get());
          if (castContext != null && !castContext.beginResults.isEmpty()) {
            castContext.beginResults.get(0).userOffset = geometryEntity.getUserOffset();
            castContext.beginResults.get(0).depth = geometryEntity.getDepth();
          }
        }
      } else {
        Vec3 newPos = entity.position().add(offset);
        entity.setPos(newPos.x, newPos.y, newPos.z);
      }
    });
  }

  private static Vec3 calculateOffset(MoveDirection direction, Vec3 playerLookDirection) {
    return switch (direction) {
      case UP -> new Vec3(0, 1, 0);
      case DOWN -> new Vec3(0, -1, 0);
      case LEFT, RIGHT -> {
        // Round to nearest axis - if looking more along X, left/right moves along Z and
        // vice versa
        double absX = Math.abs(playerLookDirection.x);
        double absZ = Math.abs(playerLookDirection.z);

        if (absX >= absZ) {
          // Looking more along X axis, so left/right is along Z
          // If looking +X, left is -Z, right is +Z
          // If looking -X, left is +Z, right is -Z
          int sign = playerLookDirection.x >= 0 ? 1 : -1;
          int leftRightSign = direction == MoveDirection.LEFT ? -1 : 1;
          yield new Vec3(0, 0, sign * leftRightSign);
        } else {
          // Looking more along Z axis, so left/right is along X
          // If looking +Z, left is +X, right is -X
          // If looking -Z, left is -X, right is +X
          int sign = playerLookDirection.z >= 0 ? 1 : -1;
          int leftRightSign = direction == MoveDirection.LEFT ? 1 : -1;
          yield new Vec3(sign * leftRightSign, 0, 0);
        }
      }
    };
  }

  @Override
  public @NotNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public enum MoveDirection {
    UP, DOWN, LEFT, RIGHT
  }
}
