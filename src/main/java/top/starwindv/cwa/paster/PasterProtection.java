package top.starwindv.cwa.paster;

import java.util.Set;
import net.minecraft.network.chat.Component;
import top.starwindv.cwa.util.CwaMessages;

public final class PasterProtection {
	private PasterProtection() {
	}

	public static boolean shouldBlockSchematicUnload(Object schematic) {
		Set<String> placementIds = PasterManager.protectedPlacementIds();
		return !placementIds.isEmpty() && LitematicaBridge.isSchematicUsedByStoredPlacements(schematic, placementIds);
	}

	public static void notifyUnloadBlocked() {
		LitematicaBridge.displayMessage(CwaMessages.translatable("cwa.paster.protection.unload_blocked"));
	}
}
