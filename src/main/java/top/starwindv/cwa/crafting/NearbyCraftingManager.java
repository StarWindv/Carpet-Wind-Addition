package top.starwindv.cwa.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import top.starwindv.cwa.rule.CwaSettings;

public final class NearbyCraftingManager {
	public static final int DEFAULT_SEARCH_SIZE = 8;
	private static final ThreadLocal<NearbyCraftingContext> CURRENT = new ThreadLocal<>();
	private static final Map<AbstractContainerMenu, NearbyCraftingContext> CONTEXTS = new WeakHashMap<>();

	private NearbyCraftingManager() {
	}

	public static int maxSearchSize(MinecraftServer server) {
		if (server == null) {
			return Integer.MAX_VALUE;
		}
		return Math.max(1, server.getPlayerList().getSimulationDistance() * 16);
	}

	public static int searchSize(MinecraftServer server) {
		int configured = CwaSettings.NearbyCraftingSearchSize;
		if (configured <= 0) {
			configured = DEFAULT_SEARCH_SIZE;
		}
		return Math.min(configured, maxSearchSize(server));
	}

	public static void begin(CraftingMenu menu, Inventory inventory) {
		CURRENT.remove();
		if (!CwaSettings.NearbyCrafting || !(inventory.player instanceof ServerPlayer)) {
			return;
		}
		if (!(menu instanceof NearbyCraftingMenu accessMenu)) {
			return;
		}

		ContainerLevelAccess access = accessMenu.cwa$getAccess();
		Optional<TableLocation> location = access.evaluate((level, pos) ->
				new TableLocation(level instanceof ServerLevel serverLevel ? serverLevel : null, pos.immutable()));
		if (location.isEmpty() || location.get().level() == null) {
			return;
		}

		TableLocation table = location.get();
		ServerLevel level = table.level();
		MinecraftServer server = level.getServer();
		int radius = searchSize(server);
		List<ContainerEntry> containers = findContainers(level, table.pos(), radius);
		if (containers.isEmpty()) {
			return;
		}

		NearbyCraftingContext context = CONTEXTS.get(menu);
		if (context == null || !context.matches(level, table.pos(), radius)) {
			context = new NearbyCraftingContext(level, table.pos(), radius, containers);
			CONTEXTS.put(menu, context);
		} else {
			context.setContainers(containers);
		}
		CURRENT.set(context);
	}

	public static void end() {
		CURRENT.remove();
	}

	public static @Nullable NearbyCraftingContext current() {
		return CURRENT.get();
	}

	public static void onMenuRemoved(CraftingMenu menu) {
		NearbyCraftingContext context = CONTEXTS.remove(menu);
		CURRENT.remove();
		if (context != null) {
			context.onMenuRemoved();
		}
	}

	public static void reset() {
		CURRENT.remove();
		CONTEXTS.clear();
	}

	public static List<ContainerEntry> findContainers(ServerLevel level, BlockPos center, int radius) {
		List<ContainerEntry> entries = new ArrayList<>();
		Set<Container> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		BlockPos first = center.offset(-radius, -radius, -radius);
		BlockPos last = center.offset(radius, radius, radius);

		for (BlockPos pos : BlockPos.betweenClosed(first, last)) {
			if (!level.hasChunkAt(pos)) {
				continue;
			}

			BlockState state = level.getBlockState(pos);
			if (!isAllowedContainer(state)) {
				continue;
			}

			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof Container container && seen.add(container)) {
				entries.add(new ContainerEntry(pos.immutable(), container, pos.distSqr(center)));
			}
		}

		entries.sort(Comparator.comparingDouble(ContainerEntry::distanceSqr));
		return List.copyOf(entries);
	}

	private static boolean isAllowedContainer(BlockState state) {
		Block block = state.getBlock();
		return block instanceof ChestBlock
				|| block instanceof BarrelBlock
				|| block instanceof ShulkerBoxBlock;
	}

	public record ContainerEntry(BlockPos pos, Container container, double distanceSqr) {
	}

	private record TableLocation(@Nullable ServerLevel level, BlockPos pos) {
	}
}
