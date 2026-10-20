package top.starwindv.cwa.util;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ItemReceiver {
	ItemStack accept(ItemStack stack);
}
