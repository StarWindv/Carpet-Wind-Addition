package top.starwindv.cwa;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import top.starwindv.cwa.crafting.NearbyCraftingClient;
import top.starwindv.cwa.crafting.NearbyCraftingPayload;
import top.starwindv.cwa.paster.PasterClient;
import top.starwindv.cwa.paster.PasterNetworking;
import top.starwindv.cwa.stack.StackSizeManager;
import top.starwindv.cwa.stack.StackSizesPayload;

public final class CarpetWindAdditionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PasterNetworking.registerClientReceivers();
		ClientPlayNetworking.registerGlobalReceiver(StackSizesPayload.TYPE,
				(payload, context) -> StackSizeManager.replace(payload.sizes()));
		ClientPlayNetworking.registerGlobalReceiver(NearbyCraftingPayload.TYPE,
				(payload, context) -> context.client().execute(() -> NearbyCraftingClient.accept(payload.stacks())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			StackSizeManager.clear();
			PasterClient.reset();
			NearbyCraftingClient.clear();
		});
	}
}
