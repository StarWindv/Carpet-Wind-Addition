package top.starwindv.cwa.stack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.starwindv.cwa.allow.AllowManager;
import top.starwindv.cwa.rule.CwaSettings;

public final class StackCommand {
	private StackCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		Predicate<CommandSourceStack> permission = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
		dispatcher.register(Commands.literal("stack")
				.requires(source -> CwaSettings.StackAnything
						&& (AllowManager.canUseHostCommand(source, "stack") || permission.test(source)))
				.then(Commands.argument("item", IdentifierArgument.id())
						.then(Commands.argument("count", IntegerArgumentType.integer(1))
								.executes(StackCommand::setStackSize))));
	}

	private static int setStackSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		int count = IntegerArgumentType.getInteger(context, "count");
		int max = CwaSettings.MaxStackAnythingSize;
		if (max <= 0) {
			max = 64;
		}
		if (count > max) {
			int limit = max;
			source.sendFailure(Component.translatable("cwa.stack.too_large", limit));
			return 0;
		}

		Identifier parsedId = IdentifierArgument.getId(context, "item");
		Identifier itemId = null;
		if (BuiltInRegistries.ITEM.containsKey(parsedId)) {
			itemId = parsedId;
		} else {
			List<Identifier> matches = new ArrayList<>();
			for (Identifier registered : BuiltInRegistries.ITEM.keySet()) {
				if (registered.getPath().equals(parsedId.getPath())) {
					matches.add(registered);
				}
			}
			if (matches.size() > 1) {
				source.sendFailure(Component.translatable(
						"cwa.stack.ambiguous_item",
						formatIds(matches)
				));
				return 0;
			}
			if (matches.size() == 1) {
				itemId = matches.get(0);
			}
		}

		if (itemId == null) {
			source.sendFailure(Component.translatable("cwa.stack.unknown_item", parsedId.toString()));
			return 0;
		}

		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		MinecraftServer server = source.getServer();
		StackSizeData.getOrCreate(server).set(itemId.toString(), count);
		StackSizeManager.set(item, count);
		StackSizeManager.broadcast(server);

		source.sendSuccess(() -> Component.translatable(
				"cwa.stack.set",
				item.getName(new ItemStack(item)),
				count
		), false);
		return 1;
	}

	private static String formatIds(List<Identifier> ids) {
		StringBuilder builder = new StringBuilder();
		for (Identifier id : ids) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(id);
		}
		return builder.toString();
	}
}
