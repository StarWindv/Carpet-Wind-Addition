package top.starwindv.cwa.util;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

public final class SavedDataIO {
	private SavedDataIO() {
	}

	public static <T> CompoundTag save(Codec<T> codec, T value, HolderLookup.Provider registries) {
		Tag encoded = codec.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), value)
				.result()
				.orElseGet(CompoundTag::new);
		CompoundTag wrapper = new CompoundTag();
		wrapper.put("data", encoded);
		return wrapper;
	}

	public static <T> T load(
			Codec<T> codec,
			CompoundTag tag,
			HolderLookup.Provider registries,
			Supplier<T> fallback
	) {
		Tag input = tag.contains("data") ? tag.get("data") : tag;
		return codec.parse(RegistryOps.create(NbtOps.INSTANCE, registries), input)
				.result()
				.orElseGet(fallback);
	}
}
