package top.starwindv.cwa.furnace;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import top.starwindv.cwa.allow.AllowManager;
import top.starwindv.cwa.rule.CwaSettings;
import top.starwindv.cwa.util.ItemReceiver;
import top.starwindv.cwa.util.ItemTransfer;
import top.starwindv.cwa.util.CwaMessages;

public final class FurnaceCommand {
	private FurnaceCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		Predicate<CommandSourceStack> permission = source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS);
		dispatcher.register(Commands.literal("furnace")
				.requires(source -> CwaSettings.FurnacesCleaner
						&& (AllowManager.canUseHostCommand(source, "furnace") || permission.test(source)))
				.then(Commands.argument("start", BlockPosArgument.blockPos())
						.then(Commands.argument("end", BlockPosArgument.blockPos())
								.then(Commands.literal("self")
										.executes(FurnaceCommand::cleanSelf))
								.then(Commands.literal("discard")
										.executes(FurnaceCommand::cleanDiscard))
								.then(Commands.literal("to")
										.then(Commands.argument("receiverStart", BlockPosArgument.blockPos())
												.then(Commands.argument("receiverEnd", BlockPosArgument.blockPos())
														.executes(FurnaceCommand::cleanToRange)))))));
	}

	private static int cleanSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		int result = clean(context, stack -> ItemTransfer.insertIntoInventory(player.getInventory(), stack));
		player.inventoryMenu.broadcastChanges();
		return result;
	}

	private static int cleanDiscard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return clean(context, stack -> ItemStack.EMPTY);
	}

	private static int cleanToRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerLevel level = context.getSource().getLevel();
		BlockPos receiverStart = BlockPosArgument.getBlockPos(context, "receiverStart");
		BlockPos receiverEnd = BlockPosArgument.getBlockPos(context, "receiverEnd");
		List<Container> containers = ItemTransfer.findContainers(
				level,
				receiverStart,
				receiverEnd,
				container -> !(container instanceof AbstractFurnaceBlockEntity)
		);
		return clean(context, stack -> ItemTransfer.insertIntoContainers(containers, stack));
	}

	private static int clean(CommandContext<CommandSourceStack> context, ItemReceiver receiver)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos start = BlockPosArgument.getBlockPos(context, "start");
		BlockPos end = BlockPosArgument.getBlockPos(context, "end");
		FurnaceCleaner.Result result = FurnaceCleaner.clean(level, start, end, receiver);
		sendResult(source, result);
		return result.removed() > 0 ? 1 : 0;
	}

	private static void sendResult(CommandSourceStack source, FurnaceCleaner.Result result) {
		if (result.found() == 0) {
			source.sendSuccess(() -> CwaMessages.translatable("cwa.furnace.none"), false);
			return;
		}

		source.sendSuccess(() -> CwaMessages.translatable(
				"cwa.furnace.result",
				result.furnaces(),
				result.found(),
				result.removed()
		), false);

		if (result.leftover() > 0) {
			source.sendSuccess(() -> CwaMessages.translatable(
					"cwa.furnace.leftover",
					result.leftover(),
					result.leftoverSlots()
			).withStyle(ChatFormatting.RED), false);
		}
	}
}
