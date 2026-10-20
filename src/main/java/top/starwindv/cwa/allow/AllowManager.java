package top.starwindv.cwa.allow;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
				&& server.isSingleplayerOwner(player.nameAndId());
	}

	public static boolean isAllowed(CommandSourceStack source, String command) {
		if (!isHost(source) || command.equals("allow")) {
			return false;
		}

		AllowData data = AllowData.get(source.getServer());
		return data != null && data.isAllowed(command);
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
			server.getCommands().sendCommands(player);
		}
	}
}
