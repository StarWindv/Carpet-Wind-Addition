package top.starwindv.cwa;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.starwindv.cwa.paster.PasterNetworking;
import top.starwindv.cwa.stack.StackSizeManager;
import top.starwindv.cwa.stack.StackSizesPayload;

public class CarpetWindAddition implements ModInitializer {
	public static final String MOD_ID = "cwa";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CarpetServer.manageExtension(CarpetWindAdditionExtension.INSTANCE);
		PasterNetworking.registerTypes();
		PasterNetworking.registerServerReceivers();
		PayloadTypeRegistry.clientboundPlay().register(StackSizesPayload.TYPE, StackSizesPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
			if (listener.player != null) {
				StackSizeManager.sendTo(listener.player);
			}
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
