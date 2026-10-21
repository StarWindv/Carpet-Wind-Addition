package top.starwindv.cwa.noupdate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import top.starwindv.cwa.util.SavedDataIO;

public final class NoUpdateData extends SavedData {
	public static final Codec<NoUpdateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			NoUpdateRegion.CODEC.listOf().optionalFieldOf("regions", List.of()).forGetter(NoUpdateData::regions)
	).apply(instance, NoUpdateData::new));

	private static final String DATA_NAME = "cwa_no_update_regions";
	private static final SavedData.Factory<NoUpdateData> FACTORY = new SavedData.Factory<>(
			NoUpdateData::new,
			(tag, registries) -> SavedDataIO.load(CODEC, tag, registries, NoUpdateData::new),
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final List<NoUpdateRegion> regions;

	public NoUpdateData() {
		this(List.of());
	}

	public NoUpdateData(List<NoUpdateRegion> regions) {
		this.regions = new ArrayList<>(regions);
	}

	public static @Nullable NoUpdateData get(ServerLevel level) {
		return level.getDataStorage().get(FACTORY, DATA_NAME);
	}

	public static NoUpdateData getOrCreate(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		return SavedDataIO.save(CODEC, this, registries);
	}

	public List<NoUpdateRegion> regions() {
		return Collections.unmodifiableList(regions);
	}

	public NoUpdateRegion addRegion(String alias, BlockPos first, BlockPos second) {
		int id = allocateId(alias);
		NoUpdateRegion region = new NoUpdateRegion(id, alias, false, first, second);
		regions.add(region);
		setDirty();
		return region;
	}

	private int allocateId(String alias) {
		int candidate = 1;

		if (alias.isEmpty()) {
			for (NoUpdateRegion region : regions) {
				if (region.alias().isEmpty()) {
					candidate = Math.max(candidate, region.id() + 1);
				}
			}
		} else {
			for (NoUpdateRegion region : regions) {
				candidate = Math.max(candidate, region.id() + 1);
			}
		}

		while (containsId(candidate)) {
			candidate++;
		}
		return candidate;
	}

	private boolean containsId(int id) {
		for (NoUpdateRegion region : regions) {
			if (region.id() == id) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAlias(String alias) {
		if (alias.isEmpty()) {
			return false;
		}
		return regions.stream().anyMatch(region -> region.alias().equalsIgnoreCase(alias));
	}

	public NoUpdateRegion findOverlapping(BlockPos first, BlockPos second) {
		int minX = Math.min(first.getX(), second.getX());
		int minY = Math.min(first.getY(), second.getY());
		int minZ = Math.min(first.getZ(), second.getZ());
		int maxX = Math.max(first.getX(), second.getX());
		int maxY = Math.max(first.getY(), second.getY());
		int maxZ = Math.max(first.getZ(), second.getZ());

		for (NoUpdateRegion region : regions) {
			boolean overlapsX = minX <= region.end().getX() && maxX >= region.start().getX();
			boolean overlapsY = minY <= region.end().getY() && maxY >= region.start().getY();
			boolean overlapsZ = minZ <= region.end().getZ() && maxZ >= region.start().getZ();
			if (overlapsX && overlapsY && overlapsZ) {
				return region;
			}
		}
		return null;
	}

	public NoUpdateRegion find(String target) {
		try {
			int id = Integer.parseInt(target);
			for (NoUpdateRegion region : regions) {
				if (region.id() == id) {
					return region;
				}
			}
			return null;
		} catch (NumberFormatException ignored) {
			// Not an id, try an alias below.
		}

		for (NoUpdateRegion region : regions) {
			if (!region.alias().isEmpty() && region.alias().equalsIgnoreCase(target)) {
				return region;
			}
		}
		return null;
	}

	public void setEnabled(NoUpdateRegion region, boolean enabled) {
		for (int i = 0; i < regions.size(); i++) {
			if (regions.get(i).id() == region.id()) {
				regions.set(i, region.withEnabled(enabled));
				setDirty();
				return;
			}
		}
	}

	public void remove(NoUpdateRegion region) {
		if (regions.removeIf(candidate -> candidate.id() == region.id())) {
			setDirty();
		}
	}

	public boolean isSuppressed(BlockPos pos) {
		for (NoUpdateRegion region : regions) {
			if (region.enabled() && region.contains(pos)) {
				return true;
			}
		}
		return false;
	}
}
