package top.starwindv.cwa.paster;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;
import top.starwindv.cwa.CarpetWindAddition;

public record PasterRequestPayload(
		UUID requestId,
		PasterAction action,
		int page,
		@Nullable BlockPos start,
		@Nullable BlockPos end,
		@Nullable String containerId,
		@Nullable String itemId
) implements CustomPacketPayload {
	public static final Type<PasterRequestPayload> TYPE =
			new Type<>(CarpetWindAddition.id("paster_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PasterRequestPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.requestId);
				buffer.writeUtf(payload.action.name());
				buffer.writeVarInt(payload.page);
				buffer.writeBoolean(payload.start != null);
				if (payload.start != null) {
					buffer.writeBlockPos(payload.start);
				}
				buffer.writeBoolean(payload.end != null);
				if (payload.end != null) {
					buffer.writeBlockPos(payload.end);
				}
				buffer.writeBoolean(payload.containerId != null);
				if (payload.containerId != null) {
					buffer.writeUtf(payload.containerId);
				}
				buffer.writeBoolean(payload.itemId != null);
				if (payload.itemId != null) {
					buffer.writeUtf(payload.itemId);
				}
			},
			buffer -> new PasterRequestPayload(
					buffer.readUUID(),
					PasterAction.valueOf(buffer.readUtf()),
					buffer.readVarInt(),
					buffer.readBoolean() ? buffer.readBlockPos() : null,
					buffer.readBoolean() ? buffer.readBlockPos() : null,
					buffer.readBoolean() ? buffer.readUtf() : null,
					buffer.readBoolean() ? buffer.readUtf() : null
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
