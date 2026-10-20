package top.starwindv.cwa.paster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import top.starwindv.cwa.noupdate.NoUpdateManager;

public final class PasterManager {
	private static @Nullable ActivePaste activePaste;
	private static volatile Set<String> protectedPlacementIds = Set.of();

	private PasterManager() {
	}

	public static Set<String> protectedPlacementIds() {
		return protectedPlacementIds;
	}

	public static boolean isPasteRunning() {
		return activePaste != null;
	}

	private static void refreshProtectedPlacements(MinecraftServer server) {
		PasterData data = PasterData.get(server);
		protectedPlacementIds = data == null ? Set.of() : Set.copyOf(data.placementIds());
	}

	public static CollectResult collectSelf(ServerPlayer player, PlacementSnapshot snapshot) {
		PasterData data = PasterData.getOrCreate(player.level().getServer());
		List<ItemStack> stored = data.get(snapshot.id());
		int[] remaining = requiredRemaining(snapshot.requiredMaterials(), stored);
		int[] before = remaining.clone();
		List<ItemStack> collected = new ArrayList<>();
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			takeFromSlot(inventory, slot, snapshot.requiredMaterials(), remaining, collected);
		}
		takeFromSlot(inventory, Inventory.SLOT_OFFHAND, snapshot.requiredMaterials(), remaining, collected);

		if (!collected.isEmpty()) {
			for (ItemStack stack : collected) {
				PasterMaterials.add(stored, stack);
			}
			data.set(snapshot.id(), stored);
			player.inventoryMenu.broadcastChanges();
		}
		MinecraftServer server = player.level().getServer();
		if (server != null) {
			refreshProtectedPlacements(server);
		}

		return new CollectResult(
				total(collected),
				missingCount(remaining),
				collectedMaterials(snapshot.requiredMaterials(), before, remaining)
		);
	}

	public static CollectResult collectRange(
			ServerLevel level,
			ServerPlayer player,
			PlacementSnapshot snapshot,
			BlockPos first,
			BlockPos second
	) {
		PasterData data = PasterData.getOrCreate(level.getServer());
		List<ItemStack> stored = data.get(snapshot.id());
		int[] remaining = requiredRemaining(snapshot.requiredMaterials(), stored);
		int[] before = remaining.clone();
		List<ItemStack> collected = new ArrayList<>();
		Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());

		for (BlockPos pos : BlockPos.betweenClosed(first, second)) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof Container container) || !visited.add(container)) {
				continue;
			}

			boolean changed = false;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (takeFromContainerSlot(container, slot, snapshot.requiredMaterials(), remaining, collected)) {
					changed = true;
				}
			}
			if (changed) {
				container.setChanged();
			}
		}

		if (!collected.isEmpty()) {
			for (ItemStack stack : collected) {
				PasterMaterials.add(stored, stack);
			}
			data.set(snapshot.id(), stored);
		}
		refreshProtectedPlacements(level.getServer());

		return new CollectResult(
				total(collected),
				missingCount(remaining),
				collectedMaterials(snapshot.requiredMaterials(), before, remaining)
		);
	}

	private static List<CollectedMaterial> collectedMaterials(
			List<PasterMaterial> required,
			int[] before,
			int[] after
	) {
		List<CollectedMaterial> collected = new ArrayList<>();
		for (int index = 0; index < required.size(); index++) {
			int amount = before[index] - after[index];
			if (amount > 0) {
				collected.add(new CollectedMaterial(required.get(index).sample(), amount, after[index]));
			}
		}
		return List.copyOf(collected);
	}

	private static void takeFromSlot(
			Inventory inventory,
			int slot,
			List<PasterMaterial> required,
			int[] remaining,
			List<ItemStack> collected
	) {
		ItemStack slotStack = inventory.getItem(slot);
		if (slotStack.isEmpty()) {
			return;
		}

		boolean changed = false;
		for (int i = 0; i < required.size(); i++) {
			if (remaining[i] <= 0) {
				continue;
			}

			PasterMaterial material = required.get(i);
			if (!ItemStack.isSameItemSameComponents(slotStack, material.sample())) {
				continue;
			}

			int amount = Math.min(remaining[i], slotStack.getCount());
			if (amount <= 0) {
				continue;
			}

			ItemStack taken = slotStack.split(amount);
			remaining[i] -= amount;
			collected.add(taken);
			changed = true;
		}

		if (!isRequired(required, remaining, slotStack)
				&& takeFromNestedContainer(slotStack, required, remaining, collected)) {
			changed = true;
		}

		if (changed) {
			inventory.setItem(slot, slotStack);
			inventory.setChanged();
		}
	}

	private static boolean takeFromContainerSlot(
			Container container,
			int slot,
			List<PasterMaterial> required,
			int[] remaining,
			List<ItemStack> collected
	) {
		ItemStack slotStack = container.getItem(slot);
		if (slotStack.isEmpty()) {
			return false;
		}

		boolean changed = false;
		for (int i = 0; i < required.size(); i++) {
			if (remaining[i] <= 0) {
				continue;
			}

			PasterMaterial material = required.get(i);
			if (!ItemStack.isSameItemSameComponents(slotStack, material.sample())) {
				continue;
			}

			int amount = Math.min(remaining[i], slotStack.getCount());
			if (amount <= 0) {
				continue;
			}

			ItemStack taken = slotStack.split(amount);
			remaining[i] -= amount;
			collected.add(taken);
			changed = true;
		}

		if (!isRequired(required, remaining, slotStack)
				&& takeFromNestedContainer(slotStack, required, remaining, collected)) {
			changed = true;
		}

		if (changed) {
			container.setItem(slot, slotStack);
		}
		return changed;
	}

	private static boolean takeFromNestedContainer(
			ItemStack outerStack,
			List<PasterMaterial> required,
			int[] remaining,
			List<ItemStack> collected
	) {
		ItemContainerContents contents = outerStack.get(DataComponents.CONTAINER);
		if (contents == null || contents == ItemContainerContents.EMPTY) {
			return false;
		}

		List<ItemStack> nested = new ArrayList<>(contents.allItemsCopyStream().toList());
		boolean changed = false;
		for (int index = 0; index < nested.size(); index++) {
			ItemStack nestedStack = nested.get(index);
			if (nestedStack.isEmpty()) {
				continue;
			}

			for (int requiredIndex = 0; requiredIndex < required.size(); requiredIndex++) {
				if (remaining[requiredIndex] <= 0) {
					continue;
				}

				PasterMaterial material = required.get(requiredIndex);
				if (!ItemStack.isSameItemSameComponents(nestedStack, material.sample())) {
					continue;
				}

				int amount = Math.min(remaining[requiredIndex], nestedStack.getCount());
				if (amount <= 0) {
					continue;
				}

				ItemStack taken = nestedStack.split(amount);
				remaining[requiredIndex] -= amount;
				collected.add(taken);
				changed = true;
			}
		}

		if (changed) {
			ItemContainerContents updated = ItemContainerContents.fromItems(nested);
			if (updated == ItemContainerContents.EMPTY) {
				outerStack.remove(DataComponents.CONTAINER);
			} else {
				outerStack.set(DataComponents.CONTAINER, updated);
			}
		}
		return changed;
	}

	private static boolean isRequired(List<PasterMaterial> required, int[] remaining, ItemStack stack) {
		for (int index = 0; index < required.size(); index++) {
			if (remaining[index] > 0 && ItemStack.isSameItemSameComponents(required.get(index).sample(), stack)) {
				return true;
			}
		}
		return false;
	}

	public static ReturnResult returnSelf(ServerPlayer player, PlacementSnapshot snapshot) {
		PasterData data = PasterData.getOrCreate(player.level().getServer());
		List<ItemStack> stored = data.get(snapshot.id());
		int before = total(stored);
		List<ItemStack> remaining = new ArrayList<>();

		Inventory inventory = player.getInventory();
		for (ItemStack stack : stored) {
			ItemStack leftover = insertIntoInventory(inventory, stack);
			if (!leftover.isEmpty()) {
				remaining.add(leftover);
			}
		}

		data.set(snapshot.id(), remaining);
		player.inventoryMenu.broadcastChanges();
		MinecraftServer server = player.level().getServer();
		if (server != null) {
			refreshProtectedPlacements(server);
		}
		return new ReturnResult(before - total(remaining), total(remaining));
	}

	public static ReturnResult returnRange(
			ServerLevel level,
			PlacementSnapshot snapshot,
			BlockPos first,
			BlockPos second
	) {
		PasterData data = PasterData.getOrCreate(level.getServer());
		List<ItemStack> stored = data.get(snapshot.id());
		int before = total(stored);
		List<ItemStack> remaining = new ArrayList<>();

		for (ItemStack stack : stored) {
			ItemStack leftover = insertIntoContainers(level, first, second, stack);
			if (!leftover.isEmpty()) {
				remaining.add(leftover);
			}
		}

		data.set(snapshot.id(), remaining);
		refreshProtectedPlacements(level.getServer());
		return new ReturnResult(before - total(remaining), total(remaining));
	}

	private static ItemStack insertIntoInventory(Inventory inventory, ItemStack stack) {
		ItemStack remaining = stack.copy();
		boolean changed = false;

		for (int pass = 0; pass < 2; pass++) {
			int slots = pass == 0 ? Inventory.INVENTORY_SIZE : 1;
			for (int index = 0; index < slots && !remaining.isEmpty(); index++) {
				int slot = pass == 0 ? index : Inventory.SLOT_OFFHAND;
				ItemStack existing = inventory.getItem(slot);
				if (existing.isEmpty()) {
					int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
					inventory.setItem(slot, remaining.split(moved));
					changed = true;
				} else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
					int space = existing.getMaxStackSize() - existing.getCount();
					if (space > 0) {
						int moved = Math.min(space, remaining.getCount());
						existing.grow(moved);
						remaining.shrink(moved);
						inventory.setItem(slot, existing);
						changed = true;
					}
				}
			}
		}

		if (changed) {
			inventory.setChanged();
		}
		return remaining;
	}

	private static ItemStack insertIntoContainers(
			ServerLevel level,
			BlockPos first,
			BlockPos second,
			ItemStack stack
	) {
		ItemStack remaining = stack.copy();
		Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());

		for (BlockPos pos : BlockPos.betweenClosed(first, second)) {
			if (remaining.isEmpty()) {
				break;
			}

			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof Container container) || !visited.add(container)) {
				continue;
			}

			boolean changed = false;
			for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
				if (!container.canPlaceItem(slot, remaining)) {
					continue;
				}

				ItemStack existing = container.getItem(slot);
				if (existing.isEmpty()) {
					int moved = Math.min(remaining.getCount(), container.getMaxStackSize(remaining));
					container.setItem(slot, remaining.split(moved));
					changed = true;
				} else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
					int space = container.getMaxStackSize(remaining) - existing.getCount();
					if (space > 0) {
						int moved = Math.min(space, remaining.getCount());
						existing.grow(moved);
						remaining.shrink(moved);
						container.setItem(slot, existing);
						changed = true;
					}
				}
			}
			if (changed) {
				container.setChanged();
			}
		}
		return remaining;
	}

	public static boolean hasMaterials(PlacementSnapshot snapshot, List<ItemStack> stored) {
		int[] remaining = requiredRemaining(snapshot.requiredMaterials(), stored);
		return missingCount(remaining) == 0;
	}

	public static List<PasterMaterial> missingMaterials(PlacementSnapshot snapshot, List<ItemStack> stored) {
		int[] remaining = requiredRemaining(snapshot.requiredMaterials(), stored);
		List<PasterMaterial> missing = new ArrayList<>();
		for (int i = 0; i < remaining.length; i++) {
			if (remaining[i] > 0) {
				PasterMaterial material = snapshot.requiredMaterials().get(i);
				missing.add(new PasterMaterial(material.sample(), remaining[i]));
			}
		}
		return missing;
	}

	public static boolean startPaste(CommandSourceStack source, ServerPlayer player, PlacementSnapshot snapshot) {
		MinecraftServer server = source.getServer();
		if (activePaste != null) {
			source.sendFailure(Component.translatable("cwa.paster.paste.busy"));
			return false;
		}

		PasterData data = PasterData.getOrCreate(server);
		List<PasterMaterial> missing = missingMaterials(snapshot, data.get(snapshot.id()));
		if (!missing.isEmpty()) {
			PasterMaterial first = missing.get(0);
			source.sendFailure(Component.translatable(
					"cwa.paster.paste.missing",
					first.sample().getHoverName(),
					first.count()
			));
			return false;
		}

		if (LitematicaBridge.hasPasteTask()) {
			source.sendFailure(Component.translatable("cwa.paster.paste.busy"));
			return false;
		}

		ServerLevel level = source.getLevel();
		List<NoUpdateManager.TemporaryRegion> temporaryRegions = new ArrayList<>();
		for (PasterBounds bounds : snapshot.bounds()) {
			temporaryRegions.add(NoUpdateManager.beginTemporary(level, bounds.start(), bounds.end()));
		}

		ActivePaste session = new ActivePaste(
				server,
				player.getUUID(),
				snapshot.id(),
				snapshot.requiredMaterials(),
				level,
				temporaryRegions,
				server.getTickCount()
		);
		activePaste = session;

		Runnable failure = () -> server.execute(() -> failPaste(server, session));
		if (!LitematicaBridge.startPaste(
				snapshot.placement(),
				success -> server.execute(() -> {
					session.completionResult = success;
					finishPaste(server, session, success);
				}),
				failure
		)) {
			failPaste(server, session);
			source.sendFailure(Component.translatable("cwa.paster.paste.failed"));
			return false;
		}

		source.sendSuccess(() -> Component.translatable(
				"cwa.paster.paste.started",
				snapshot.name()
		), false);
		return true;
	}

	public static void tick(MinecraftServer server) {
		ActivePaste session = activePaste;
		if (session == null || session.server != server) {
			return;
		}

		boolean hasTask = LitematicaBridge.hasPasteTask();
		if (hasTask) {
			session.seen = true;
			session.notRunningSince = -1;
		}

		if (session.seen && !hasTask) {
			if (session.completionResult != null) {
				finishPaste(server, session, session.completionResult);
			} else {
				if (session.notRunningSince < 0) {
					session.notRunningSince = server.getTickCount();
				} else if (server.getTickCount() - session.notRunningSince > 60) {
					finishPaste(server, session, true);
				}
			}
		} else if (!session.seen && server.getTickCount() - session.startTick > 40) {
			finishPaste(server, session, false);
		}
	}

	private static void failPaste(MinecraftServer server, ActivePaste session) {
		finishPaste(server, session, false);
	}

	private static void finishPaste(MinecraftServer server, ActivePaste session, boolean success) {
		if (activePaste != session) {
			return;
		}

		for (NoUpdateManager.TemporaryRegion region : session.temporaryRegions) {
			NoUpdateManager.endTemporary(session.level, region);
		}
		activePaste = null;

		if (success) {
			consumePastedMaterials(server, session.placementId, session.consumedMaterials);
		}

		ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
		if (player != null) {
			player.sendSystemMessage(Component.translatable(
					success ? "cwa.paster.paste.finished" : "cwa.paster.paste.failed"
			));
		}
	}

	private static void consumePastedMaterials(
			MinecraftServer server,
			UUID placementId,
			List<PasterMaterial> materials
	) {
		PasterData data = PasterData.get(server);
		if (data == null) {
			return;
		}

		List<ItemStack> stored = data.get(placementId);
		for (PasterMaterial material : materials) {
			PasterMaterials.take(stored, material.sample(), material.count());
		}
		data.set(placementId, stored);
		refreshProtectedPlacements(server);
	}

	public static void onServerLoaded(MinecraftServer server) {
		refreshProtectedPlacements(server);
	}

	public static void onServerClosed(MinecraftServer server) {
		ActivePaste session = activePaste;
		if (session != null && session.server == server) {
			for (NoUpdateManager.TemporaryRegion region : session.temporaryRegions) {
				NoUpdateManager.endTemporary(session.level, region);
			}
			activePaste = null;
		}
		protectedPlacementIds = Set.of();
	}

	private static int[] requiredRemaining(List<PasterMaterial> required, List<ItemStack> stored) {
		int[] remaining = new int[required.size()];
		for (int i = 0; i < required.size(); i++) {
			PasterMaterial material = required.get(i);
			remaining[i] = Math.max(0, material.count() - PasterMaterials.count(stored, material.sample()));
		}
		return remaining;
	}

	private static int missingCount(int[] remaining) {
		int missing = 0;
		for (int count : remaining) {
			missing += count;
		}
		return missing;
	}

	private static int total(List<ItemStack> stacks) {
		return PasterMaterials.totalCount(stacks);
	}

	public record CollectedMaterial(ItemStack sample, int collected, int missing) {
	}

	public record CollectResult(int collected, int missing, List<CollectedMaterial> materials) {
	}

	public record ReturnResult(int returned, int remaining) {
	}

	private static final class ActivePaste {
		private final MinecraftServer server;
		private final UUID playerId;
		private final UUID placementId;
		private final List<PasterMaterial> consumedMaterials;
		private final ServerLevel level;
		private final List<NoUpdateManager.TemporaryRegion> temporaryRegions;
		private final int startTick;
		private boolean seen;
		private int notRunningSince = -1;
		private @Nullable Boolean completionResult;

		private ActivePaste(
				MinecraftServer server,
				UUID playerId,
				UUID placementId,
				List<PasterMaterial> consumedMaterials,
				ServerLevel level,
				List<NoUpdateManager.TemporaryRegion> temporaryRegions,
				int startTick
		) {
			this.server = server;
			this.playerId = playerId;
			this.placementId = placementId;
			this.consumedMaterials = List.copyOf(consumedMaterials);
			this.level = level;
			this.temporaryRegions = temporaryRegions;
			this.startTick = startTick;
		}
	}

}
