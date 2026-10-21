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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.starwindv.cwa.allow.AllowManager;
import top.starwindv.cwa.rule.CwaSettings;
import top.starwindv.cwa.util.CwaMessages;

public final class StackCommand {
	private StackCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		Predicate<CommandSourceStack> permission = source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS);
		dispatcher.register(Commands.literal("stack")
				.requires(source -> CwaSettings.StackAnything
						&& (AllowManager.canUseHostCommand(source, "stack") || permission.test(source)))
				.then(Commands.argument("item", ResourceLocationArgument.id())
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
			source.sendFailure(CwaMessages.translatable("cwa.stack.too_large", limit));
			return 0;
		}

		ResourceLocation parsedId = ResourceLocationArgument.getId(context, "item");
		ResourceLocation itemId = null;
		if (BuiltInRegistries.ITEM.containsKey(parsedId)) {
			itemId = parsedId;
		} else {
			List<ResourceLocation> matches = new ArrayList<>();
			for (ResourceLocation registered : BuiltInRegistries.ITEM.keySet()) {
				if (registered.getPath().equals(parsedId.getPath())) {
					matches.add(registered);
				}
			}
			if (matches.size() > 1) {
				source.sendFailure(CwaMessages.translatable(
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
			source.sendFailure(CwaMessages.translatable("cwa.stack.unknown_item", parsedId.toString()));
			return 0;
		}

		Item item = BuiltInRegistries.ITEM.get(itemId);
		MinecraftServer server = source.getServer();
		StackSizeData.getOrCreate(server).set(itemId.toString(), count);
		StackSizeManager.set(item, count);
		StackSizeManager.broadcast(server);

		source.sendSuccess(() -> CwaMessages.translatable(
				"cwa.stack.set",
				item.getName(new ItemStack(item)),
				count
		), false);
		return 1;
	}

	private static String formatIds(List<ResourceLocation> ids) {
		StringBuilder builder = new StringBuilder();
		for (ResourceLocation id : ids) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(id);
		}
		return builder.toString();
	}
}
