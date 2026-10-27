package top.starwindv.cwa.rule;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.Validator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public final class CwaSettings {
	public static final String CWA = "CWA";
	public static final String NEVER_FULL = "NeverFull";
	public static final String PISTON_HARVEST_AMETHYST = "PistonHarvestAmethyst";
	public static final String FURNACES_CLEANER = "FurnacesCleaner";
	public static final String CONTAINER_CLEANER = "ContainerCleaner";
	public static final String ALLOW_SHARE_PERMISSION = "AllowSharePermission";
	public static final String STACK_ANYTHING = "StackAnything";
	public static final String MAX_STACK_ANYTHING_SIZE = "MaxStackAnythingSize";
	public static final String MOBS_NO_EQUIPMENT_DROP = "MobsNoEquipmentDrop";
	public static final String MOBS_NO_SPEAR = "MobsNoSpear";
	public static final String BUCKET_TANS_OSIDIAN_TO_LAVA = "BucketTansOsidianToLava";
	public static final String OBSIDIAN_TO_LAVA_RECIPE = "ObsidianToLavaRecipe";
	public static final String QUICK_ICE_TO_WATER = "QuickIceToWater";
	public static final String QUICK_ITW_CHECK = "QuickITWCheck";
	public static final String QUICK_PLACE_OBSIDIAN = "QuickPlaceObsidian";
	public static final String HOT_TNT = "HotTNT";
	public static final String WATERPROOF_REDSTONE_COMPONENTS = "WaterproofRedstoneComponents";
	public static final String WATERPROOF_CARPET = "WaterproofCarpet";
	public static final String EXPLOSION_PROOF_TINTED_GLASS = "ExplosionProofTintedGlass";
	public static final String NEARBY_CRAFTING = "NearbyCrafting";
	public static final String NEARBY_CRAFTING_SEARCH_SIZE = "NearbyCraftingSearchSize";

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

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean MobsNoEquipmentDrop = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean MobsNoSpear = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean BucketTansOsidianToLava = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean ObsidianToLavaRecipe = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean QuickIceToWater = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean QuickITWCheck = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean QuickPlaceObsidian = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean HotTNT = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean WaterproofRedstoneComponents = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean WaterproofCarpet = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean ExplosionProofTintedGlass = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, options = {"true", "false"}, strict = true)
	public static boolean NearbyCrafting = false;

	@Rule(categories = {CWA, RuleCategory.FEATURE}, strict = false, validators = {CwaSettings.NearbyCraftingSearchSizeValidator.class})
	public static int NearbyCraftingSearchSize = 8;

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

	public static class NearbyCraftingSearchSizeValidator extends Validator<Integer> {
		@Override
		public Integer validate(
				CommandSourceStack source,
				CarpetRule<Integer> currentRule,
				Integer newValue,
				String userString
		) {
			if (newValue == null || newValue <= 0) {
				return null;
			}

			MinecraftServer server = source == null ? null : source.getServer();
			if (server == null) {
				return newValue;
			}

			if (server.getPlayerList() == null) {
				return newValue;
			}

			int simulationDistance = server.getPlayerList().getSimulationDistance();
			if (simulationDistance <= 0) {
				// The integrated server restores rule defaults before the player
				// list has received the world's simulation distance.
				return newValue;
			}

			return newValue <= simulationDistance * 16 ? newValue : null;
		}

		@Override
		public String description() {
			return "Must be positive and no larger than the loaded radius of the current simulation distance";
		}
	}
}
