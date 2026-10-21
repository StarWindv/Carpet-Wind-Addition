package top.starwindv.cwa.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import top.starwindv.cwa.noupdate.NoUpdateData;
import top.starwindv.cwa.noupdate.NoUpdateManager;
import top.starwindv.cwa.noupdate.NoUpdateRegion;
import top.starwindv.cwa.util.CwaMessages;

public final class NoUpdateCommand {
	private NoUpdateCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("carpet").then(Commands.literal("NoUpdate")
				.then(Commands.literal("range")
						.then(Commands.argument("start", BlockPosArgument.blockPos())
								.then(Commands.argument("end", BlockPosArgument.blockPos())
										.executes(context -> addRegion(context, ""))
										.then(Commands.argument("alias", StringArgumentType.word())
												.executes(context -> addRegion(context, StringArgumentType.getString(context, "alias")))))))
				.then(Commands.literal("list")
						.executes(NoUpdateCommand::listRegions))
				.then(Commands.literal("remove")
						.then(Commands.argument("target", StringArgumentType.word())
								.suggests(NoUpdateCommand::suggestTargets)
								.executes(context -> removeRegion(context, StringArgumentType.getString(context, "target")))))
				.then(Commands.literal("enable")
						.then(Commands.argument("target", StringArgumentType.word())
								.suggests(NoUpdateCommand::suggestTargets)
								.executes(context -> setRegionEnabled(context, StringArgumentType.getString(context, "target"), true))))
				.then(Commands.literal("disable")
						.then(Commands.argument("target", StringArgumentType.word())
								.suggests(NoUpdateCommand::suggestTargets)
								.executes(context -> setRegionEnabled(context, StringArgumentType.getString(context, "target"), false))))));
	}

	private static int addRegion(CommandContext<CommandSourceStack> context, String alias) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		NoUpdateData data = NoUpdateData.get(level);

		if (!alias.isEmpty()) {
			if (alias.equals("-")) {
				source.sendFailure(CwaMessages.translatable("cwa.noupdate.alias.dash"));
				return 0;
			}
			if (alias.matches("\\d+")) {
				source.sendFailure(CwaMessages.translatable("cwa.noupdate.alias.numeric"));
				return 0;
			}
			if (data != null && data.hasAlias(alias)) {
				source.sendFailure(CwaMessages.translatable("cwa.noupdate.alias.taken", alias));
				return 0;
			}
		}

		BlockPos start = BlockPosArgument.getBlockPos(context, "start");
		BlockPos end = BlockPosArgument.getBlockPos(context, "end");
		NoUpdateRegion overlap = data == null ? null : data.findOverlapping(start, end);

		if (overlap != null) {
			source.sendFailure(CwaMessages.translatable(
					"cwa.noupdate.overlap",
					overlap.id(),
					overlap.displayAlias(),
					overlap.startString(),
					overlap.endString()
			));
			return 0;
		}

		NoUpdateRegion region = NoUpdateData.getOrCreate(level).addRegion(alias, start, end);
		NoUpdateManager.refresh(source.getServer());

		source.sendSuccess(() -> CwaMessages.translatable(
				"cwa.noupdate.added",
				region.id(),
				region.displayAlias(),
				region.startString(),
				region.endString(),
				CwaMessages.translatable("cwa.noupdate.status.disabled")
		), false);
		return 1;
	}

	private static int listRegions(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		NoUpdateData data = NoUpdateData.get(level);

		if (data == null || data.regions().isEmpty()) {
			source.sendSuccess(() -> CwaMessages.translatable("cwa.noupdate.list.empty"), false);
			return 0;
		}

		source.sendSuccess(() -> CwaMessages.translatable(
				"cwa.noupdate.list.header",
				level.dimension().location().toString()
		), false);
		for (NoUpdateRegion region : data.regions().stream().sorted(Comparator.comparingInt(NoUpdateRegion::id)).toList()) {
			Component status = CwaMessages.translatable(
					region.enabled() ? "cwa.noupdate.status.enabled" : "cwa.noupdate.status.disabled"
			);
			source.sendSuccess(() -> CwaMessages.translatable(
					"cwa.noupdate.list.entry",
					region.id(),
					region.displayAlias(),
					status,
					region.startString(),
					region.endString()
			), false);
		}
		return data.regions().size();
	}

	private static int setRegionEnabled(CommandContext<CommandSourceStack> context, String target, boolean enabled) {
		CommandSourceStack source = context.getSource();
		NoUpdateData data = NoUpdateData.get(source.getLevel());
		NoUpdateRegion region = data == null ? null : data.find(target);

		if (region == null) {
			source.sendFailure(CwaMessages.translatable("cwa.noupdate.not_found", target));
			return 0;
		}

		data.setEnabled(region, enabled);
		NoUpdateManager.refresh(source.getServer());
		source.sendSuccess(() -> CwaMessages.translatable(
				enabled ? "cwa.noupdate.enabled" : "cwa.noupdate.disabled",
				region.id(),
				region.displayAlias()
		), false);
		return 1;
	}

	private static int removeRegion(CommandContext<CommandSourceStack> context, String target) {
		CommandSourceStack source = context.getSource();
		NoUpdateData data = NoUpdateData.get(source.getLevel());
		NoUpdateRegion region = data == null ? null : data.find(target);

		if (region == null) {
			source.sendFailure(CwaMessages.translatable("cwa.noupdate.not_found", target));
			return 0;
		}

		data.remove(region);
		NoUpdateManager.refresh(source.getServer());
		source.sendSuccess(() -> CwaMessages.translatable(
				"cwa.noupdate.removed",
				region.id(),
				region.displayAlias(),
				region.startString(),
				region.endString()
		), false);
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestTargets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		NoUpdateData data = NoUpdateData.get(context.getSource().getLevel());
		if (data == null) {
			return builder.buildFuture();
		}
		for (NoUpdateRegion region : data.regions()) {
			builder.suggest(Integer.toString(region.id()));
			if (!region.alias().isEmpty()) {
				builder.suggest(region.alias());
			}
		}
		return builder.buildFuture();
	}
}
