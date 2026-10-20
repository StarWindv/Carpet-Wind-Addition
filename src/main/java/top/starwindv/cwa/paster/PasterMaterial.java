package top.starwindv.cwa.paster;

import net.minecraft.world.item.ItemStack;

public record PasterMaterial(ItemStack sample, int count) {
	public PasterMaterial {
		sample = sample.copyWithCount(1);
		if (count < 0) {
			throw new IllegalArgumentException("Material count cannot be negative");
		}
	}
}
