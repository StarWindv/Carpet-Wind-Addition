package top.starwindv.cwa.rule;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.Validator;
import net.minecraft.commands.CommandSourceStack;

public final class CwaSettings {
	public static final String CWA = "CWA";
	public static final String NEVER_FULL = "NeverFull";
	public static final String PISTON_HARVEST_AMETHYST = "PistonHarvestAmethyst";
	public static final String FURNACES_CLEANER = "FurnacesCleaner";
	public static final String CONTAINER_CLEANER = "ContainerCleaner";
	public static final String ALLOW_SHARE_PERMISSION = "AllowSharePermission";
	public static final String STACK_ANYTHING = "StackAnything";
	public static final String MAX_STACK_ANYTHING_SIZE = "MaxStackAnythingSize";

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

	@Rule(categories = {CWA, RuleCategory.FEATURE}, strict = false, validators = {CwaSettings.PositiveInteger.class})
	public static int MaxStackAnythingSize = 64;

	private CwaSettings() {
	}

	public static class PositiveInteger extends Validator<Integer> {
		@Override
		public Integer validate(
				CommandSourceStack source,
				CarpetRule<Integer> currentRule,
				Integer newValue,
				String userString
		) {
			return newValue > 0 ? newValue : null;
		}

		@Override
		public String description() {
			return "Must be a positive integer";
		}
	}
}
