package top.starwindv.cwa.paster;

import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import top.starwindv.cwa.CarpetWindAddition;

public final class PasterNetworking {
	private PasterNetworking() {
	}

	public static void registerTypes() {
		PayloadTypeRegistry.clientboundPlay().register(PasterRequestPayload.TYPE, PasterRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PasterStartPastePayload.TYPE, PasterStartPastePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PasterSnapshotPayload.TYPE, PasterSnapshotPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PasterPasteResultPayload.TYPE, PasterPasteResultPayload.CODEC);
	}

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(PasterSnapshotPayload.TYPE, (payload, context) ->
				context.server().execute(() -> RemotePaster.handleSnapshot(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(PasterPasteResultPayload.TYPE, (payload, context) ->
				context.server().execute(() -> PasterManager.completeRemotePaste(
						context.player(),
						payload.placementId(),
						payload.success()
				)));
	}

	public static void registerClientReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(PasterRequestPayload.TYPE, (payload, context) ->
				context.client().execute(() -> PasterClient.handleRequest(payload)));
		ClientPlayNetworking.registerGlobalReceiver(PasterStartPastePayload.TYPE, (payload, context) ->
				context.client().execute(() -> PasterClient.handleStartPaste(payload)));
	}

	public static boolean sendRequest(ServerPlayer player, PasterRequestPayload payload) {
		try {
			if (!ServerPlayNetworking.canSend(player, PasterRequestPayload.TYPE)) {
				CarpetWindAddition.LOGGER.warn("Cannot request a paster snapshot from {}: the client has no CWA paster channel", player.getScoreboardName());
				return false;
			}
			ServerPlayNetworking.send(player, payload);
			return true;
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to request a paster snapshot from {}", player.getScoreboardName(), throwable);
			return false;
		}
	}

	public static boolean sendStartPaste(ServerPlayer player, java.util.UUID placementId, String fingerprint) {
		try {
			if (!ServerPlayNetworking.canSend(player, PasterStartPastePayload.TYPE)) {
				CarpetWindAddition.LOGGER.warn("Cannot start a remote paste for {}: the client has no CWA paster channel", player.getScoreboardName());
				return false;
			}
			ServerPlayNetworking.send(player, new PasterStartPastePayload(placementId, fingerprint));
			return true;
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to start a remote paste for {}", player.getScoreboardName(), throwable);
			return false;
		}
	}

	static void sendSnapshot(PasterSnapshotPayload payload) {
		if (ClientPlayNetworking.canSend(PasterSnapshotPayload.TYPE)) {
			ClientPlayNetworking.send(payload);
		}
	}

	static void sendSnapshotResult(UUID placementId, boolean success) {
		if (ClientPlayNetworking.canSend(PasterPasteResultPayload.TYPE)) {
			ClientPlayNetworking.send(new PasterPasteResultPayload(placementId, success));
		}
	}
}
