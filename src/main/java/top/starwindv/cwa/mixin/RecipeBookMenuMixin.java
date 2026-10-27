package top.starwindv.cwa.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.starwindv.cwa.crafting.NearbyCraftingManager;

@Mixin(RecipeBookMenu.class)
public abstract class RecipeBookMenuMixin {
	@Inject(method = "handlePlacement", at = @At("HEAD"))
	private void cwa$beginNearbyCrafting(
			boolean useMaxItems,
			RecipeHolder<?> recipe,
			ServerPlayer player,
			CallbackInfo ci
	) {
		if ((Object) this instanceof CraftingMenu menu) {
			NearbyCraftingManager.begin(menu, player.getInventory());
		}
	}

	@Inject(method = "handlePlacement", at = @At("RETURN"))
	private void cwa$endNearbyCrafting(
			boolean useMaxItems,
			RecipeHolder<?> recipe,
			ServerPlayer player,
			CallbackInfo ci
	) {
		if ((Object) this instanceof CraftingMenu menu) {
			NearbyCraftingManager.sendNearbyContents(menu, player);
		}
		NearbyCraftingManager.end();
	}
}
