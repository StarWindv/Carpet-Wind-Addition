package top.starwindv.cwa.mixin;

import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public interface ContainerMixin {
	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	private void cwa$useConfiguredStackLimit(CallbackInfoReturnable<Integer> cir) {
		// The per-item limit is provided by ItemStack#getMaxStackSize. The
		// vanilla container default of 99 must not override larger configured
		// stack sizes (for example 999).
		cir.setReturnValue(Integer.MAX_VALUE);
	}
}
