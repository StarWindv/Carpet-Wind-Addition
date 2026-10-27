package top.starwindv.cwa.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.starwindv.cwa.crafting.NearbyCraftingManager;
import top.starwindv.cwa.crafting.NearbyCraftingMenu;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin implements NearbyCraftingMenu {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Override
	public ContainerLevelAccess cwa$getAccess() {
		return this.access;
	}

	@Inject(
			method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL")
	)
	private void cwa$sendNearbyContents(
			int containerId,
			Inventory inventory,
			ContainerLevelAccess access,
			CallbackInfo ci
	) {
		NearbyCraftingManager.sendNearbyContents((CraftingMenu) (Object) this, inventory.player);
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void cwa$returnNearbyCraftingItems(Player player, CallbackInfo ci) {
		NearbyCraftingManager.onMenuRemoved((CraftingMenu) (Object) this);
		NearbyCraftingManager.clearClient(player);
	}
}
