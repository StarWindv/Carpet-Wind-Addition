package top.starwindv.cwa.paster;

import com.mojang.serialization.Codec;
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
	public static final Codec<PasterData> CODEC = Codec.unboundedMap(Codec.STRING, ItemStack.CODEC.listOf())
			.xmap(PasterData::new, PasterData::entries);

	private static final String DATA_NAME = "cwa_paster_materials";
	private static final SavedData.Factory<PasterData> FACTORY = new SavedData.Factory<>(
			PasterData::new,
			(tag, registries) -> SavedDataIO.load(CODEC, tag, registries, PasterData::new),
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<String, List<ItemStack>> entries;

	public PasterData() {
		this(Map.of());
	}

	public PasterData(Map<String, List<ItemStack>> entries) {
		this.entries = new LinkedHashMap<>();
		entries.forEach((placement, stacks) -> {
			List<ItemStack> copied = copyStacks(stacks);
			if (!copied.isEmpty()) {
				this.entries.put(placement, copied);
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

	public Map<String, List<ItemStack>> entries() {
		Map<String, List<ItemStack>> copy = new LinkedHashMap<>();
		entries.forEach((placement, stacks) -> copy.put(placement, copyStacks(stacks)));
		return Collections.unmodifiableMap(copy);
	}

	public List<ItemStack> get(UUID placementId) {
		return copyStacks(entries.get(placementId.toString()));
	}

	public void set(UUID placementId, List<ItemStack> stacks) {
		List<ItemStack> copied = copyStacks(stacks);
		if (copied.isEmpty()) {
			if (entries.remove(placementId.toString()) != null) {
				setDirty();
			}
		} else {
			entries.put(placementId.toString(), copied);
			setDirty();
		}
	}

	public boolean hasAny() {
		return !entries.isEmpty();
	}

	public Set<String> placementIds() {
		return Set.copyOf(entries.keySet());
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
}
