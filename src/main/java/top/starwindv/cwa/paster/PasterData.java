package top.starwindv.cwa.paster;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import top.starwindv.cwa.util.SavedDataIO;

public final class PasterData extends SavedData {
	private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(Entry::stacks),
			Codec.STRING.optionalFieldOf("fingerprint", "").forGetter(Entry::fingerprint)
	).apply(instance, Entry::new));

	/**
	 * Older builds stored the entry as a bare ItemStack list. Accept both layouts
	 * so existing worlds keep their stored materials.
	 */
	private static final Codec<Entry> ENTRY_OR_LIST = Codec.either(ItemStack.CODEC.listOf(), ENTRY_CODEC)
			.xmap(
					either -> either.map(stacks -> new Entry(stacks, ""), entry -> entry),
					Either::right
			);

	public static final Codec<PasterData> CODEC = Codec.unboundedMap(Codec.STRING, ENTRY_OR_LIST)
			.xmap(PasterData::new, PasterData::entries);

	private static final String DATA_NAME = "cwa_paster_materials";
	private static final SavedData.Factory<PasterData> FACTORY = new SavedData.Factory<>(
			PasterData::new,
			(tag, registries) -> SavedDataIO.load(CODEC, tag, registries, PasterData::new),
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<String, Entry> entries;

	public PasterData() {
		this(Map.of());
	}

	public PasterData(Map<String, Entry> entries) {
		this.entries = new LinkedHashMap<>();
		entries.forEach((placement, entry) -> {
			List<ItemStack> copied = copyStacks(entry.stacks());
			if (!copied.isEmpty()) {
				this.entries.put(placement, new Entry(copied, entry.fingerprint()));
			}
		});
	}

	public static @Nullable PasterData get(MinecraftServer server) {
		return server.overworld().getDataStorage().get(FACTORY, DATA_NAME);
	}

	public static PasterData getOrCreate(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		return SavedDataIO.save(CODEC, this, registries);
	}

	public Map<String, Entry> entries() {
		Map<String, Entry> copy = new LinkedHashMap<>();
		entries.forEach((placement, entry) -> copy.put(
				placement,
				new Entry(copyStacks(entry.stacks()), entry.fingerprint())
		));
		return Collections.unmodifiableMap(copy);
	}

	public List<ItemStack> get(UUID playerId, UUID placementId) {
		Entry stored = getEntry(playerId, placementId);
		return copyStacks(stored == null ? null : stored.stacks());
	}

	public String fingerprint(UUID playerId, UUID placementId) {
		Entry stored = getEntry(playerId, placementId);
		return stored == null ? "" : stored.fingerprint();
	}

	public void set(UUID playerId, UUID placementId, List<ItemStack> stacks) {
		set(playerId, placementId, stacks, fingerprint(playerId, placementId));
	}

	public void set(UUID playerId, UUID placementId, List<ItemStack> stacks, String fingerprint) {
		String key = key(playerId, placementId);
		List<ItemStack> copied = copyStacks(stacks);
		String normalizedFingerprint = fingerprint == null ? "" : fingerprint;
		boolean changed = entries.remove(placementId.toString()) != null;
		if (copied.isEmpty()) {
			changed |= entries.remove(key) != null;
		} else {
			entries.put(key, new Entry(copied, normalizedFingerprint));
			changed = true;
		}
		if (changed) {
			setDirty();
		}
	}

	public boolean hasAny() {
		return !entries.isEmpty();
	}

	public Set<String> placementIds() {
		Set<String> ids = new java.util.HashSet<>();
		for (String key : entries.keySet()) {
			ids.add(placementIdFromKey(key));
		}
		return Set.copyOf(ids);
	}

	public Map<String, String> fingerprints() {
		Map<String, String> fingerprints = new LinkedHashMap<>();
		entries.forEach((key, entry) -> fingerprints.put(placementIdFromKey(key), entry.fingerprint()));
		return Collections.unmodifiableMap(fingerprints);
	}

	private @Nullable Entry getEntry(UUID playerId, UUID placementId) {
		Entry stored = entries.get(key(playerId, placementId));
		if (stored == null) {
			// Older builds stored only the placement UUID. Keep those worlds readable;
			// the next set() migrates the entry to the player-scoped key.
			stored = entries.get(placementId.toString());
		}
		return stored;
	}

	private static String key(UUID playerId, UUID placementId) {
		return playerId + "/" + placementId;
	}

	private static String placementIdFromKey(String key) {
		int separator = key.lastIndexOf('/');
		return separator >= 0 ? key.substring(separator + 1) : key;
	}

	private static List<ItemStack> copyStacks(@Nullable List<ItemStack> stacks) {
		if (stacks == null || stacks.isEmpty()) {
			return new ArrayList<>();
		}

		List<ItemStack> copy = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				copy.add(stack.copy());
			}
		}
		return copy;
	}

	public record Entry(List<ItemStack> stacks, String fingerprint) {
		public Entry {
			stacks = List.copyOf(stacks);
			fingerprint = fingerprint == null ? "" : fingerprint;
		}
	}
}
