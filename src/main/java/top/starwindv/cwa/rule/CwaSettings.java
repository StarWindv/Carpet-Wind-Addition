package top.starwindv.cwa.rule;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public final class CwaSettings {
	public static final String CWA = "CWA";
	public static final String NEVER_FULL = "NeverFull";
	public static final String PISTON_HARVEST_AMETHYST = "PistonHarvestAmethyst";
	public static final String FURNACES_CLEANER = "FurnacesCleaner";
	public static final String CONTAINER_CLEANER = "ContainerCleaner";
	public static final String ALLOW_SHARE_PERMISSION = "AllowSharePermission";
	public static final String STACK_ANYTHING = "StackAnything";

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean NeverFull = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean PistonHarvestAmethyst = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean FurnacesCleaner = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean ContainerCleaner = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean AllowSharePermission = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean StackAnything = false;

	private CwaSettings() {
	}
}
