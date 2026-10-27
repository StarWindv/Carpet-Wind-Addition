package top.starwindv.cwa.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.crafting.NearbyCraftingManager;

@Mixin(AbstractCraftingMenu.class)
public abstract class AbstractCraftingMenuMixin {
	@Inject(method = "handlePlacement", at = @At("HEAD"))
	private void cwa$beginNearbyCrafting(
			boolean useMaxItems,
			boolean allowDroppingItemsToClear,
			RecipeHolder<?> recipe,
			ServerLevel level,
			Inventory inventory,
			CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir
	) {
		if ((Object) this instanceof CraftingMenu menu) {
			NearbyCraftingManager.begin(menu, inventory);
		}
	}

	@Inject(method = "handlePlacement", at = @At("RETURN"))
	private void cwa$endNearbyCrafting(
			boolean useMaxItems,
			boolean allowDroppingItemsToClear,
			RecipeHolder<?> recipe,
			ServerLevel level,
			Inventory inventory,
			CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir
	) {
		if ((Object) this instanceof CraftingMenu menu) {
			NearbyCraftingManager.sendNearbyContents(menu, inventory.player);
		}
		NearbyCraftingManager.end();
	}
}
