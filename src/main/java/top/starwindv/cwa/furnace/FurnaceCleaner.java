package top.starwindv.cwa.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import top.starwindv.cwa.util.ItemReceiver;

public final class FurnaceCleaner {
	private static final int SLOT_INPUT = 0;
	private static final int SLOT_FUEL = 1;
	private static final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> SMELTING =
			RecipeManager.createCheck(RecipeType.SMELTING);
	private static final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> BLASTING =
			RecipeManager.createCheck(RecipeType.BLASTING);
	private static final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> SMOKING =
			RecipeManager.createCheck(RecipeType.SMOKING);

	private FurnaceCleaner() {
	}

	public static Result clean(ServerLevel level, BlockPos first, BlockPos second, ItemReceiver receiver) {
		int furnaces = 0;
		int found = 0;
		int removed = 0;
		int leftover = 0;
		int leftoverSlots = 0;

		for (BlockPos pos : BlockPos.betweenClosed(first, second)) {
			if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) {
				continue;
			}

			boolean furnaceHasAsh = false;
			boolean changed = false;

			for (int slot = SLOT_INPUT; slot <= SLOT_FUEL; slot++) {
				ItemStack stack = furnace.getItem(slot);
				if (stack.isEmpty() || !isAsh(level, furnace, slot, stack)) {
					continue;
				}

				furnaceHasAsh = true;
				found += stack.getCount();
				ItemStack leftoverStack = receiver.accept(stack.copy());
				int moved = stack.getCount() - leftoverStack.getCount();

				if (moved > 0) {
					furnace.setItem(slot, leftoverStack.isEmpty() ? ItemStack.EMPTY : leftoverStack);
					removed += moved;
					changed = true;
				}
				if (!leftoverStack.isEmpty()) {
					leftover += leftoverStack.getCount();
					leftoverSlots++;
				}
			}

			if (furnaceHasAsh) {
				furnaces++;
			}
			if (changed) {
				furnace.setChanged();
			}
		}

		return new Result(furnaces, found, removed, leftover, leftoverSlots);
	}

	private static boolean isAsh(ServerLevel level, AbstractFurnaceBlockEntity furnace, int slot, ItemStack stack) {
		if (slot == SLOT_INPUT) {
			return !canSmelt(level, furnace, stack);
		}
		return level.fuelValues().burnDuration(stack) <= 0;
	}

	private static boolean canSmelt(ServerLevel level, AbstractFurnaceBlockEntity furnace, ItemStack stack) {
		RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> check;
		if (furnace instanceof BlastFurnaceBlockEntity) {
			check = BLASTING;
		} else if (furnace instanceof SmokerBlockEntity) {
			check = SMOKING;
		} else {
			check = SMELTING;
		}
		return check.getRecipeFor(new SingleRecipeInput(stack), level).isPresent();
	}

	public record Result(int furnaces, int found, int removed, int leftover, int leftoverSlots) {
	}
}
