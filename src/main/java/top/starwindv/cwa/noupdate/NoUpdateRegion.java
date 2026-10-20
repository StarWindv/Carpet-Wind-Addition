package top.starwindv.cwa.noupdate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record NoUpdateRegion(int id, String alias, boolean enabled, BlockPos start, BlockPos end) {
	public static final Codec<NoUpdateRegion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("id").forGetter(NoUpdateRegion::id),
			Codec.STRING.optionalFieldOf("alias", "").forGetter(NoUpdateRegion::alias),
			Codec.BOOL.fieldOf("enabled").forGetter(NoUpdateRegion::enabled),
			BlockPos.CODEC.fieldOf("start").forGetter(NoUpdateRegion::start),
			BlockPos.CODEC.fieldOf("end").forGetter(NoUpdateRegion::end)
	).apply(instance, NoUpdateRegion::new));

	public NoUpdateRegion {
		int minX = Math.min(start.getX(), end.getX());
		int minY = Math.min(start.getY(), end.getY());
		int minZ = Math.min(start.getZ(), end.getZ());
		int maxX = Math.max(start.getX(), end.getX());
		int maxY = Math.max(start.getY(), end.getY());
		int maxZ = Math.max(start.getZ(), end.getZ());
		start = new BlockPos(minX, minY, minZ);
		end = new BlockPos(maxX, maxY, maxZ);
	}

	public boolean contains(BlockPos pos) {
		return pos.getX() >= start.getX() && pos.getX() <= end.getX()
				&& pos.getY() >= start.getY() && pos.getY() <= end.getY()
				&& pos.getZ() >= start.getZ() && pos.getZ() <= end.getZ();
	}

	public NoUpdateRegion withEnabled(boolean enabled) {
		return new NoUpdateRegion(id, alias, enabled, start, end);
	}

	public String displayAlias() {
		return alias.isEmpty() ? "-" : alias;
	}

	public String startString() {
		return start.getX() + " " + start.getY() + " " + start.getZ();
	}

	public String endString() {
		return end.getX() + " " + end.getY() + " " + end.getZ();
	}
}
