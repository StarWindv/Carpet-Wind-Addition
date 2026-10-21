package top.starwindv.cwa.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import top.starwindv.cwa.allow.AllowData;
import top.starwindv.cwa.allow.AllowManager;
import top.starwindv.cwa.util.CwaMessages;

public final class AllowCommand {
	private AllowCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("allow")
				.requires(AllowManager::canManage)
				.executes(AllowCommand::listAllowed)
				.then(Commands.argument("command", StringArgumentType.word())
						.suggests(AllowCommand::suggestCommands)
						.executes(context -> setAllowed(context, StringArgumentType.getString(context, "command"), true))
						.then(Commands.argument("allowed", BoolArgumentType.bool())
								.executes(context -> setAllowed(
										context,
										StringArgumentType.getString(context, "command"),
										BoolArgumentType.getBool(context, "allowed")
								)))));
	}

	private static int setAllowed(CommandContext<CommandSourceStack> context, String command, boolean allowed) {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();

		if (command.equals("allow")) {
			source.sendFailure(CwaMessages.translatable("cwa.allow.self"));
			return 0;
		}

		if (server.getCommands().getDispatcher().getRoot().getChild(command) == null) {
			source.sendFailure(CwaMessages.translatable("cwa.allow.unknown", command));
			return 0;
		}

		AllowData data = AllowData.getOrCreate(server);
		data.setAllowed(command, allowed);
		AllowManager.refreshCommands(server);

		source.sendSuccess(() -> CwaMessages.translatable(
				allowed ? "cwa.allow.set" : "cwa.allow.unset",
				command
		), false);
		return 1;
	}

	private static int listAllowed(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		AllowData data = AllowData.get(source.getServer());

		if (data == null || data.commands().isEmpty()) {
			source.sendSuccess(() -> CwaMessages.translatable("cwa.allow.list.empty"), false);
			return 0;
		}

		source.sendSuccess(() -> CwaMessages.translatable(
				"cwa.allow.list.header",
				String.join(", ", data.commands())
		), false);
		return data.commands().size();
	}

	private static CompletableFuture<Suggestions> suggestCommands(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		CommandDispatcher<CommandSourceStack> dispatcher = context.getSource().getServer().getCommands().getDispatcher();

		for (CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChildren()) {
			if (!node.getName().equals("allow")) {
				builder.suggest(node.getName());
			}
		}
		return builder.buildFuture();
	}
}
