package top.starwindv.cwa.allow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import top.starwindv.cwa.CarpetWindAddition;

public final class AllowData extends SavedData {
	public static final Codec<AllowData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.listOf().optionalFieldOf("commands", List.of()).forGetter(AllowData::commandList)
	).apply(instance, AllowData::new));

	public static final SavedDataType<AllowData> TYPE = new SavedDataType<>(
			CarpetWindAddition.id("allowed_commands"),
			AllowData::new,
			CODEC,
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
		return server.getDataStorage().get(TYPE);
	}

	public static AllowData getOrCreate(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
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
