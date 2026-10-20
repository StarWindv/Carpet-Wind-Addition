package top.starwindv.cwa.paster;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public record PlacementSnapshot(
		UUID id,
		String name,
		String dimension,
		Object placement,
		List<PasterMaterial> requiredMaterials,
		List<PasterBounds> bounds
) {
}
