package top.starwindv.cwa.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import top.starwindv.cwa.CarpetWindAddition;

public record NearbyCraftingPayload(List<ItemStack> stacks) implements CustomPacketPayload {
	public static final int MAX_STACKS = 4096;
	public static final CustomPacketPayload.Type<NearbyCraftingPayload> TYPE =
			new CustomPacketPayload.Type<>(CarpetWindAddition.id("nearby_crafting"));

	public static final StreamCodec<RegistryFriendlyByteBuf, NearbyCraftingPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeVarInt(payload.stacks.size());
				for (ItemStack stack : payload.stacks) {
					ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
				}
			},
			buffer -> {
				int size = Math.min(buffer.readVarInt(), MAX_STACKS);
				List<ItemStack> stacks = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
				}
				return new NearbyCraftingPayload(stacks);
			}
	);

	public NearbyCraftingPayload {
		stacks = stacks.size() > MAX_STACKS
				? List.copyOf(stacks.subList(0, MAX_STACKS))
				: List.copyOf(stacks);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
