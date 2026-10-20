package top.starwindv.cwa.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import top.starwindv.cwa.util.ItemReceiver;

public final class ContainerCleaner {
	private ContainerCleaner() {
	}

	public static boolean isContainerBlock(Block block) {
		BlockState state = block.defaultBlockState();

		for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
			if (!type.isValid(state)) {
				continue;
			}

			try {
				if (type.create(BlockPos.ZERO, state) instanceof Container) {
					return true;
				}
			} catch (RuntimeException ignored) {
				// Some block entity types cannot be created without a fully initialised level.
			}
		}
		return false;
	}

	public static ScanResult findContainers(
			ServerLevel level,
			BlockPos first,
			BlockPos second,
			Block containerBlock
	) {
		List<Container> containers = new ArrayList<>();
		Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		int matched = 0;

		for (BlockPos pos : BlockPos.betweenClosed(first, second)) {
			if (!level.getBlockState(pos).is(containerBlock)) {
				continue;
			}
			if (!(level.getBlockEntity(pos) instanceof Container container)) {
				continue;
			}

			matched++;
			if (visited.add(container)) {
				containers.add(container);
			}
		}
		return new ScanResult(List.copyOf(containers), matched);
	}

	public static Result clean(List<Container> containers, Item item, ItemReceiver receiver) {
		int containersWithItem = 0;
		int found = 0;
		int removed = 0;
		int leftover = 0;
		int leftoverSlots = 0;

		for (Container container : containers) {
			boolean containerHasItem = false;
			boolean changed = false;

			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.isEmpty() || !stack.is(item)) {
					continue;
				}

				containerHasItem = true;
				found += stack.getCount();
				ItemStack leftoverStack = receiver.accept(stack.copy());
				int moved = stack.getCount() - leftoverStack.getCount();

				if (moved > 0) {
					container.setItem(slot, leftoverStack.isEmpty() ? ItemStack.EMPTY : leftoverStack);
					removed += moved;
					changed = true;
				}
				if (!leftoverStack.isEmpty()) {
					leftover += leftoverStack.getCount();
					leftoverSlots++;
				}
			}

			if (containerHasItem) {
				containersWithItem++;
			}
			if (changed) {
				container.setChanged();
			}
		}

		return new Result(containersWithItem, found, removed, leftover, leftoverSlots);
	}

	public record ScanResult(List<Container> containers, int matched) {
	}

	public record Result(int containers, int found, int removed, int leftover, int leftoverSlots) {
	}
}
