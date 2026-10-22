package top.starwindv.cwa.paster;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import top.starwindv.cwa.CarpetWindAddition;

public record PasterStartPastePayload(UUID placementId, String fingerprint) implements CustomPacketPayload {
	public static final Type<PasterStartPastePayload> TYPE =
			new Type<>(CarpetWindAddition.id("paster_start_paste"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PasterStartPastePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.placementId);
				buffer.writeUtf(payload.fingerprint);
			},
			buffer -> new PasterStartPastePayload(buffer.readUUID(), buffer.readUtf())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
