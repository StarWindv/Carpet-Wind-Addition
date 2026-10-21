package top.starwindv.cwa.mixin;

import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.stack.StackSizeManager;

@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {
	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	default void cwa$changeMaxStackSize(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this instanceof ItemStack stack) {
			int size = StackSizeManager.getMaxStackSize(stack);
			if (size > 0) {
				cir.setReturnValue(size);
			}
		}
	}
}
