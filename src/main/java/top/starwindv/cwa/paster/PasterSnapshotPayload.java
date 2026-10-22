package top.starwindv.cwa.paster;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import top.starwindv.cwa.CarpetWindAddition;

public record PasterSnapshotPayload(
		UUID requestId,
		UUID placementId,
		String name,
		String dimension,
		List<PasterMaterial> requiredMaterials,
		List<PasterBounds> bounds,
		String fingerprint,
		String error
) implements CustomPacketPayload {
	public static final Type<PasterSnapshotPayload> TYPE =
			new Type<>(CarpetWindAddition.id("paster_snapshot"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PasterSnapshotPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.requestId);
				buffer.writeUUID(payload.placementId);
				buffer.writeUtf(payload.name);
				buffer.writeUtf(payload.dimension);
				buffer.writeVarInt(payload.requiredMaterials.size());
				for (PasterMaterial material : payload.requiredMaterials) {
					ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, material.sample());
					buffer.writeVarInt(material.count());
				}
				buffer.writeVarInt(payload.bounds.size());
				for (PasterBounds bound : payload.bounds) {
					buffer.writeBlockPos(bound.start());
					buffer.writeBlockPos(bound.end());
				}
				buffer.writeUtf(payload.fingerprint);
				buffer.writeUtf(payload.error);
			},
			buffer -> {
				UUID requestId = buffer.readUUID();
				UUID placementId = buffer.readUUID();
				String name = buffer.readUtf();
				String dimension = buffer.readUtf();
				int materialCount = buffer.readVarInt();
				List<PasterMaterial> materials = new ArrayList<>(materialCount);
				for (int i = 0; i < materialCount; i++) {
					ItemStack sample = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
					materials.add(new PasterMaterial(sample, buffer.readVarInt()));
				}
				int boundsCount = buffer.readVarInt();
				List<PasterBounds> bounds = new ArrayList<>(boundsCount);
				for (int i = 0; i < boundsCount; i++) {
					bounds.add(new PasterBounds(buffer.readBlockPos(), buffer.readBlockPos()));
				}
				return new PasterSnapshotPayload(
						requestId,
						placementId,
						name,
						dimension,
						materials,
						bounds,
						buffer.readUtf(),
						buffer.readUtf()
				);
			}
	);

	public PasterSnapshotPayload {
		requiredMaterials = List.copyOf(requiredMaterials);
		bounds = List.copyOf(bounds);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
