package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
	private static final int CWA_FUEL_SLOT = 1;

	@Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
	private void cwa$rejectStackedLavaFuel(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (cwa$isStackedLava(slot, stack)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
	private void cwa$rejectStackedLavaFuelThroughFace(
			int slot,
			ItemStack stack,
			Direction direction,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (cwa$isStackedLava(slot, stack)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "serverTick", at = @At("HEAD"))
	private static void cwa$limitStackedLavaFuel(
			Level level,
			BlockPos pos,
			BlockState state,
			AbstractFurnaceBlockEntity furnace,
			CallbackInfo ci
	) {
		ItemStack fuel = furnace.getItem(CWA_FUEL_SLOT);
		if (fuel.is(Items.LAVA_BUCKET) && fuel.getCount() > 1) {
			int extra = fuel.getCount() - 1;
			furnace.setItem(CWA_FUEL_SLOT, fuel.copyWithCount(1));
			Block.popResource(level, pos, fuel.copyWithCount(extra));
		}
	}

	private static boolean cwa$isStackedLava(int slot, ItemStack stack) {
		return slot == CWA_FUEL_SLOT && stack.is(Items.LAVA_BUCKET) && stack.getCount() > 1;
	}
}
