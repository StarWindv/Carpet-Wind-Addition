package top.starwindv.cwa;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.Translations;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import top.starwindv.cwa.allow.AllowManager;
import top.starwindv.cwa.command.AllowCommand;
import top.starwindv.cwa.command.NoUpdateCommand;
import top.starwindv.cwa.container.ContainerCleanerCommand;
import top.starwindv.cwa.crafting.NearbyCraftingManager;
import top.starwindv.cwa.furnace.FurnaceCommand;
import top.starwindv.cwa.noupdate.NoUpdateManager;
import top.starwindv.cwa.paster.LitematicaBridge;
import top.starwindv.cwa.paster.LitematicaSettings;
import top.starwindv.cwa.paster.PasterCommand;
import top.starwindv.cwa.paster.PasterManager;
import top.starwindv.cwa.rule.CwaSettings;
import top.starwindv.cwa.stack.StackCommand;
import top.starwindv.cwa.stack.StackSizeManager;

public final class CarpetWindAdditionExtension implements CarpetExtension {
	public static final CarpetWindAdditionExtension INSTANCE = new CarpetWindAdditionExtension();
	private static boolean ruleObserverRegistered;

	private CarpetWindAdditionExtension() {
	}

	@Override
	public void onGameStarted() {
		if (CarpetServer.settingsManager == null) {
			return;
		}

		if (CarpetServer.settingsManager.getCarpetRule(CwaSettings.NEVER_FULL) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.PISTON_HARVEST_AMETHYST) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.FURNACES_CLEANER) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.CONTAINER_CLEANER) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.ALLOW_SHARE_PERMISSION) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.STACK_ANYTHING) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.MAX_STACK_ANYTHING_SIZE) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.MOBS_NO_EQUIPMENT_DROP) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.BUCKET_TANS_OSIDIAN_TO_LAVA) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.OBSIDIAN_TO_LAVA_RECIPE) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.QUICK_ICE_TO_WATER) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.QUICK_ITW_CHECK) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.QUICK_PLACE_OBSIDIAN) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.HOT_TNT) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.WATERPROOF_REDSTONE_COMPONENTS) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.WATERPROOF_CARPET) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.EXPLOSION_PROOF_TINTED_GLASS) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.NEARBY_CRAFTING) == null
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.NEARBY_CRAFTING_SEARCH_SIZE) == null) {
			CarpetServer.settingsManager.parseSettingsClass(CwaSettings.class);
		}

		if ((LitematicaBridge.isAvailable() || FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
				&& CarpetServer.settingsManager.getCarpetRule(LitematicaSettings.LITEMATICA_PASTER) == null) {
			CarpetServer.settingsManager.parseSettingsClass(LitematicaSettings.class);
		}

		if (!ruleObserverRegistered) {
			CarpetServer.settingsManager.registerRuleObserver((source, rule, value) -> {
				if (rule.name().equals(LitematicaSettings.LITEMATICA_PASTER)
						|| rule.name().equals(CwaSettings.FURNACES_CLEANER)
						|| rule.name().equals(CwaSettings.CONTAINER_CLEANER)
						|| rule.name().equals(CwaSettings.ALLOW_SHARE_PERMISSION)
						|| rule.name().equals(CwaSettings.STACK_ANYTHING)) {
					MinecraftServer server = source != null
							? source.getServer()
							: CarpetServer.minecraft_server;
					if (server != null) {
						AllowManager.refreshCommands(server);
					}
				}
			});
			ruleObserverRegistered = true;
		}
	}

	@Override
	public Map<String, String> canHasTranslations(String lang) {
		Map<String, String> translations = Translations.getTranslationFromResourcePath("assets/cwa/lang/" + lang + ".json");
		if (translations.isEmpty() && !"en_us".equals(lang)) {
			translations = Translations.getTranslationFromResourcePath("assets/cwa/lang/en_us.json");
		}
		return translations;
	}

	@Override
	public void onServerLoadedWorlds(MinecraftServer server) {
		NoUpdateManager.refresh(server);
		PasterManager.onServerLoaded(server);
		StackSizeManager.onServerLoaded(server);
	}

	@Override
	public void onTick(MinecraftServer server) {
		PasterManager.tick(server);
	}

	@Override
	public void onServerClosed(MinecraftServer server) {
		NoUpdateManager.reset();
		PasterManager.onServerClosed(server);
		StackSizeManager.clear();
		NearbyCraftingManager.reset();
	}

	@Override
	public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
		AllowCommand.register(dispatcher);
		NoUpdateCommand.register(dispatcher);
		FurnaceCommand.register(dispatcher);
		ContainerCleanerCommand.register(dispatcher);
		StackCommand.register(dispatcher);
		if (LitematicaBridge.isAvailable() || FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			PasterCommand.register(dispatcher);
		}
	}
}
