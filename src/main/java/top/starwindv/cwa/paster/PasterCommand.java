package top.starwindv.cwa.paster;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.starwindv.cwa.allow.AllowManager;

public final class PasterCommand {
	private static final int MATERIALS_PER_PAGE = 10;
	private static final int COUNT_WIDTH = 5;

	private PasterCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("paster")
				.requires(source -> LitematicaBridge.isAvailable()
						&& LitematicaSettings.LitematicaPaster
						&& AllowManager.isHost(source))
				.then(Commands.literal("collect")
						.then(Commands.literal("self")
								.executes(PasterCommand::collectSelf))
						.then(Commands.literal("list")
								.executes(PasterCommand::listMaterials)
								.then(Commands.argument("page", IntegerArgumentType.integer(1))
										.executes(PasterCommand::listMaterialsPage)))
						.then(Commands.argument("start", BlockPosArgument.blockPos())
								.then(Commands.argument("end", BlockPosArgument.blockPos())
										.executes(PasterCommand::collectRange))))
				.then(Commands.literal("paste")
						.executes(PasterCommand::paste))
				.then(Commands.literal("return")
						.then(Commands.literal("self")
								.executes(PasterCommand::returnSelf))
						.then(Commands.argument("start", BlockPosArgument.blockPos())
								.then(Commands.argument("end", BlockPosArgument.blockPos())
										.executes(PasterCommand::returnRange)))));
	}

	private static int collectSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		PlacementSnapshot snapshot = snapshot(source);
		if (snapshot == null) {
			return 0;
		}

		PasterManager.CollectResult result = PasterManager.collectSelf(player, snapshot);
		sendCollectResult(source, result);
		sendProtectionMessage(source);
		return result.collected() > 0 ? 1 : 0;
	}

	private static int collectRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		PlacementSnapshot snapshot = snapshot(source);
		if (snapshot == null) {
			return 0;
		}

		BlockPos start = BlockPosArgument.getBlockPos(context, "start");
		BlockPos end = BlockPosArgument.getBlockPos(context, "end");
		PasterManager.CollectResult result = PasterManager.collectRange(
				source.getLevel(),
				player,
				snapshot,
				start,
				end
		);
		sendCollectResult(source, result);
		sendProtectionMessage(source);
		return result.collected() > 0 ? 1 : 0;
	}

	private static int listMaterials(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return listMaterials(context, 1);
	}

	private static int listMaterialsPage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return listMaterials(context, IntegerArgumentType.getInteger(context, "page"));
	}

	private static int listMaterials(CommandContext<CommandSourceStack> context, int requestedPage) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		PlacementSnapshot snapshot = snapshot(source);
		if (snapshot == null) {
			return 0;
		}

		PasterData data = PasterData.get(source.getServer());
		List<ItemStack> stored = data == null ? List.of() : data.get(snapshot.id());
		List<MaterialLine> lines = materialLines(snapshot, stored);
		if (lines.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("cwa.paster.list.empty"), false);
			return 0;
		}

		int pages = (lines.size() + MATERIALS_PER_PAGE - 1) / MATERIALS_PER_PAGE;
		int page = Math.min(Math.max(1, requestedPage), pages);
		int requiredCount = 0;
		int missingCount = 0;
		for (MaterialLine line : lines) {
			if (!line.extra()) {
				requiredCount += line.total();
				missingCount += line.missing();
			}
		}

		int requiredTotal = requiredCount;
		int missingTotal = missingCount;
		int storedCount = PasterMaterials.totalCount(stored);
		int from = (page - 1) * MATERIALS_PER_PAGE;
		int to = Math.min(from + MATERIALS_PER_PAGE, lines.size());
		boolean hasExtra = false;
		for (int index = from; index < to; index++) {
			if (lines.get(index).extra()) {
				hasExtra = true;
				break;
			}
		}

		MutableComponent table = Component.empty();
		table.append(Component.translatable("cwa.paster.list.header", snapshot.name()).withStyle(ChatFormatting.GOLD));
		table.append(Component.literal("\n"));
		table.append(Component.translatable(
				"cwa.paster.list.summary",
				requiredTotal,
				storedCount,
				missingTotal
		).withStyle(ChatFormatting.YELLOW));
		table.append(Component.literal("\n"));
		table.append(Component.translatable("cwa.paster.list.page", page, pages).withStyle(ChatFormatting.GRAY));
		table.append(Component.literal("\n"));

		table.append(Component.translatable(
				"cwa.paster.list.columns",
				Component.literal(" ").append(Component.translatable("cwa.paster.list.column.required")),
				Component.literal(" ").append(Component.translatable("cwa.paster.list.column.stored")),
				Component.literal(" ").append(Component.translatable("cwa.paster.list.column.missing")),
				Component.translatable("cwa.paster.list.column.item")
		).withStyle(ChatFormatting.AQUA));
		table.append(Component.literal("\n"));

		if (hasExtra) {
			table.append(Component.translatable("cwa.paster.list.legend").withStyle(ChatFormatting.DARK_GRAY));
			table.append(Component.literal("\n"));
		}

		for (int index = from; index < to; index++) {
			MaterialLine line = lines.get(index);
			ChatFormatting rowColor = (index - from) % 2 == 0 ? ChatFormatting.GRAY : ChatFormatting.WHITE;
			Component name = line.extra()
					? Component.literal("* ").append(line.sample().getHoverName())
					: line.sample().getHoverName();
			Component missing = line.extra()
					? Component.literal(padLeft("-", COUNT_WIDTH)).withStyle(ChatFormatting.DARK_GRAY)
					: Component.literal(padLeft(Integer.toString(line.missing()), COUNT_WIDTH))
							.withStyle(line.missing() > 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
			table.append(Component.translatable(
					"cwa.paster.list.row",
					line.extra() ? padLeft("-", COUNT_WIDTH) : padLeft(Integer.toString(line.total()), COUNT_WIDTH),
					padLeft(Integer.toString(line.stored()), COUNT_WIDTH),
					missing,
					name
			).withStyle(rowColor));
			if (index + 1 < to) {
				table.append(Component.literal("\n"));
			}
		}

		source.sendSuccess(() -> table, false);
		return lines.size();
	}

	private static String padLeft(String value, int width) {
		return value.length() >= width ? value : " ".repeat(width - value.length()) + value;
	}

	private static List<MaterialLine> materialLines(PlacementSnapshot snapshot, List<ItemStack> stored) {
		List<MaterialLine> lines = new ArrayList<>();
		for (PasterMaterial material : snapshot.requiredMaterials()) {
			int storedCount = PasterMaterials.count(stored, material.sample());
			lines.add(new MaterialLine(
					material.sample().copyWithCount(1),
					material.count(),
					storedCount,
					Math.max(0, material.count() - storedCount),
					false
			));
		}

		for (ItemStack stack : stored) {
			if (stack.isEmpty() || isRequired(snapshot.requiredMaterials(), stack)) {
				continue;
			}

			int extraIndex = -1;
			for (int index = 0; index < lines.size(); index++) {
				MaterialLine line = lines.get(index);
				if (line.extra() && ItemStack.isSameItemSameComponents(line.sample(), stack)) {
					extraIndex = index;
					break;
				}
			}

			if (extraIndex >= 0) {
				MaterialLine line = lines.get(extraIndex);
				lines.set(extraIndex, new MaterialLine(
						line.sample(),
						0,
						line.stored() + stack.getCount(),
						0,
						true
				));
			} else {
				lines.add(new MaterialLine(stack.copyWithCount(1), 0, stack.getCount(), 0, true));
			}
		}

		lines.sort(
				Comparator.comparing(MaterialLine::extra)
						.thenComparing(Comparator.comparingInt(MaterialLine::missing).reversed())
						.thenComparing(Comparator.comparingInt(MaterialLine::stored).reversed())
						.thenComparing(line -> line.sample().getItem().getDescriptionId())
		);
		return lines;
	}

	private static boolean isRequired(List<PasterMaterial> required, ItemStack stack) {
		for (PasterMaterial material : required) {
			if (ItemStack.isSameItemSameComponents(material.sample(), stack)) {
				return true;
			}
		}
		return false;
	}

	private static int paste(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		PlacementSnapshot snapshot = snapshot(source);
		if (snapshot == null) {
			return 0;
		}

		return PasterManager.startPaste(source, player, snapshot) ? 1 : 0;
	}

	private static int returnSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		PlacementSnapshot snapshot = snapshot(source);
		if (snapshot == null) {
			return 0;
		}

		PasterManager.ReturnResult result = PasterManager.returnSelf(player, snapshot);
		sendReturnMessage(source, result);
		PasterManager.releaseProtectionIfEmpty(source.getServer());
		return result.returned() > 0 ? 1 : 0;
	}

	private static int returnRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		PlacementSnapshot snapshot = snapshot(source);
		if (snapshot == null) {
			return 0;
		}

		BlockPos start = BlockPosArgument.getBlockPos(context, "start");
		BlockPos end = BlockPosArgument.getBlockPos(context, "end");
		PasterManager.ReturnResult result = PasterManager.returnRange(
				source.getLevel(),
				snapshot,
				start,
				end
		);
		sendReturnMessage(source, result);
		PasterManager.releaseProtectionIfEmpty(source.getServer());
		return result.returned() > 0 ? 1 : 0;
	}

	private static void sendCollectResult(CommandSourceStack source, PasterManager.CollectResult result) {
		if (result.materials().isEmpty()) {
			source.sendSuccess(() -> Component.translatable(
					"cwa.paster.collect.none",
					result.missing()
			).withStyle(ChatFormatting.GRAY), false);
			return;
		}

		MutableComponent table = Component.empty();
		table.append(Component.translatable(
				"cwa.paster.collect.header",
				result.materials().size(),
				result.collected(),
				result.missing()
		).withStyle(ChatFormatting.GOLD));
		table.append(Component.literal("\n"));
		table.append(Component.translatable(
				"cwa.paster.collect.columns",
				Component.literal(" ").append(Component.translatable("cwa.paster.collect.column.collected")),
				Component.literal(" ").append(Component.translatable("cwa.paster.collect.column.missing")),
				Component.translatable("cwa.paster.list.column.item")
		).withStyle(ChatFormatting.AQUA));
		table.append(Component.literal("\n"));

		for (int index = 0; index < result.materials().size(); index++) {
			PasterManager.CollectedMaterial material = result.materials().get(index);
			ChatFormatting rowColor = index % 2 == 0 ? ChatFormatting.GRAY : ChatFormatting.WHITE;
			Component missing = Component.literal(padLeft(Integer.toString(material.missing()), COUNT_WIDTH))
					.withStyle(material.missing() > 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
			table.append(Component.translatable(
					"cwa.paster.collect.row",
					padLeft(Integer.toString(material.collected()), COUNT_WIDTH),
					missing,
					material.sample().getHoverName()
			).withStyle(rowColor));
			if (index + 1 < result.materials().size()) {
				table.append(Component.literal("\n"));
			}
		}

		source.sendSuccess(() -> table, false);
	}

	private static void sendReturnMessage(CommandSourceStack source, PasterManager.ReturnResult result) {
		if (result.returned() <= 0 && result.remaining() <= 0) {
			source.sendSuccess(() -> Component.translatable("cwa.paster.return.none"), false);
			return;
		}

		source.sendSuccess(() -> Component.translatable(
				"cwa.paster.return.result",
				result.returned(),
				result.remaining()
		), false);
	}

	private static void sendProtectionMessage(CommandSourceStack source) {
		PasterData data = PasterData.get(source.getServer());
		if (data == null || !data.hasAny()) {
			return;
		}

		if (PasterManager.protectProjection()) {
			source.sendSuccess(() -> Component.translatable("cwa.paster.protection.enabled"), false);
		} else {
			source.sendFailure(Component.translatable("cwa.paster.protection.failed"));
		}
	}

	private static PlacementSnapshot snapshot(CommandSourceStack source) {
		PlacementSnapshot snapshot = LitematicaBridge.currentPlacement();
		if (snapshot == null) {
			source.sendFailure(Component.translatable("cwa.paster.no_projection"));
			return null;
		}

		ServerLevel level = source.getLevel();
		String dimension = level.dimension().identifier().toString();
		if (!dimension.equals(snapshot.dimension())) {
			source.sendFailure(Component.translatable("cwa.paster.wrong_dimension"));
			return null;
		}
		return snapshot;
	}

	private record MaterialLine(ItemStack sample, int total, int stored, int missing, boolean extra) {
	}
}
