package top.starwindv.cwa.paster;

import net.minecraft.core.BlockPos;

public record PasterBounds(BlockPos start, BlockPos end) {
	public PasterBounds {
		start = new BlockPos(
				Math.min(start.getX(), end.getX()),
				Math.min(start.getY(), end.getY()),
				Math.min(start.getZ(), end.getZ())
		);
		end = new BlockPos(
				Math.max(start.getX(), end.getX()),
				Math.max(start.getY(), end.getY()),
				Math.max(start.getZ(), end.getZ())
		);
	}
}
