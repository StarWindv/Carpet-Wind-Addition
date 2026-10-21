package top.starwindv.cwa.stack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StackSizeManager {
	private static final Map<Item, Integer> SIZES = new ConcurrentHashMap<>();

	private StackSizeManager() {
	}

	public static int getMaxStackSize(ItemStack stack) {
		if (stack.isEmpty()) {
			return -1;
		}

		Integer size = SIZES.get(stack.getItem());
		return size == null ? -1 : size;
	}

	public static void set(Item item, int size) {
		if (size > 0) {
			SIZES.put(item, size);
		}
	}

	/**
	 * A damageable item stack must not lose durability as a whole stack. Before
	 * durability is applied, keep one item in the used slot and distribute the
	 * rest of the stack normally.
	 */
	public static void splitDamageableStack(ItemStack stack, ServerPlayer player) {
		if (player == null || player.hasInfiniteMaterials()) {
			return;
		}
		if (stack.getCount() <= 1 || !stack.isDamageableItem()) {
			return;
		}

		int remaining = stack.getCount() - 1;
		ItemStack remainder = stack.copyWithCount(remaining);
		stack.setCount(1);

		Inventory inventory = player.getInventory();
		int slots = Math.min(36, inventory.getContainerSize());
		for (int slot = 0; slot < slots && remaining > 0; slot++) {
			ItemStack existing = inventory.getItem(slot);
			if (existing == stack) {
				continue;
			}

			if (existing.isEmpty()) {
				inventory.setItem(slot, remainder.copyWithCount(remaining));
				remaining = 0;
				break;
			}

			if (ItemStack.isSameItemSameComponents(existing, remainder)) {
				int space = existing.getMaxStackSize() - existing.getCount();
				if (space > 0) {
					int moved = Math.min(space, remaining);
					existing.grow(moved);
					remaining -= moved;
				}
			}
		}

		if (remaining > 0) {
			player.drop(remainder.copyWithCount(remaining), false);
		}
	}

	public static void replace(Map<String, Integer> sizes) {
		SIZES.clear();
		sizes.forEach((id, size) -> {
			ResourceLocation itemId = ResourceLocation.tryParse(id);
			if (itemId == null) {
				return;
			}
			BuiltInRegistries.ITEM.getOptional(itemId).ifPresent(item -> set(item, size));
		});
	}

	public static Map<String, Integer> snapshot() {
		Map<String, Integer> snapshot = new LinkedHashMap<>();
		SIZES.forEach((item, size) -> {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			if (id != null) {
				snapshot.put(id.toString(), size);
			}
		});
		return snapshot;
	}

	public static void clear() {
		SIZES.clear();
	}

	public static void onServerLoaded(MinecraftServer server) {
		StackSizeData data = StackSizeData.get(server);
		replace(data == null ? Map.of() : data.sizes());
	}

	public static void broadcast(MinecraftServer server) {
		StackSizesPayload payload = new StackSizesPayload(snapshot());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void sendTo(ServerPlayer player) {
		ServerPlayNetworking.send(player, new StackSizesPayload(snapshot()));
	}
}
