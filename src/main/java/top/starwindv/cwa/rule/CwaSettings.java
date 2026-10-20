package top.starwindv.cwa.rule;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public final class CwaSettings {
	public static final String NEVER_FULL = "NeverFull";
	public static final String PISTON_HARVEST_AMETHYST = "PistonHarvestAmethyst";

	@Rule(categories = RuleCategory.FEATURE, options = {"true", "false"}, strict = true)
	public static boolean NeverFull = false;

	@Rule(categories = RuleCategory.FEATURE, options = {"true", "false"}, strict = true)
	public static boolean PistonHarvestAmethyst = false;

	private CwaSettings() {
	}
}
