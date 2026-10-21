package top.starwindv.cwa.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemStack.class)
public abstract class ItemStackCodecLimitMixin {
	@Redirect(
			method = "lambda$static$1",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"
			)
	)
	private static Codec<Integer> cwa$raiseCountRange(int min, int max) {
		// The real per-item limit is enforced by getMaxStackSize and the
		// stack-size settings. This only removes the vanilla codec's hard 99.
		return ExtraCodecs.intRange(min, Integer.MAX_VALUE);
	}
}
