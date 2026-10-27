package top.starwindv.cwa.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.entity.player.StackedItemContents;
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

	@Inject(
			method = "initVisuals",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V",
					shift = At.Shift.AFTER
			)
	)
	private void cwa$addNearbyOnInit(CallbackInfo ci) {
		NearbyCraftingClient.account(this.stackedContents::accountSimpleStack);
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
		NearbyCraftingClient.account(this.stackedContents::accountSimpleStack);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void cwa$refreshNearbyRecipes(CallbackInfo ci) {
		if (NearbyCraftingClient.consumeDirty() && ((RecipeBookComponent<?>) (Object) this).isVisible()) {
			((RecipeBookComponentInvoker) (Object) this).cwa$updateStackedContents();
		}
	}
}
