package top.starwindv.cwa.paster;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import top.starwindv.cwa.CarpetWindAddition;

public record PasterPasteResultPayload(UUID placementId, boolean success) implements CustomPacketPayload {
	public static final Type<PasterPasteResultPayload> TYPE =
			new Type<>(CarpetWindAddition.id("paster_paste_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PasterPasteResultPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.placementId);
				buffer.writeBoolean(payload.success);
			},
			buffer -> new PasterPasteResultPayload(buffer.readUUID(), buffer.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
