package top.starwindv.cwa.allow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import top.starwindv.cwa.util.SavedDataIO;

public final class AllowData extends SavedData {
	public static final Codec<AllowData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.listOf().optionalFieldOf("commands", List.of()).forGetter(AllowData::commandList)
	).apply(instance, AllowData::new));

	private static final String DATA_NAME = "cwa_allowed_commands";
	private static final SavedData.Factory<AllowData> FACTORY = new SavedData.Factory<>(
			AllowData::new,
			(tag, registries) -> SavedDataIO.load(CODEC, tag, registries, AllowData::new),
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Set<String> commands;

	public AllowData() {
		this(List.of());
	}

	public AllowData(List<String> commands) {
		this.commands = new LinkedHashSet<>(commands);
	}

	public static @Nullable AllowData get(MinecraftServer server) {
		return server == null ? null : server.overworld().getDataStorage().get(FACTORY, DATA_NAME);
	}

	public static AllowData getOrCreate(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		return SavedDataIO.save(CODEC, this, registries);
	}

	public Set<String> commands() {
		return Collections.unmodifiableSet(commands);
	}

	private List<String> commandList() {
		return List.copyOf(commands);
	}

	public boolean isAllowed(String command) {
		return commands.contains(command);
	}

	public void setAllowed(String command, boolean allowed) {
		boolean changed = allowed ? commands.add(command) : commands.remove(command);
		if (changed) {
			setDirty();
		}
	}
}
