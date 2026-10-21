package top.starwindv.cwa;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import top.starwindv.cwa.stack.StackSizeManager;
import top.starwindv.cwa.stack.StackSizesPayload;

public final class CarpetWindAdditionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(StackSizesPayload.TYPE,
				(payload, context) -> StackSizeManager.replace(payload.sizes()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> StackSizeManager.clear());
	}
}
