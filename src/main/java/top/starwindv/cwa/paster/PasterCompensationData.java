package top.starwindv.cwa.paster;

import com.mojang.serialization.Codec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import top.starwindv.cwa.CarpetWindAddition;

/**
 * Server-side guard for the one-time paster compensation. The client also keeps
 * a marker across servers; this set prevents reuse on the same server.
 */
public final class PasterCompensationData extends SavedData {
	public static final Codec<PasterCompensationData> CODEC = Codec.STRING.listOf().xmap(
			values -> new PasterCompensationData(new LinkedHashSet<>(values)),
			data -> List.copyOf(data.used)
	);

	public static final SavedDataType<PasterCompensationData> TYPE = new SavedDataType<>(
			CarpetWindAddition.id("paster_compensations"),
			PasterCompensationData::new,
			CODEC,
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
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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
