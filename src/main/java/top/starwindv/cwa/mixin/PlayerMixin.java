package top.starwindv.cwa.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
	private void cwa$neverFull(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
		if (CwaSettings.NeverFull) {
			cir.setReturnValue(true);
		}
	}
}
