package top.starwindv.cwa.stack;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import top.starwindv.cwa.CarpetWindAddition;

public record StackSizesPayload(Map<String, Integer> sizes) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StackSizesPayload> TYPE =
			new CustomPacketPayload.Type<>(CarpetWindAddition.id("stack_sizes"));

	public static final StreamCodec<RegistryFriendlyByteBuf, StackSizesPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeVarInt(payload.sizes.size());
				payload.sizes.forEach((item, size) -> {
					buffer.writeUtf(item);
					buffer.writeVarInt(size);
				});
			},
			buffer -> {
				int count = buffer.readVarInt();
				Map<String, Integer> sizes = new HashMap<>();
				for (int i = 0; i < count; i++) {
					sizes.put(buffer.readUtf(), buffer.readVarInt());
				}
				return new StackSizesPayload(Map.copyOf(sizes));
			}
	);

	public StackSizesPayload {
		sizes = Map.copyOf(sizes);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
