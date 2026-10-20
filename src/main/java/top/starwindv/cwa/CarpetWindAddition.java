package top.starwindv.cwa;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CarpetWindAddition implements ModInitializer {
	public static final String MOD_ID = "cwa";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CarpetServer.manageExtension(CarpetWindAdditionExtension.INSTANCE);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
