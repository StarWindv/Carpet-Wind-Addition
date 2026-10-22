package top.starwindv.cwa.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemStackTemplate.class)
public abstract class ItemStackTemplateCodecLimitMixin {
	@Redirect(
			method = "lambda$static$0",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"
			)
	)
	private static Codec<Integer> cwa$raiseCountRange(int min, int max) {
		// ItemStackTemplate is used by shulker boxes and other container
		// components. Its count field has a separate vanilla 1..99 codec.
		return ExtraCodecs.intRange(min, Integer.MAX_VALUE);
	}
}
