package top.starwindv.cwa.paster;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Server-side coordinator for the remote (dedicated server) paster path.
 * The integrated/singleplayer path is intentionally left unchanged.
 */
public final class RemotePaster {
	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

	private RemotePaster() {
	}

	public static int request(
			CommandSourceStack source,
			PasterAction action,
			int page,
			@Nullable BlockPos start,
			@Nullable BlockPos end
	) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.translatable("cwa.paster.no_projection"));
			return 0;
		}

		UUID requestId = UUID.randomUUID();
		PENDING.put(player.getUUID(), new Pending(requestId, action, page, start, end));
		if (!PasterNetworking.sendRequest(player, new PasterRequestPayload(requestId, action, page, start, end))) {
			PENDING.remove(player.getUUID());
			source.sendFailure(Component.translatable("cwa.paster.paste.failed"));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("cwa.paster.requesting"), false);
		return 1;
	}

	public static void handleSnapshot(ServerPlayer player, PasterSnapshotPayload payload) {
		Pending pending = PENDING.remove(player.getUUID());
		if (pending == null || !pending.requestId.equals(payload.requestId())) {
			return;
		}

		if (!payload.error().isEmpty()) {
			player.sendSystemMessage(switch (payload.error()) {
				case "no_projection" -> Component.translatable("cwa.paster.no_projection");
				case "fingerprint_unavailable" -> Component.translatable("cwa.paster.fingerprint_unavailable");
				default -> Component.literal(payload.error());
			});
			return;
		}

		if (payload.placementId().equals(new UUID(0L, 0L))) {
			player.sendSystemMessage(Component.translatable("cwa.paster.no_projection"));
			return;
		}

		PasterSnapshotData.StoredSnapshot incoming = new PasterSnapshotData.StoredSnapshot(
				payload.name(),
				payload.dimension(),
				payload.requiredMaterials(),
				payload.bounds(),
				payload.fingerprint()
		);
		PasterSnapshotData data = PasterSnapshotData.getOrCreate(player.level().getServer());
		PasterSnapshotData.StoredSnapshot stored = data.get(player.getUUID(), payload.placementId());
		CommandSourceStack source = player.createCommandSourceStack();

		if (pending.action == PasterAction.PASTE) {
			if (stored == null) {
				source.sendFailure(Component.translatable("cwa.paster.paste.no_snapshot"));
				return;
			}
			if (!stored.fingerprint().equals(incoming.fingerprint())) {
				source.sendFailure(Component.translatable("cwa.paster.schematic_changed"));
				return;
			}

			// The schematic file can stay identical while its placement is moved
			// after collecting materials. The stored material list remains
			// server-owned, but the current placement box has to come from the
			// client that is about to run the paste task.
			if (incoming.bounds().isEmpty()) {
				source.sendFailure(Component.translatable("cwa.paster.paste.no_snapshot"));
				return;
			}
			stored = new PasterSnapshotData.StoredSnapshot(
					stored.name(),
					incoming.dimension(),
					stored.requiredMaterials(),
					incoming.bounds(),
					stored.fingerprint()
			);
			data.set(player.getUUID(), payload.placementId(), stored);
		} else if (pending.action == PasterAction.COLLECT_SELF
				|| pending.action == PasterAction.COLLECT_RANGE) {
			data.set(player.getUUID(), payload.placementId(), incoming);
			stored = incoming;
		} else if (stored == null) {
			data.set(player.getUUID(), payload.placementId(), incoming);
			stored = incoming;
		}

		PlacementSnapshot snapshot = stored.toPlacementSnapshot(payload.placementId());
		if (!player.level().dimension().identifier().toString().equals(snapshot.dimension())) {
			source.sendFailure(Component.translatable("cwa.paster.wrong_dimension"));
			return;
		}

		switch (pending.action) {
			case COLLECT_SELF -> {
				PasterManager.CollectResult result = PasterManager.collectSelf(player, snapshot);
				PasterCommand.sendCollectResult(source, result);
			}
			case COLLECT_RANGE -> {
				if (pending.start == null || pending.end == null) {
					return;
				}
				PasterManager.CollectResult result = PasterManager.collectRange(
						player.level(),
						player,
						snapshot,
						pending.start,
						pending.end
				);
				PasterCommand.sendCollectResult(source, result);
			}
			case LIST -> PasterCommand.sendMaterialList(source, player, snapshot, pending.page);
			case PASTE -> {
				PasterManager.startPaste(source, player, snapshot);
			}
			case RETURN_SELF -> {
				if (PasterManager.isPasteRunning(player.getUUID())) {
					source.sendFailure(Component.translatable("cwa.paster.return.busy"));
					return;
				}
				PasterManager.ReturnResult result = PasterManager.returnSelf(player, snapshot);
				PasterCommand.sendReturnMessage(source, result);
			}
			case RETURN_RANGE -> {
				if (PasterManager.isPasteRunning(player.getUUID())) {
					source.sendFailure(Component.translatable("cwa.paster.return.busy"));
					return;
				}
				if (pending.start == null || pending.end == null) {
					return;
				}
				PasterManager.ReturnResult result = PasterManager.returnRange(
						player.level(),
						player.getUUID(),
						snapshot,
						pending.start,
						pending.end
				);
				PasterCommand.sendReturnMessage(source, result);
			}
		}
	}

	private record Pending(
			UUID requestId,
			PasterAction action,
			int page,
			@Nullable BlockPos start,
			@Nullable BlockPos end
	) {
	}
}
