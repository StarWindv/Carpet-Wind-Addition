package top.starwindv.cwa.crafting;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

public final class NearbyCraftingClient {
	private static List<ItemStack> stacks = List.of();
	private static boolean dirty;

	private NearbyCraftingClient() {
	}

	public static void accept(List<ItemStack> received) {
		stacks = received.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
		dirty = true;
	}

	public static void account(Consumer<ItemStack> consumer) {
		for (ItemStack stack : stacks) {
			consumer.accept(stack);
		}
	}

	public static boolean consumeDirty() {
		if (!dirty) {
			return false;
		}
		dirty = false;
		return true;
	}

	public static void clear() {
		stacks = List.of();
		dirty = true;
	}
}
