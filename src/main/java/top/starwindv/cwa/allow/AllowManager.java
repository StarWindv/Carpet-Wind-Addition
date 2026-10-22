package top.starwindv.cwa.allow;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import top.starwindv.cwa.CarpetWindAddition;
import top.starwindv.cwa.rule.CwaSettings;

public final class AllowManager {
	private AllowManager() {
	}

	public static boolean canManage(CommandSourceStack source) {
		return isHost(source);
	}

	public static boolean isHost(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		MinecraftServer server = source.getServer();

		return player != null
				&& server != null
				&& server.isSingleplayer()
				&& server.isSingleplayerOwner(player.getGameProfile());
	}

	public static boolean isAllowed(CommandSourceStack source, String command) {
		if (command.equals("allow")) {
			return false;
		}
		MinecraftServer server = source.getServer();
		if (server == null) {
			// ClientboundCommandsPacket evaluates command requirements with a
			// synthetic source that has no server attached.
			return false;
		}
		if (!isHost(source) && !CwaSettings.AllowSharePermission) {
			return false;
		}

		AllowData data = AllowData.get(server);
		return data != null && data.isAllowed(command);
	}

	public static boolean canUseHostCommand(CommandSourceStack source, String command) {
		return isHost(source) || isAllowed(source, command);
	}

	public static String commandRoot(String commandLine) {
		String command = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
		int end = 0;
		while (end < command.length() && !Character.isWhitespace(command.charAt(end))) {
			end++;
		}
		return command.substring(0, end);
	}

	public static void refreshCommands(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			try {
				server.getCommands().sendCommands(player);
			} catch (Throwable throwable) {
				CarpetWindAddition.LOGGER.warn(
						"Failed to refresh command permissions for {}",
						player.getScoreboardName(),
						throwable
				);
			}
		}
	}
}
