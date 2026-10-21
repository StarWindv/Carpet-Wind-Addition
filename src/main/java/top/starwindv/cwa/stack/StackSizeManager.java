package top.starwindv.cwa.stack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
		if (size >= 1 && size <= 64) {
			SIZES.put(item, size);
		}
	}

	public static void replace(Map<String, Integer> sizes) {
		SIZES.clear();
		sizes.forEach((id, size) -> {
			Identifier itemId = Identifier.tryParse(id);
			if (itemId == null) {
				return;
			}
			BuiltInRegistries.ITEM.getOptional(itemId).ifPresent(item -> set(item, size));
		});
	}

	public static Map<String, Integer> snapshot() {
		Map<String, Integer> snapshot = new LinkedHashMap<>();
		SIZES.forEach((item, size) -> {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
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
