package top.starwindv.cwa.paster;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import top.starwindv.cwa.CarpetWindAddition;

public record PasterCompensateResultPayload(boolean success) implements CustomPacketPayload {
	public static final Type<PasterCompensateResultPayload> TYPE =
			new Type<>(CarpetWindAddition.id("paster_compensate_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PasterCompensateResultPayload> CODEC =
			StreamCodec.of(
					(buffer, payload) -> buffer.writeBoolean(payload.success),
					buffer -> new PasterCompensateResultPayload(buffer.readBoolean())
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
