package top.starwindv.cwa.container;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jspecify.annotations.Nullable;
import top.starwindv.cwa.allow.AllowManager;
import top.starwindv.cwa.rule.CwaSettings;
import top.starwindv.cwa.util.ItemReceiver;
import top.starwindv.cwa.util.ItemTransfer;

public final class ContainerCleanerCommand {
	private ContainerCleanerCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		Predicate<CommandSourceStack> permission = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
		dispatcher.register(Commands.literal("cc")
				.requires(source -> CwaSettings.ContainerCleaner
						&& (AllowManager.canUseHostCommand(source, "cc") || permission.test(source)))
				.then(Commands.argument("start", BlockPosArgument.blockPos())
						.then(Commands.argument("end", BlockPosArgument.blockPos())
								.then(Commands.argument("container", StringArgumentType.word())
										.then(Commands.argument("ash", StringArgumentType.word())
												.then(Commands.literal("self")
														.executes(ContainerCleanerCommand::cleanSelf))
												.then(Commands.literal("discard")
														.executes(ContainerCleanerCommand::cleanDiscard))
												.then(Commands.literal("to")
														.then(Commands.argument("receiverStart", BlockPosArgument.blockPos())
																.then(Commands.argument("receiverEnd", BlockPosArgument.blockPos())
																		.executes(ContainerCleanerCommand::cleanToRange)))))))));
	}

	private static int cleanSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		int result = clean(context, targets -> stack -> ItemTransfer.insertIntoInventory(player.getInventory(), stack));
		player.inventoryMenu.broadcastChanges();
		return result;
	}

	private static int cleanDiscard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return clean(context, targets -> stack -> ItemStack.EMPTY);
	}

	private static int cleanToRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerLevel level = context.getSource().getLevel();
		BlockPos receiverStart = BlockPosArgument.getBlockPos(context, "receiverStart");
		BlockPos receiverEnd = BlockPosArgument.getBlockPos(context, "receiverEnd");

		return clean(context, targets -> {
			Set<Container> excluded = Collections.newSetFromMap(new IdentityHashMap<>());
			excluded.addAll(targets);
			List<Container> receivers = ItemTransfer.findContainers(
					level,
					receiverStart,
					receiverEnd,
					container -> !excluded.contains(container)
							&& !(container instanceof AbstractFurnaceBlockEntity)
			);
			return stack -> ItemTransfer.insertIntoContainers(receivers, stack);
		});
	}

	private static int clean(CommandContext<CommandSourceStack> context, ReceiverFactory receiverFactory)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos start = BlockPosArgument.getBlockPos(context, "start");
		BlockPos end = BlockPosArgument.getBlockPos(context, "end");
		Target target = target(source, context);
		if (target == null) {
			return 0;
		}

		ContainerCleaner.ScanResult scan = ContainerCleaner.findContainers(level, start, end, target.block());
		if (scan.matched() == 0) {
			source.sendFailure(Component.translatable("cwa.container.none_container", target.blockName()));
			return 0;
		}

		ItemReceiver receiver = receiverFactory.create(scan.containers());
		ContainerCleaner.Result result = ContainerCleaner.clean(scan.containers(), target.item(), receiver);
		sendResult(source, target, result);
		return result.removed() > 0 ? 1 : 0;
	}

	private static @Nullable Target target(CommandSourceStack source, CommandContext<CommandSourceStack> context) {
		String containerInput = StringArgumentType.getString(context, "container");
		String ashInput = StringArgumentType.getString(context, "ash");

		Identifier containerId = Identifier.tryParse(containerInput);
		if (containerId == null) {
			source.sendFailure(Component.translatable("cwa.container.invalid_id", containerInput));
			return null;
		}
		if (!BuiltInRegistries.BLOCK.containsKey(containerId)) {
			source.sendFailure(Component.translatable("cwa.container.unknown_block", containerInput));
			return null;
		}

		Block block = BuiltInRegistries.BLOCK.getValue(containerId);
		if (!ContainerCleaner.isContainerBlock(block)) {
			source.sendFailure(Component.translatable("cwa.container.not_container", block.getName()));
			return null;
		}

		Identifier ashId = Identifier.tryParse(ashInput);
		if (ashId == null) {
			source.sendFailure(Component.translatable("cwa.container.invalid_id", ashInput));
			return null;
		}
		if (!BuiltInRegistries.ITEM.containsKey(ashId)) {
			source.sendFailure(Component.translatable("cwa.container.unknown_item", ashInput));
			return null;
		}

		return new Target(block, BuiltInRegistries.ITEM.getValue(ashId));
	}

	private static void sendResult(CommandSourceStack source, Target target, ContainerCleaner.Result result) {
		Component blockName = target.blockName();
		Component itemName = target.itemName();

		if (result.found() == 0) {
			source.sendSuccess(() -> Component.translatable(
					"cwa.container.none_item",
					blockName,
					itemName
			), false);
			return;
		}

		source.sendSuccess(() -> Component.translatable(
				"cwa.container.result",
				result.containers(),
				blockName,
				result.found(),
				itemName,
				result.removed()
		), false);

		if (result.leftover() > 0) {
			source.sendSuccess(() -> Component.translatable(
					"cwa.container.leftover",
					result.leftover(),
					itemName,
					result.leftoverSlots()
			).withStyle(ChatFormatting.RED), false);
		}
	}

	@FunctionalInterface
	private interface ReceiverFactory {
		ItemReceiver create(List<Container> targets);
	}

	private record Target(Block block, Item item) {
		private Component blockName() {
			return block.getName();
		}

		private Component itemName() {
			return new ItemStack(item).getHoverName();
		}
	}
}
