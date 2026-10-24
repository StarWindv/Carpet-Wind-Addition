package top.starwindv.cwa.paster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public record PasterDiscardRule(String containerId, String itemId) {
	public static final Codec<PasterDiscardRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("container").forGetter(PasterDiscardRule::containerId),
			Codec.STRING.fieldOf("item").forGetter(PasterDiscardRule::itemId)
	).apply(instance, PasterDiscardRule::new));

	public boolean matchesItem(ItemStack stack) {
		return !stack.isEmpty() && this.itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
	}

	public boolean matchesContainer(String blockEntityId, String blockId) {
		return this.containerId.equals(blockEntityId) || this.containerId.equals(blockId);
	}
}
