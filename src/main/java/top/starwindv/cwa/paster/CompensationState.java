package top.starwindv.cwa.paster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import top.starwindv.cwa.CarpetWindAddition;

/**
 * Per-client one-time marker for {@code /paster debug compensate}. The file is
 * shared by all worlds and servers used by this client installation.
 */
public final class CompensationState {
	private static final Path MARKER = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("cwa-compensate-used");

	private CompensationState() {
	}

	public static boolean isUsed() {
		return Files.exists(MARKER);
	}

	public static void markUsed() {
		try {
			Files.createDirectories(MARKER.getParent());
			Files.writeString(MARKER, Long.toString(System.currentTimeMillis()));
		} catch (IOException exception) {
			CarpetWindAddition.LOGGER.warn("Failed to persist the paster compensation marker", exception);
		}
	}
}
