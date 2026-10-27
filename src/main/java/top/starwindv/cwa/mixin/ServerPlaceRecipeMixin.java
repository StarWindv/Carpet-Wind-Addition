package top.starwindv.cwa.mixin;

import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.crafting.NearbyCraftingContext;
import top.starwindv.cwa.crafting.NearbyCraftingManager;

@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
	private static final String PLACE_RECIPE = "placeRecipe(Lnet/minecraft/recipebook/ServerPlaceRecipe$CraftingMenuAccess;IILjava/util/List;Ljava/util/List;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/crafting/RecipeHolder;ZZ)Lnet/minecraft/world/inventory/RecipeBookMenu$PostPlaceAction;";

	@Redirect(
			method = PLACE_RECIPE,
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Inventory;fillStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V"
			)
	)
	private static void cwa$accountNearbyContainers(Inventory inventory, StackedItemContents contents) {
		inventory.fillStackedContents(contents);
		NearbyCraftingContext context = NearbyCraftingManager.current();
		if (context != null) {
			context.account(contents);
		}
	}

	@Inject(method = "moveItemToGrid", at = @At("RETURN"), cancellable = true)
	private void cwa$takeFromNearbyContainer(
			Slot targetSlot,
			Holder<Item> item,
			int count,
			CallbackInfoReturnable<Integer> cir
	) {
		if (cir.getReturnValue() != -1) {
			return;
		}

		NearbyCraftingContext context = NearbyCraftingManager.current();
		if (context == null) {
			return;
		}

		NearbyCraftingContext.Taken taken = context.take(item, targetSlot.getItem(), count);
		if (taken == null) {
			return;
		}

		ItemStack target = targetSlot.getItem();
		if (target.isEmpty()) {
			targetSlot.set(taken.stack());
		} else {
			target.grow(taken.stack().getCount());
		}
		context.record(targetSlot, taken);
		cir.setReturnValue(count - taken.stack().getCount());
	}

	@Inject(method = "clearGrid", at = @At("HEAD"))
	private void cwa$returnGridItemsBeforeClear(CallbackInfo ci) {
		NearbyCraftingContext context = NearbyCraftingManager.current();
		if (context != null) {
			context.onGridCleared();
		}
	}
}
