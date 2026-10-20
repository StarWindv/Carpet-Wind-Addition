package top.starwindv.cwa.noupdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

public final class NoUpdateManager {
	private static final Map<ServerLevel, LevelCache> CACHE = new WeakHashMap<>();
	private static final Map<ServerLevel, List<TemporaryRegion>> TEMPORARY = new ConcurrentHashMap<>();
	private static volatile boolean anyEnabled;
	private static volatile boolean anyTemporary;

	private NoUpdateManager() {
	}

	public static boolean shouldSuppress(LevelReader level, BlockPos pos) {
		if ((!anyEnabled && !anyTemporary) || !(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		if (anyTemporary) {
			List<TemporaryRegion> temporaryRegions = TEMPORARY.get(serverLevel);
			if (temporaryRegions != null) {
				for (TemporaryRegion region : temporaryRegions) {
					if (region.contains(pos)) {
						return true;
					}
				}
			}
		}

		if (!anyEnabled) {
			return false;
		}

		LevelCache cache = CACHE.get(serverLevel);
		if (cache == null) {
			cache = LevelCache.of(NoUpdateData.get(serverLevel));
			CACHE.put(serverLevel, cache);
		}
		return cache.contains(pos);
	}

	public static TemporaryRegion beginTemporary(ServerLevel level, BlockPos first, BlockPos second) {
		TemporaryRegion region = new TemporaryRegion(first, second);
		TEMPORARY.computeIfAbsent(level, ignored -> new CopyOnWriteArrayList<>()).add(region);
		anyTemporary = true;
		return region;
	}

	public static void endTemporary(ServerLevel level, TemporaryRegion region) {
		List<TemporaryRegion> regions = TEMPORARY.get(level);
		if (regions != null) {
			regions.remove(region);
			if (regions.isEmpty()) {
				TEMPORARY.remove(level);
			}
		}
		anyTemporary = !TEMPORARY.isEmpty();
	}

	public static void refresh(MinecraftServer server) {
		CACHE.clear();
		boolean any = false;

		for (ServerLevel level : server.getAllLevels()) {
			LevelCache cache = LevelCache.of(NoUpdateData.get(level));
			CACHE.put(level, cache);
			any |= !cache.isEmpty();
		}

		anyEnabled = any;
	}

	public static void reset() {
		CACHE.clear();
		TEMPORARY.clear();
		anyEnabled = false;
		anyTemporary = false;
	}

	public static final class TemporaryRegion {
		private final BlockPos start;
		private final BlockPos end;

		private TemporaryRegion(BlockPos first, BlockPos second) {
			this.start = new BlockPos(
					Math.min(first.getX(), second.getX()),
					Math.min(first.getY(), second.getY()),
					Math.min(first.getZ(), second.getZ())
			);
			this.end = new BlockPos(
					Math.max(first.getX(), second.getX()),
					Math.max(first.getY(), second.getY()),
					Math.max(first.getZ(), second.getZ())
			);
		}

		private boolean contains(BlockPos pos) {
			return pos.getX() >= start.getX() && pos.getX() <= end.getX()
					&& pos.getY() >= start.getY() && pos.getY() <= end.getY()
					&& pos.getZ() >= start.getZ() && pos.getZ() <= end.getZ();
		}
	}

	private static final class LevelCache {
		private static final LevelCache EMPTY = new LevelCache(new NoUpdateRegion[0], 0, 0, 0, -1, -1, -1);

		private final NoUpdateRegion[] regions;
		private final int minX;
		private final int minY;
		private final int minZ;
		private final int maxX;
		private final int maxY;
		private final int maxZ;

		private LevelCache(NoUpdateRegion[] regions, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			this.regions = regions;
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxY = maxY;
			this.maxZ = maxZ;
		}

		private static LevelCache of(@Nullable NoUpdateData data) {
			if (data == null) {
				return EMPTY;
			}

			List<NoUpdateRegion> enabled = new ArrayList<>();
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;
			int maxZ = Integer.MIN_VALUE;

			for (NoUpdateRegion region : data.regions()) {
				if (!region.enabled()) {
					continue;
				}
				enabled.add(region);
				minX = Math.min(minX, region.start().getX());
				minY = Math.min(minY, region.start().getY());
				minZ = Math.min(minZ, region.start().getZ());
				maxX = Math.max(maxX, region.end().getX());
				maxY = Math.max(maxY, region.end().getY());
				maxZ = Math.max(maxZ, region.end().getZ());
			}

			if (enabled.isEmpty()) {
				return EMPTY;
			}
			return new LevelCache(enabled.toArray(NoUpdateRegion[]::new), minX, minY, minZ, maxX, maxY, maxZ);
		}

		private boolean isEmpty() {
			return regions.length == 0;
		}

		private boolean contains(BlockPos pos) {
			if (regions.length == 0
					|| pos.getX() < minX || pos.getX() > maxX
					|| pos.getY() < minY || pos.getY() > maxY
					|| pos.getZ() < minZ || pos.getZ() > maxZ) {
				return false;
			}

			for (NoUpdateRegion region : regions) {
				if (region.contains(pos)) {
					return true;
				}
			}
			return false;
		}
	}
}
