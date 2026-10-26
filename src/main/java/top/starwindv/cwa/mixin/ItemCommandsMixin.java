package top.starwindv.cwa.mixin;

import net.minecraft.server.commands.item.ItemCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemCommands.class)
public abstract class ItemCommandsMixin {
	@ModifyArgs(
			method = "lambda$wrapSetItems$0",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/brigadier/arguments/IntegerArgumentType;integer(II)Lcom/mojang/brigadier/arguments/IntegerArgumentType;",
					ordinal = 0
			)
	)
	private static void cwa$raiseItemCountLimits0(Args args) {
		args.set(1, Integer.MAX_VALUE);
	}
}
