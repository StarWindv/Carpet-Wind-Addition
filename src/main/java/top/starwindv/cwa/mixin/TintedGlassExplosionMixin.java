package top.starwindv.cwa.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(Block.class)
public abstract class TintedGlassExplosionMixin {
	@Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
	private void cwa$useObsidianResistance(CallbackInfoReturnable<Float> cir) {
		if (CwaSettings.ExplosionProofTintedGlass && (Object) this == Blocks.TINTED_GLASS) {
			cir.setReturnValue(Blocks.OBSIDIAN.getExplosionResistance());
		}
	}
}
