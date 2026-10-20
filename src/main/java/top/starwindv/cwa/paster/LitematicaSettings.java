package top.starwindv.cwa.paster;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public final class LitematicaSettings {
	public static final String LITEMATICA_PASTER = "LitematicaPaster";

	@Rule(categories = RuleCategory.FEATURE, options = {"true", "false"}, strict = true)
	public static boolean LitematicaPaster = false;

	private LitematicaSettings() {
	}
}
