package top.starwindv.cwa.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemCraftingRemainderMixin {
	@Inject(method = "hasCraftingRemainingItem", at = @At("HEAD"), cancellable = true)
	private void cwa$bucketHasRemainder(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this == Items.BUCKET) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getCraftingRemainingItem", at = @At("HEAD"), cancellable = true)
	private void cwa$bucketRemainder(CallbackInfoReturnable<Item> cir) {
		if ((Object) this == Items.BUCKET) {
			cir.setReturnValue(Items.BUCKET);
		}
	}
}
