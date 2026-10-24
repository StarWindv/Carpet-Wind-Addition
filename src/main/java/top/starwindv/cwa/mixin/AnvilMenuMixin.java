package top.starwindv.cwa.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Shadow
	private int repairItemCountCost;

	@Unique
	private ItemStack cwa$stackedAdditionalInput = ItemStack.EMPTY;

	@Inject(method = "onTake", at = @At("HEAD"))
	private void cwa$captureStackedAdditionalInput(Player player, ItemStack result, CallbackInfo ci) {
		this.cwa$stackedAdditionalInput = ItemStack.EMPTY;
		if (this.repairItemCountCost > 0) {
			return;
		}

		ItemStack additional = this.cwa$additionalInput();
		if (additional.getCount() > 1) {
			this.cwa$stackedAdditionalInput = additional.copy();
		}
	}

	@Inject(method = "onTake", at = @At("RETURN"))
	private void cwa$restoreStackedAdditionalInput(Player player, ItemStack result, CallbackInfo ci) {
		ItemStack additional = this.cwa$stackedAdditionalInput;
		this.cwa$stackedAdditionalInput = ItemStack.EMPTY;
		if (additional.isEmpty()) {
			return;
		}

		if (this.cwa$additionalInput().isEmpty()) {
			((AnvilMenu) (Object) this).getSlot(AnvilMenu.ADDITIONAL_SLOT)
					.set(additional.copyWithCount(additional.getCount() - 1));
		}
	}

	@Unique
	private ItemStack cwa$additionalInput() {
		return ((AnvilMenu) (Object) this).getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
	}
}
