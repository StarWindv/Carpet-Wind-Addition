package top.starwindv.cwa.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.stack.StackSizeManager;

@Mixin(ItemStack.class)
public abstract class ItemInstanceMixin {
	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	private void cwa$changeMaxStackSize(CallbackInfoReturnable<Integer> cir) {
		int size = StackSizeManager.getMaxStackSize((ItemStack) (Object) this);
		if (size > 0) {
			cir.setReturnValue(size);
		}
	}
}
