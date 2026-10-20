package top.starwindv.cwa.paster;

import java.util.Iterator;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class PasterMaterials {
	private PasterMaterials() {
	}

	public static void add(List<ItemStack> stacks, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		ItemStack remaining = stack.copy();
		for (ItemStack stored : stacks) {
			if (!ItemStack.isSameItemSameComponents(stored, remaining)) {
				continue;
			}

			int space = stored.getMaxStackSize() - stored.getCount();
			if (space <= 0) {
				continue;
			}

			int moved = Math.min(space, remaining.getCount());
			stored.grow(moved);
			remaining.shrink(moved);
			if (remaining.isEmpty()) {
				return;
			}
		}

		while (!remaining.isEmpty()) {
			int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
			stacks.add(remaining.copyWithCount(moved));
			remaining.shrink(moved);
		}
	}

	public static int count(List<ItemStack> stacks, ItemStack sample) {
		int count = 0;
		for (ItemStack stack : stacks) {
			if (ItemStack.isSameItemSameComponents(stack, sample)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	public static int take(List<ItemStack> stacks, ItemStack sample, int amount) {
		if (amount <= 0) {
			return 0;
		}

		int remaining = amount;
		Iterator<ItemStack> iterator = stacks.iterator();
		while (iterator.hasNext() && remaining > 0) {
			ItemStack stack = iterator.next();
			if (!ItemStack.isSameItemSameComponents(stack, sample)) {
				continue;
			}

			int taken = Math.min(remaining, stack.getCount());
			stack.shrink(taken);
			remaining -= taken;
			if (stack.isEmpty()) {
				iterator.remove();
			}
		}
		return amount - remaining;
	}

	public static int totalCount(List<ItemStack> stacks) {
		int count = 0;
		for (ItemStack stack : stacks) {
			count += stack.getCount();
		}
		return count;
	}
}
