package top.starwindv.cwa.mixin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.starwindv.cwa.allow.AllowManager;

@Mixin(Commands.class)
public abstract class CommandsMixin {
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Inject(method = "fillUsableCommands", at = @At("HEAD"), cancellable = true)
	private static void cwa$fillUsableCommands(CommandNode source, CommandNode target, Object commandFilter,
			Map converted, CallbackInfo ci) {
		if (!(commandFilter instanceof CommandSourceStack sourceStack)) {
			return;
		}

		cwa$fillUsableCommands(source, target, sourceStack, converted, false);
		ci.cancel();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void cwa$fillUsableCommands(CommandNode source, CommandNode target, CommandSourceStack commandFilter,
			Map converted, boolean allowedRoot) {
		for (Object childObject : source.getChildren()) {
			CommandNode child = (CommandNode) childObject;
			boolean allowed = allowedRoot
					|| (source instanceof RootCommandNode && AllowManager.isAllowed(commandFilter, child.getName()));

			if (child.canUse(commandFilter) || allowed) {
				ArgumentBuilder builder = child.createBuilder();
				if (builder.getRedirect() != null) {
					builder.redirect((CommandNode) converted.get(builder.getRedirect()));
				}

				CommandNode node = builder.build();
				converted.put(child, node);
				target.addChild(node);

				if (!child.getChildren().isEmpty()) {
					cwa$fillUsableCommands(child, node, commandFilter, converted, allowed);
				}
			}
		}
	}
}
