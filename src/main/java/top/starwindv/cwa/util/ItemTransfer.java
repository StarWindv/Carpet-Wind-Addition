package top.starwindv.cwa.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ItemTransfer {
	private ItemTransfer() {
	}

	public static ItemStack insertIntoInventory(Inventory inventory, ItemStack stack) {
		ItemStack remaining = stack.copy();
		boolean changed = false;

		for (int pass = 0; pass < 2; pass++) {
			int slots = pass == 0 ? Inventory.INVENTORY_SIZE : 1;
			for (int index = 0; index < slots && !remaining.isEmpty(); index++) {
				int slot = pass == 0 ? index : Inventory.SLOT_OFFHAND;
				ItemStack existing = inventory.getItem(slot);
				if (existing.isEmpty()) {
					int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
					inventory.setItem(slot, remaining.split(moved));
					changed = true;
				} else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
					int space = existing.getMaxStackSize() - existing.getCount();
					if (space > 0) {
						int moved = Math.min(space, remaining.getCount());
						existing.grow(moved);
						remaining.shrink(moved);
						inventory.setItem(slot, existing);
						changed = true;
					}
				}
			}
		}

		if (changed) {
			inventory.setChanged();
		}
		return remaining;
	}

	public static List<ContainerEntry> findContainerEntries(
			ServerLevel level,
			BlockPos first,
			BlockPos second,
			Predicate<Container> filter
	) {
		List<ContainerEntry> entries = new ArrayList<>();
		Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());

		for (BlockPos pos : BlockPos.betweenClosed(first, second)) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof Container container
					&& visited.add(container)
					&& filter.test(container)) {
				entries.add(new ContainerEntry(pos.immutable(), container));
			}
		}
		return entries;
	}

	public static List<Container> findContainers(
			ServerLevel level,
			BlockPos first,
			BlockPos second,
			Predicate<Container> filter
	) {
		return findContainerEntries(level, first, second, filter).stream()
				.map(ContainerEntry::container)
				.toList();
	}

	public static ItemStack insertIntoContainers(List<Container> containers, ItemStack stack) {
		ItemStack remaining = stack.copy();

		for (Container container : containers) {
			if (remaining.isEmpty()) {
				break;
			}
			remaining = insertIntoContainer(container, remaining);
		}
		return remaining;
	}

	public static ItemStack insertIntoContainer(Container container, ItemStack stack) {
		ItemStack remaining = stack.copy();
		boolean changed = false;

		for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
			if (!container.canPlaceItem(slot, remaining)) {
				continue;
			}

			ItemStack existing = container.getItem(slot);
			if (existing.isEmpty()) {
				int moved = Math.min(remaining.getCount(), container.getMaxStackSize(remaining));
				container.setItem(slot, remaining.split(moved));
				changed = true;
			} else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
				int space = container.getMaxStackSize(remaining) - existing.getCount();
				if (space > 0) {
					int moved = Math.min(space, remaining.getCount());
					existing.grow(moved);
					remaining.shrink(moved);
					container.setItem(slot, existing);
					changed = true;
				}
			}
		}

		if (changed) {
			container.setChanged();
		}
		return remaining;
	}

	public record ContainerEntry(BlockPos pos, Container container) {
	}
}
