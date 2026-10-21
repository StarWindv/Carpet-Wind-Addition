package top.starwindv.cwa.stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import top.starwindv.cwa.CarpetWindAddition;

public final class StackSizeData extends SavedData {
	public static final Codec<StackSizeData> CODEC = Entry.CODEC.listOf()
			.xmap(StackSizeData::new, StackSizeData::entries);

	public static final SavedDataType<StackSizeData> TYPE = new SavedDataType<>(
			CarpetWindAddition.id("stack_sizes"),
			StackSizeData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<String, Integer> sizes;

	public StackSizeData() {
		this(List.of());
	}

	public StackSizeData(List<Entry> entries) {
		this.sizes = new LinkedHashMap<>();
		for (Entry entry : entries) {
			if (entry.size() > 0) {
				this.sizes.put(entry.item(), entry.size());
			}
		}
	}

	public static @Nullable StackSizeData get(MinecraftServer server) {
		return server.overworld().getDataStorage().get(TYPE);
	}

	public static StackSizeData getOrCreate(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public Map<String, Integer> sizes() {
		return Collections.unmodifiableMap(sizes);
	}

	public void set(String itemId, int size) {
		if (size <= 0) {
			return;
		}
		sizes.put(itemId, size);
		setDirty();
	}

	private List<Entry> entries() {
		List<Entry> entries = new ArrayList<>(sizes.size());
		sizes.forEach((item, size) -> entries.add(new Entry(item, size)));
		return entries;
	}

	public record Entry(String item, int size) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("item").forGetter(Entry::item),
				Codec.INT.fieldOf("size").forGetter(Entry::size)
		).apply(instance, Entry::new));
	}
}
