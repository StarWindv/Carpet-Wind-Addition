package top.starwindv.cwa.paster;

import java.util.Map;
import net.minecraft.network.chat.Component;

public final class PasterProtection {
	private PasterProtection() {
	}

	public static boolean shouldBlockSchematicUnload(Object schematic) {
		Map<String, String> placements = PasterManager.protectedPlacements();
		return !placements.isEmpty() && LitematicaBridge.isSchematicUsedByStoredPlacements(schematic, placements);
	}

	public static void notifyUnloadBlocked() {
		LitematicaBridge.displayMessage(Component.translatable("cwa.paster.protection.unload_blocked"));
	}
}
