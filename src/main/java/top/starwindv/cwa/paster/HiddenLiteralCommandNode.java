package top.starwindv.cwa.paster;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;

/**
 * A literal command node that can be typed manually but is never suggested by
 * client or server command completion.
 */
public final class HiddenLiteralCommandNode extends LiteralCommandNode<CommandSourceStack> {
	public HiddenLiteralCommandNode(
			String literal,
			Command<CommandSourceStack> command,
			Predicate<CommandSourceStack> requirement
	) {
		super(literal, command, requirement, null, null, false);
	}

	@Override
	public CompletableFuture<Suggestions> listSuggestions(
			CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder
	) {
		return Suggestions.empty();
	}
}
