package top.starwindv.cwa.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import top.starwindv.cwa.util.ItemTransfer;

public final class NearbyCraftingContext {
	private final ServerLevel level;
	private final BlockPos tablePos;
	private final int radius;
	private List<NearbyCraftingManager.ContainerEntry> containers;
	private final List<Origin> origins = new ArrayList<>();

	NearbyCraftingContext(
			ServerLevel level,
			BlockPos tablePos,
			int radius,
			List<NearbyCraftingManager.ContainerEntry> containers
	) {
		this.level = level;
		this.tablePos = tablePos;
		this.radius = radius;
		this.containers = List.copyOf(containers);
	}

	public boolean matches(ServerLevel level, BlockPos tablePos, int radius) {
		return this.level == level && this.tablePos.equals(tablePos) && this.radius == radius;
	}

	void setContainers(List<NearbyCraftingManager.ContainerEntry> containers) {
		this.containers = List.copyOf(containers);
	}

	public void account(StackedItemContents contents) {
		for (NearbyCraftingManager.ContainerEntry entry : this.containers) {
			Container container = entry.container();
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (!stack.isEmpty()) {
					contents.accountSimpleStack(stack);
				}
			}
		}
	}

	public @Nullable Taken take(Holder<Item> item, ItemStack target, int count) {
		for (NearbyCraftingManager.ContainerEntry entry : this.containers) {
			if (!stillPresent(entry)) {
				continue;
			}

			Container container = entry.container();
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.isEmpty()
						|| !stack.is(item.value())
						|| !Inventory.isUsableForCrafting(stack)
						|| (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, stack))) {
					continue;
				}

				int moved = Math.min(count, stack.getCount());
				if (moved <= 0) {
					continue;
				}

				ItemStack taken = stack.copyWithCount(moved);
				stack.shrink(moved);
				container.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				container.setChanged();
				return new Taken(entry, slot, taken);
			}
		}
		return null;
	}

	public void record(Slot gridSlot, Taken taken) {
		this.origins.add(new Origin(gridSlot, taken.entry(), taken.slot(), taken.stack()));
	}

	public void onGridCleared() {
		returnToOrigins();
		this.origins.clear();
	}

	public void onMenuRemoved() {
		returnToOrigins();
		this.origins.clear();
	}

	private void returnToOrigins() {
		for (Origin origin : this.origins) {
			ItemStack gridStack = origin.gridSlot().getItem();
			if (gridStack.isEmpty()
					|| !ItemStack.isSameItemSameComponents(gridStack, origin.sample())
					|| !stillPresent(origin.entry())) {
				continue;
			}

			int amount = Math.min(origin.count(), gridStack.getCount());
			if (amount <= 0) {
				continue;
			}

			ItemStack returning = origin.sample().copyWithCount(amount);
			int moved = insertIntoOriginalSlot(origin, returning);
			if (moved < amount) {
				ItemStack leftover = ItemTransfer.insertIntoContainer(
						origin.entry().container(),
						returning.copyWithCount(amount - moved)
				);
				moved += amount - moved - leftover.getCount();
			}

			if (moved > 0) {
				origin.gridSlot().set(gridStack.getCount() == moved
						? ItemStack.EMPTY
						: gridStack.copyWithCount(gridStack.getCount() - moved));
				origin.entry().container().setChanged();
			}
		}
	}

	private boolean stillPresent(NearbyCraftingManager.ContainerEntry entry) {
		return this.level.hasChunkAt(entry.pos())
				&& this.level.getBlockEntity(entry.pos()) == entry.container();
	}

	private int insertIntoOriginalSlot(Origin origin, ItemStack stack) {
		ItemStack existing = origin.entry().container().getItem(origin.slot());
		if (existing.isEmpty()) {
			origin.entry().container().setItem(origin.slot(), stack.copy());
			return stack.getCount();
		}
		if (!ItemStack.isSameItemSameComponents(existing, stack)) {
			return 0;
		}

		int space = existing.getMaxStackSize() - existing.getCount();
		int moved = Math.min(space, stack.getCount());
		if (moved > 0) {
			existing.grow(moved);
			origin.entry().container().setItem(origin.slot(), existing);
		}
		return moved;
	}

	public record Taken(NearbyCraftingManager.ContainerEntry entry, int slot, ItemStack stack) {
	}

	private record Origin(
			Slot gridSlot,
			NearbyCraftingManager.ContainerEntry entry,
			int slot,
			ItemStack sample,
			int count
	) {
		private Origin(
				Slot gridSlot,
				NearbyCraftingManager.ContainerEntry entry,
				int slot,
				ItemStack stack
		) {
			this(gridSlot, entry, slot, stack.copyWithCount(1), stack.getCount());
		}
	}
}
