package top.starwindv.cwa.paster;

import java.util.List;
import java.util.UUID;

public final class PasterClient {
	private static volatile UUID activeCommandPaste;
	private static volatile Object activeCommandTask;

	private PasterClient() {
	}

	public static void handleRequest(PasterRequestPayload request) {
		PlacementSnapshot snapshot = LitematicaBridge.currentPlacement();
		if (snapshot == null) {
			PasterNetworking.sendSnapshot(new PasterSnapshotPayload(
					request.requestId(),
					new UUID(0L, 0L),
					"",
					"",
					List.of(),
					List.of(),
					"",
					"no_projection"
			));
			return;
		}

		String fingerprint = snapshot.fingerprint();
		if (fingerprint == null || fingerprint.isEmpty()) {
			PasterNetworking.sendSnapshot(new PasterSnapshotPayload(
					request.requestId(),
					snapshot.id(),
					snapshot.name(),
					snapshot.dimension(),
					List.of(),
					List.of(),
					"",
					"fingerprint_unavailable"
			));
			return;
		}

		PasterNetworking.sendSnapshot(new PasterSnapshotPayload(
				request.requestId(),
				snapshot.id(),
				snapshot.name(),
				snapshot.dimension(),
				snapshot.requiredMaterials(),
				snapshot.bounds(),
				fingerprint,
				""
		));
	}

	public static void handleStartPaste(PasterStartPastePayload request) {
		UUID placementId = request.placementId();
		activeCommandPaste = placementId;
		boolean started = LitematicaBridge.startCommandPaste(
				placementId,
				request.fingerprint(),
				success -> finishCommandPaste(placementId, success)
		);
		if (!started) {
			finishCommandPaste(placementId, false);
		}
	}

	/**
	 * Called from the Litematica paste task right before it would send a vanilla
	 * command. The command is forwarded through CWA so the server can authorize
	 * only the command types produced by the paste task.
	 */
	public static boolean interceptCommand(Object task, String command) {
		UUID placementId = activeCommandPaste;
		if (placementId == null
				|| task != activeCommandTask
				|| !PasterCommandRules.canRoute(command)) {
			return false;
		}
		return PasterNetworking.sendCommand(placementId, command);
	}

	public static void setActiveCommandTask(Object task) {
		activeCommandTask = task;
	}

	public static void reset() {
		activeCommandPaste = null;
		activeCommandTask = null;
	}

	private static void finishCommandPaste(UUID placementId, boolean success) {
		if (!placementId.equals(activeCommandPaste)) {
			return;
		}
		activeCommandPaste = null;
		activeCommandTask = null;
		PasterNetworking.sendSnapshotResult(placementId, success);
	}
}
