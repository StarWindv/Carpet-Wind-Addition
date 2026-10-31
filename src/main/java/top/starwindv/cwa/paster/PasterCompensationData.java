package top.starwindv.cwa.paster;

import com.mojang.serialization.Codec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import top.starwindv.cwa.util.SavedDataIO;

/**
 * Server-side guard for the one-time paster compensation. The client also keeps
 * a marker across servers; this set prevents reuse on the same server.
 */
public final class PasterCompensationData extends SavedData {
	public static final Codec<PasterCompensationData> CODEC = Codec.STRING.listOf().xmap(
			values -> new PasterCompensationData(new LinkedHashSet<>(values)),
			data -> List.copyOf(data.used)
	);

	private static final String DATA_NAME = "cwa_paster_compensations";
	private static final SavedData.Factory<PasterCompensationData> FACTORY = new SavedData.Factory<>(
			PasterCompensationData::new,
			(tag, registries) -> SavedDataIO.load(CODEC, tag, registries, PasterCompensationData::new),
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Set<String> used;

	public PasterCompensationData() {
		this(Set.of());
	}

	public PasterCompensationData(Set<String> used) {
		this.used = new LinkedHashSet<>(used);
	}

	public static PasterCompensationData getOrCreate(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		return SavedDataIO.save(CODEC, this, registries);
	}

	public boolean hasUsed(UUID playerId) {
		return used.contains(playerId.toString());
	}

	public void markUsed(UUID playerId) {
		if (used.add(playerId.toString())) {
			setDirty();
		}
	}
}
