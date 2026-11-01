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
import top.starwindv.cwa.CarpetWindAddition;
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

		try {
			cwa$fillUsableCommands(source, target, sourceStack, converted, false);
			ci.cancel();
		} catch (Throwable throwable) {
			// Never make /allow fail just because a third-party command node
			// cannot be copied. Vanilla will run as a fallback; the guest may
			// lose the extra allowed nodes but keeps a valid command tree.
			CarpetWindAddition.LOGGER.warn("Failed to add allowed commands to the command tree", throwable);
		}
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
					CommandNode redirect = (CommandNode) converted.get(builder.getRedirect());
					// A redirect target can be outside the copied subtree when
					// a command is made visible through /allow. Leaving the
					// redirect unset is safer than passing null to Brigadier.
					if (redirect != null) {
						builder.redirect(redirect);
					}
				}

				CommandNode node = builder.build();
				converted.put(child, node);
				if (target.getChild(node.getName()) == null) {
					target.addChild(node);
				}

				if (!child.getChildren().isEmpty()) {
					cwa$fillUsableCommands(child, node, commandFilter, converted, allowed);
				}
			}
		}
	}
}
