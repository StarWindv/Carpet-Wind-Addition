package top.starwindv.cwa;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.Translations;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Map;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import top.starwindv.cwa.allow.AllowManager;
import top.starwindv.cwa.command.AllowCommand;
import top.starwindv.cwa.command.NoUpdateCommand;
import top.starwindv.cwa.noupdate.NoUpdateManager;
import top.starwindv.cwa.paster.LitematicaBridge;
import top.starwindv.cwa.paster.LitematicaSettings;
import top.starwindv.cwa.paster.PasterCommand;
import top.starwindv.cwa.paster.PasterManager;
import top.starwindv.cwa.rule.CwaSettings;

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
				|| CarpetServer.settingsManager.getCarpetRule(CwaSettings.PISTON_HARVEST_AMETHYST) == null) {
			CarpetServer.settingsManager.parseSettingsClass(CwaSettings.class);
		}

		if (LitematicaBridge.isAvailable()
				&& CarpetServer.settingsManager.getCarpetRule(LitematicaSettings.LITEMATICA_PASTER) == null) {
			CarpetServer.settingsManager.parseSettingsClass(LitematicaSettings.class);
		}

		if (!ruleObserverRegistered) {
			CarpetServer.settingsManager.registerRuleObserver((source, rule, value) -> {
				if (rule.name().equals(LitematicaSettings.LITEMATICA_PASTER)) {
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
	}

	@Override
	public void onTick(MinecraftServer server) {
		PasterManager.tick(server);
	}

	@Override
	public void onServerClosed(MinecraftServer server) {
		NoUpdateManager.reset();
		PasterManager.onServerClosed(server);
	}

	@Override
	public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
		AllowCommand.register(dispatcher);
		NoUpdateCommand.register(dispatcher);
		if (LitematicaBridge.isAvailable()) {
			PasterCommand.register(dispatcher);
		}
	}
}
