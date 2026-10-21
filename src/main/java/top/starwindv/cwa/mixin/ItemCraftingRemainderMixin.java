package top.starwindv.cwa.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemCraftingRemainderMixin {
	@Inject(method = "getCraftingRemainder", at = @At("HEAD"), cancellable = true)
	private void cwa$bucketRemainder(CallbackInfoReturnable<ItemStackTemplate> cir) {
		if ((Object) this == Items.BUCKET) {
			cir.setReturnValue(new ItemStackTemplate(Items.BUCKET));
		}
	}
}
