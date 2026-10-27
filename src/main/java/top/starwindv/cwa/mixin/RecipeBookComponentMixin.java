package top.starwindv.cwa.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.starwindv.cwa.crafting.NearbyCraftingClient;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	@Shadow
	@Final
	private StackedItemContents stackedContents;

	@Shadow
	@Final
	protected RecipeBookMenu menu;

	@Inject(
			method = "initVisuals",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V",
					shift = At.Shift.AFTER
			)
	)
	private void cwa$addNearbyOnInit(CallbackInfo ci) {
		if (this.menu instanceof CraftingMenu) {
			NearbyCraftingClient.account(this.stackedContents::accountSimpleStack);
		}
	}

	@Inject(
			method = "updateStackedContents",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V",
					shift = At.Shift.AFTER
			)
	)
	private void cwa$addNearbyOnUpdate(CallbackInfo ci) {
		if (this.menu instanceof CraftingMenu) {
			NearbyCraftingClient.account(this.stackedContents::accountSimpleStack);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void cwa$refreshNearbyRecipes(CallbackInfo ci) {
		if (NearbyCraftingClient.consumeDirty() && ((RecipeBookComponent<?>) (Object) this).isVisible()) {
			((RecipeBookComponentInvoker) (Object) this).cwa$updateStackedContents();
		}
	}
}
