package top.starwindv.cwa.paster;

import java.util.List;
import java.util.UUID;

public final class PasterClient {
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
		boolean started = LitematicaBridge.startServuxPaste(
				request.placementId(),
				request.fingerprint(),
				success -> PasterNetworking.sendSnapshotResult(request.placementId(), success)
		);
		if (!started) {
			PasterNetworking.sendSnapshotResult(request.placementId(), false);
		}
	}
}
