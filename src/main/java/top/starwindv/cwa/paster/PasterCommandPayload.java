package top.starwindv.cwa.paster;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import top.starwindv.cwa.CarpetWindAddition;

/**
 * A single command produced by Litematica's client side paste task. It is not
 * executed until the server has an active remote paste for the same placement.
 */
public record PasterCommandPayload(UUID placementId, String command) implements CustomPacketPayload {
	public static final Type<PasterCommandPayload> TYPE =
			new Type<>(CarpetWindAddition.id("paster_command"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PasterCommandPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.placementId);
				buffer.writeUtf(payload.command, 32767);
			},
			buffer -> new PasterCommandPayload(buffer.readUUID(), buffer.readUtf(32767))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
