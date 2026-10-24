package top.starwindv.cwa.paster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import top.starwindv.cwa.CarpetWindAddition;

/**
 * Server-owned data for a Litematica placement. The client only supplies the
 * initial snapshot; later collection and paste operations use this copy.
 */
public final class PasterSnapshotData extends SavedData {
	private static final Codec<PasterBounds> BOUNDS_CODEC = Codec.INT.listOf().xmap(
			values -> {
				if (values.size() != 6) {
					throw new IllegalArgumentException("Expected six coordinates for a paster bound");
				}
				return new PasterBounds(
						new BlockPos(values.get(0), values.get(1), values.get(2)),
						new BlockPos(values.get(3), values.get(4), values.get(5))
				);
			},
			bounds -> List.of(
					bounds.start().getX(), bounds.start().getY(), bounds.start().getZ(),
					bounds.end().getX(), bounds.end().getY(), bounds.end().getZ()
			)
	);

	private static final Codec<PasterMaterial> MATERIAL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.CODEC.fieldOf("item").forGetter(PasterMaterial::sample),
			Codec.INT.fieldOf("count").forGetter(PasterMaterial::count)
	).apply(instance, PasterMaterial::new));

	private static final Codec<StoredSnapshot> SNAPSHOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("name").forGetter(StoredSnapshot::name),
			Codec.STRING.fieldOf("dimension").forGetter(StoredSnapshot::dimension),
			MATERIAL_CODEC.listOf().fieldOf("required").forGetter(StoredSnapshot::requiredMaterials),
			BOUNDS_CODEC.listOf().fieldOf("bounds").forGetter(StoredSnapshot::bounds),
			PasterDiscardRule.CODEC.listOf()
					.optionalFieldOf("discard", List.of())
					.forGetter(StoredSnapshot::discardRules),
			Codec.STRING.fieldOf("fingerprint").forGetter(StoredSnapshot::fingerprint)
	).apply(instance, StoredSnapshot::new));

	public static final Codec<PasterSnapshotData> CODEC = Codec.unboundedMap(Codec.STRING, SNAPSHOT_CODEC)
			.xmap(PasterSnapshotData::new, PasterSnapshotData::entries);

	public static final SavedDataType<PasterSnapshotData> TYPE = new SavedDataType<>(
			CarpetWindAddition.id("paster_snapshots"),
			PasterSnapshotData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<String, StoredSnapshot> entries;

	public PasterSnapshotData() {
		this(Map.of());
	}

	public PasterSnapshotData(Map<String, StoredSnapshot> entries) {
		this.entries = new LinkedHashMap<>(entries);
	}

	public static PasterSnapshotData get(MinecraftServer server) {
		return server.overworld().getDataStorage().get(TYPE);
	}

	public static PasterSnapshotData getOrCreate(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public StoredSnapshot get(UUID playerId, UUID placementId) {
		return entries.get(key(playerId, placementId));
	}

	public void set(UUID playerId, UUID placementId, StoredSnapshot snapshot) {
		entries.put(key(playerId, placementId), snapshot);
		setDirty();
	}

	private static String key(UUID playerId, UUID placementId) {
		return playerId + "/" + placementId;
	}

	private Map<String, StoredSnapshot> entries() {
		return Collections.unmodifiableMap(entries);
	}

	public record StoredSnapshot(
			String name,
			String dimension,
			List<PasterMaterial> requiredMaterials,
			List<PasterBounds> bounds,
			List<PasterDiscardRule> discardRules,
			String fingerprint
	) {
		public StoredSnapshot {
			requiredMaterials = List.copyOf(requiredMaterials);
			bounds = List.copyOf(bounds);
			discardRules = List.copyOf(discardRules);
		}

		public PlacementSnapshot toPlacementSnapshot(UUID id) {
			return new PlacementSnapshot(
					id,
					name,
					dimension,
					null,
					requiredMaterials,
					bounds,
					fingerprint,
					discardRules
			);
		}
	}
}
