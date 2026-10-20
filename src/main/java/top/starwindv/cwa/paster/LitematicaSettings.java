package top.starwindv.cwa.paster;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import top.starwindv.cwa.rule.CwaSettings;

public final class LitematicaSettings {
	public static final String LITEMATICA_PASTER = "LitematicaPaster";

	@Rule(categories = {CwaSettings.CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean LitematicaPaster = false;

	private LitematicaSettings() {
	}
}
