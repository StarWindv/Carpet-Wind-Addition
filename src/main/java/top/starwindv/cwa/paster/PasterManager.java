package top.starwindv.cwa.paster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import top.starwindv.cwa.CarpetWindAddition;
import top.starwindv.cwa.noupdate.NoUpdateManager;
import top.starwindv.cwa.util.CwaMessages;
import top.starwindv.cwa.util.ItemTransfer;

public final class PasterManager {
	private static final Map<UUID, ActivePaste> ACTIVE_PASTES = new ConcurrentHashMap<>();
	private static volatile Map<String, String> protectedPlacements = Map.of();

	private PasterManager() {
	}

	public static Map<String, String> protectedPlacements() {
		return protectedPlacements;
	}

	public static boolean isPasteRunning() {
		return !ACTIVE_PASTES.isEmpty();
	}

	public static boolean isPasteRunning(UUID playerId) {
		return ACTIVE_PASTES.containsKey(playerId);
	}

	private static void refreshProtectedPlacements(MinecraftServer server) {
		PasterData data = PasterData.get(server);
		protectedPlacements = data == null ? Map.of() : data.fingerprints();
	}

	public static CollectResult collectSelf(ServerPlayer player, PlacementSnapshot snapshot) {
		PasterData data = PasterData.getOrCreate(player.level().getServer());
		List<ItemStack> stored = data.get(player.getUUID(), snapshot.id());
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
			data.set(player.getUUID(), snapshot.id(), stored, snapshot.fingerprint());
			player.inventoryMenu.broadcastChanges();
		}
		logUncollectedPlayerMaterials(player, snapshot, remaining);
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

	private static void logUncollectedPlayerMaterials(
			ServerPlayer player,
			PlacementSnapshot snapshot,
			int[] remaining
	) {
		if (!hasRemaining(remaining)) {
			return;
		}

		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			logUncollectedStack(
					inventory.getItem(slot),
					"inventory slot " + slot,
					snapshot.requiredMaterials(),
					remaining
			);
		}
		logUncollectedStack(
				inventory.getItem(Inventory.SLOT_OFFHAND),
				"offhand",
				snapshot.requiredMaterials(),
				remaining
		);
		for (int slot = 1; slot <= 4; slot++) {
			logUncollectedStack(
					player.inventoryMenu.getSlot(slot).getItem(),
					"crafting slot " + slot,
					snapshot.requiredMaterials(),
					remaining
			);
		}
	}

	private static void logUncollectedContainer(
			Container container,
			String where,
			List<PasterMaterial> required,
			int[] remaining
	) {
		if (!hasRemaining(remaining)) {
			return;
		}

		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			logUncollectedStack(container.getItem(slot), where + " slot " + slot, required, remaining);
		}
	}

	private static void logUncollectedStack(
			ItemStack stack,
			String where,
			List<PasterMaterial> required,
			int[] remaining
	) {
		if (stack.isEmpty()) {
			return;
		}

		for (int index = 0; index < required.size(); index++) {
			if (remaining[index] <= 0) {
				continue;
			}

			PasterMaterial material = required.get(index);
			ItemStack sample = material.sample();
			if (!stack.is(sample.getItem())) {
				continue;
			}

			CarpetWindAddition.LOGGER.info(
					"Paster uncollected same item: {} item={} count={} matches={} stackPatch={} samplePatch={} remaining={}",
					where,
					stack.getItem(),
					stack.getCount(),
					PasterMaterials.matches(stack, sample),
					stack.getComponentsPatch(),
					sample.getComponentsPatch(),
					remaining[index]
			);
		}
	}

	private static boolean hasRemaining(int[] remaining) {
		for (int value : remaining) {
			if (value > 0) {
				return true;
			}
		}
		return false;
	}

	public static CollectResult collectRange(
			ServerLevel level,
			ServerPlayer player,
			PlacementSnapshot snapshot,
			BlockPos first,
			BlockPos second
	) {
		PasterData data = PasterData.getOrCreate(level.getServer());
		List<ItemStack> stored = data.get(player.getUUID(), snapshot.id());
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
			logUncollectedContainer(
					container,
					"container " + pos.toShortString(),
					snapshot.requiredMaterials(),
					remaining
			);
		}

		if (!collected.isEmpty()) {
			for (ItemStack stack : collected) {
				PasterMaterials.add(stored, stack);
			}
			data.set(player.getUUID(), snapshot.id(), stored, snapshot.fingerprint());
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
			if (!PasterMaterials.matches(slotStack, material.sample())) {
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
			if (!PasterMaterials.matches(slotStack, material.sample())) {
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

		List<ItemStack> nested = new ArrayList<>();
		contents.nonEmptyItemsCopy().forEach(nested::add);
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
				if (!PasterMaterials.matches(nestedStack, material.sample())) {
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
			if (remaining[index] > 0 && PasterMaterials.matches(required.get(index).sample(), stack)) {
				return true;
			}
		}
		return false;
	}

	public static ReturnOrphanedResult returnOrphanedSelf(
			ServerPlayer player,
			Set<String> activePlacementIds
	) {
		PasterData data = PasterData.getOrCreate(player.level().getServer());
		Inventory inventory = player.getInventory();
		int returned = 0;
		int remaining = 0;
		int placements = 0;

		for (String key : List.copyOf(data.entries().keySet())) {
			UUID id = orphanedPlacementId(player, key, activePlacementIds);
			if (id == null) {
				continue;
			}

			List<ItemStack> stored = data.get(player.getUUID(), id);
			if (stored.isEmpty()) {
				continue;
			}

			placements++;
			int before = total(stored);
			List<ItemStack> leftover = new ArrayList<>();
			for (ItemStack stack : stored) {
				ItemStack rest = ItemTransfer.insertIntoInventory(inventory, stack);
				if (!rest.isEmpty()) {
					leftover.add(rest);
				}
			}
			data.set(player.getUUID(), id, leftover);
			returned += before - total(leftover);
			remaining += total(leftover);
		}

		player.inventoryMenu.broadcastChanges();
		refreshProtectedPlacements(player.level().getServer());
		return new ReturnOrphanedResult(returned, remaining, placements);
	}

	public static ReturnOrphanedResult returnOrphanedRange(
			ServerLevel level,
			ServerPlayer player,
			Set<String> activePlacementIds,
			BlockPos first,
			BlockPos second
	) {
		PasterData data = PasterData.getOrCreate(level.getServer());
		List<ItemTransfer.ContainerEntry> containers = ItemTransfer.findContainerEntries(
				level,
				first,
				second,
				container -> true
		);
		int returned = 0;
		int remaining = 0;
		int placements = 0;

		for (String key : List.copyOf(data.entries().keySet())) {
			UUID id = orphanedPlacementId(player, key, activePlacementIds);
			if (id == null) {
				continue;
			}

			List<ItemStack> stored = data.get(player.getUUID(), id);
			if (stored.isEmpty()) {
				continue;
			}

			placements++;
			int before = total(stored);
			List<ItemStack> leftover = new ArrayList<>();
			for (ItemStack stack : stored) {
				ItemStack remainingStack = stack.copy();
				for (ItemTransfer.ContainerEntry entry : containers) {
					if (remainingStack.isEmpty()) {
						break;
					}
					remainingStack = ItemTransfer.insertIntoContainer(entry.container(), remainingStack);
				}
				if (!remainingStack.isEmpty()) {
					leftover.add(remainingStack);
				}
			}
			data.set(player.getUUID(), id, leftover);
			returned += before - total(leftover);
			remaining += total(leftover);
		}

		refreshProtectedPlacements(level.getServer());
		return new ReturnOrphanedResult(returned, remaining, placements);
	}

	private static @Nullable UUID orphanedPlacementId(
			ServerPlayer player,
			String key,
			Set<String> activePlacementIds
	) {
		String prefix = player.getUUID() + "/";
		if (!key.startsWith(prefix)) {
			return null;
		}

		String placementId = key.substring(prefix.length());
		try {
			UUID id = UUID.fromString(placementId);
			return activePlacementIds.contains(placementId) ? null : id;
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public static ReturnResult returnSelf(ServerPlayer player, PlacementSnapshot snapshot) {
		PasterData data = PasterData.getOrCreate(player.level().getServer());
		List<ItemStack> stored = data.get(player.getUUID(), snapshot.id());
		int before = total(stored);
		List<ItemStack> remaining = new ArrayList<>();

		Inventory inventory = player.getInventory();
		for (ItemStack stack : stored) {
			ItemStack leftover = ItemTransfer.insertIntoInventory(inventory, stack);
			if (!leftover.isEmpty()) {
				remaining.add(leftover);
			}
		}

		data.set(player.getUUID(), snapshot.id(), remaining);
		player.inventoryMenu.broadcastChanges();
		MinecraftServer server = player.level().getServer();
		if (server != null) {
			refreshProtectedPlacements(server);
		}
		return new ReturnResult(before - total(remaining), total(remaining), List.of());
	}

	public static ReturnResult returnRange(
			ServerLevel level,
			UUID playerId,
			PlacementSnapshot snapshot,
			BlockPos first,
			BlockPos second
	) {
		PasterData data = PasterData.getOrCreate(level.getServer());
		List<ItemStack> stored = data.get(playerId, snapshot.id());
		int before = total(stored);
		List<ItemStack> remaining = new ArrayList<>();
		List<ItemTransfer.ContainerEntry> entries = stored.isEmpty()
				? List.of()
				: ItemTransfer.findContainerEntries(level, first, second, container -> true);
		Set<BlockPos> filled = new LinkedHashSet<>();

		for (ItemStack stack : stored) {
			ItemStack remainingStack = stack.copy();
			for (ItemTransfer.ContainerEntry entry : entries) {
				if (remainingStack.isEmpty()) {
					break;
				}

				int beforeCount = remainingStack.getCount();
				remainingStack = ItemTransfer.insertIntoContainer(entry.container(), remainingStack);
				if (remainingStack.getCount() < beforeCount) {
					filled.add(entry.pos());
				}
			}
			if (!remainingStack.isEmpty()) {
				remaining.add(remainingStack);
			}
		}

		data.set(playerId, snapshot.id(), remaining);
		refreshProtectedPlacements(level.getServer());
		return new ReturnResult(before - total(remaining), total(remaining), List.copyOf(filled));
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
		if (ACTIVE_PASTES.containsKey(player.getUUID())) {
			source.sendFailure(CwaMessages.translatable("cwa.paster.paste.busy"));
			return false;
		}

		PasterData data = PasterData.getOrCreate(server);
		List<PasterMaterial> missing = missingMaterials(snapshot, data.get(player.getUUID(), snapshot.id()));
		if (!missing.isEmpty()) {
			PasterMaterial first = missing.get(0);
			source.sendFailure(CwaMessages.translatable(
					"cwa.paster.paste.missing",
					first.sample().getHoverName(),
					first.count()
			));
			return false;
		}

		// A snapshot restored from server data has no live client-side
		// placement object. That also covers LAN guests on an integrated
		// server, whose snapshot must be pasted by their own client.
		boolean remote = server.isDedicatedServer() || snapshot.placement() == null;
		if (!remote && LitematicaBridge.hasPasteTask()) {
			source.sendFailure(CwaMessages.translatable("cwa.paster.paste.busy"));
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
				snapshot.bounds(),
				snapshot.discardRules(),
				level,
				temporaryRegions,
				server.getTickCount(),
				remote
		);
		ACTIVE_PASTES.put(player.getUUID(), session);

		if (remote) {
			if (snapshot.fingerprint() == null
					|| snapshot.fingerprint().isEmpty()
					|| !PasterNetworking.sendStartPaste(player, snapshot.id(), snapshot.fingerprint())) {
				failPaste(server, session);
				source.sendFailure(CwaMessages.translatable("cwa.paster.paste.failed"));
				return false;
			}
		} else {
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
				source.sendFailure(CwaMessages.translatable("cwa.paster.paste.failed"));
				return false;
			}
		}

		source.sendSuccess(() -> CwaMessages.translatable(
				"cwa.paster.paste.started",
				snapshot.name()
		), false);
		return true;
	}

	public static void completeRemotePaste(ServerPlayer player, UUID placementId, boolean success) {
		ActivePaste session = ACTIVE_PASTES.get(player.getUUID());
		if (session == null
				|| !session.remote
				|| !session.placementId.equals(placementId)) {
			return;
		}
		finishPaste(player.level().getServer(), session, success);
	}

	public static void handleRemoteCommand(ServerPlayer player, PasterCommandPayload payload) {
		ActivePaste session = ACTIVE_PASTES.get(player.getUUID());
		if (session == null
				|| !session.remote
				|| !session.placementId.equals(payload.placementId())
				|| session.level != player.level()) {
			return;
		}

		String command = PasterCommandRules.normalize(payload.command());
		if (!PasterCommandRules.isValid(command, session.bounds)) {
			session.rejectedCommands++;
			CarpetWindAddition.LOGGER.warn(
					"Rejected remote paster command from {}: {}",
					player.getScoreboardName(),
					command.length() > 120 ? command.substring(0, 120) + "..." : command
			);
			return;
		}

		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		if (command.equalsIgnoreCase("gamerule send_command_feedback false")) {
			session.feedbackDisabled = true;
		} else if (command.equalsIgnoreCase("gamerule send_command_feedback true")) {
			session.feedbackDisabled = false;
		}

		try {
			CommandSourceStack source = player.createCommandSourceStack().withPermission(4);
			server.getCommands().performPrefixedCommand(source, command);
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to execute a remote paster command", throwable);
		}
	}

	public static void tick(MinecraftServer server) {
		for (ActivePaste session : List.copyOf(ACTIVE_PASTES.values())) {
			if (session.server != server) {
				continue;
			}

			if (session.remote) {
				if (server.getTickCount() - session.startTick > 20 * 60 * 30) {
					finishPaste(server, session, false);
				}
				continue;
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
	}

	public static void onPlayerDisconnect(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		ActivePaste session = ACTIVE_PASTES.get(player.getUUID());
		if (session != null) {
			finishPaste(server, session, false);
		}
	}

	private static void failPaste(MinecraftServer server, ActivePaste session) {
		finishPaste(server, session, false);
	}

	private static void finishPaste(MinecraftServer server, ActivePaste session, boolean success) {
		if (ACTIVE_PASTES.get(session.playerId) != session) {
			return;
		}
		if (success && session.rejectedCommands > 0) {
			CarpetWindAddition.LOGGER.warn(
					"Remote paster for {} reported success but the server rejected {} commands; not consuming materials",
					session.playerId,
					session.rejectedCommands
			);
			success = false;
		}

		for (NoUpdateManager.TemporaryRegion region : session.temporaryRegions) {
			NoUpdateManager.endTemporary(session.level, region);
		}
		ACTIVE_PASTES.remove(session.playerId);

		if (session.remote && session.feedbackDisabled) {
			try {
				server.getCommands().performPrefixedCommand(
						server.createCommandSourceStack(),
						"gamerule send_command_feedback true"
				);
			} catch (Throwable throwable) {
				CarpetWindAddition.LOGGER.warn("Failed to restore command feedback after a paster paste", throwable);
			}
			session.feedbackDisabled = false;
		}

		if (success) {
			discardContainerItems(session.level, session.bounds, session.discardRules);
			consumePastedMaterials(server, session.playerId, session.placementId, session.consumedMaterials);
		}

		ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
		if (player != null) {
			player.sendSystemMessage(CwaMessages.translatable(
					success ? "cwa.paster.paste.finished" : "cwa.paster.paste.failed"
			));
		}
	}

	private static void discardContainerItems(
			ServerLevel level,
			List<PasterBounds> bounds,
			List<PasterDiscardRule> discardRules
	) {
		if (discardRules.isEmpty()) {
			return;
		}

		try {
			for (PasterBounds bound : bounds) {
				for (BlockPos pos : BlockPos.betweenClosed(bound.start(), bound.end())) {
					BlockEntity blockEntity = level.getBlockEntity(pos);
					if (blockEntity == null) {
						continue;
					}

					String blockId = BuiltInRegistries.BLOCK
							.getKey(level.getBlockState(pos).getBlock())
							.toString();
					String blockEntityId = String.valueOf(
							BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType())
					);
					List<PasterDiscardRule> matchingRules = new ArrayList<>();
					for (PasterDiscardRule rule : discardRules) {
						if (rule.matchesContainer(blockEntityId, blockId)) {
							matchingRules.add(rule);
						}
					}
					if (matchingRules.isEmpty() || !(blockEntity instanceof Container container)) {
						continue;
					}

					boolean changed = false;
					for (int slot = 0; slot < container.getContainerSize(); slot++) {
						ItemStack stack = container.getItem(slot);
						if (stack.isEmpty()) {
							continue;
						}
						for (PasterDiscardRule rule : matchingRules) {
							if (rule.matchesItem(stack)) {
								container.setItem(slot, ItemStack.EMPTY);
								changed = true;
								break;
							}
						}
					}
					if (changed) {
						container.setChanged();
					}
				}
			}
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to discard ignored container items after a paster paste", throwable);
		}
	}

	private static void consumePastedMaterials(
			MinecraftServer server,
			UUID playerId,
			UUID placementId,
			List<PasterMaterial> materials
	) {
		PasterData data = PasterData.get(server);
		if (data == null) {
			return;
		}

		List<ItemStack> stored = data.get(playerId, placementId);
		for (PasterMaterial material : materials) {
			PasterMaterials.take(stored, material.sample(), material.count());
		}
		data.set(playerId, placementId, stored);
		refreshProtectedPlacements(server);
	}

	public static void onServerLoaded(MinecraftServer server) {
		refreshProtectedPlacements(server);
		CarpetWindAddition.LOGGER.info(
				"Paster data loaded: {} protected placement(s): {}",
				protectedPlacements.size(),
				protectedPlacements
		);
	}

	public static void onServerClosed(MinecraftServer server) {
		for (ActivePaste session : List.copyOf(ACTIVE_PASTES.values())) {
			if (session.server != server) {
				continue;
			}
			for (NoUpdateManager.TemporaryRegion region : session.temporaryRegions) {
				NoUpdateManager.endTemporary(session.level, region);
			}
		}
		ACTIVE_PASTES.clear();
		protectedPlacements = Map.of();
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

	public record ReturnResult(int returned, int remaining, List<BlockPos> containers) {
	}

	public record ReturnOrphanedResult(int returned, int remaining, int placements) {
	}

	private static final class ActivePaste {
		private final MinecraftServer server;
		private final UUID playerId;
		private final UUID placementId;
		private final List<PasterMaterial> consumedMaterials;
		private final List<PasterBounds> bounds;
		private final List<PasterDiscardRule> discardRules;
		private final ServerLevel level;
		private final List<NoUpdateManager.TemporaryRegion> temporaryRegions;
		private final int startTick;
		private final boolean remote;
		private boolean seen;
		private boolean feedbackDisabled;
		private int rejectedCommands;
		private int notRunningSince = -1;
		private @Nullable Boolean completionResult;

		private ActivePaste(
				MinecraftServer server,
				UUID playerId,
				UUID placementId,
				List<PasterMaterial> consumedMaterials,
				List<PasterBounds> bounds,
				List<PasterDiscardRule> discardRules,
				ServerLevel level,
				List<NoUpdateManager.TemporaryRegion> temporaryRegions,
				int startTick,
				boolean remote
		) {
			this.server = server;
			this.playerId = playerId;
			this.placementId = placementId;
			this.consumedMaterials = List.copyOf(consumedMaterials);
			this.bounds = List.copyOf(bounds);
			this.discardRules = List.copyOf(discardRules);
			this.level = level;
			this.temporaryRegions = temporaryRegions;
			this.startTick = startTick;
			this.remote = remote;
		}
	}

}
