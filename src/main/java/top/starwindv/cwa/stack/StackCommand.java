package top.starwindv.cwa.stack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
				.then(Commands.argument("item", StringArgumentType.word())
						.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
								.executes(StackCommand::setStackSize))));
	}

	private static int setStackSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		String itemInput = StringArgumentType.getString(context, "item");
		int count = IntegerArgumentType.getInteger(context, "count");

		Identifier itemId = Identifier.tryParse(itemInput);
		if (itemId == null) {
			source.sendFailure(Component.translatable("cwa.stack.invalid_id", itemInput));
			return 0;
		}
		if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
			source.sendFailure(Component.translatable("cwa.stack.unknown_item", itemInput));
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
}
