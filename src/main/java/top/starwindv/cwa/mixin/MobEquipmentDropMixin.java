package top.starwindv.cwa.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(Mob.class)
public abstract class MobEquipmentDropMixin {
	@Inject(method = "getEquipmentDropChance", at = @At("RETURN"), cancellable = true)
	private void cwa$disableNaturalEquipmentDrop(EquipmentSlot slot, CallbackInfoReturnable<Float> cir) {
		// Drop chances above 1 are the "preserved"/guaranteed drops used for
		// equipment picked up by a mob. Only suppress ordinary natural drops.
		if (CwaSettings.MobsNoEquipmentDrop && cir.getReturnValueF() <= 1.0F) {
			cir.setReturnValue(0.0F);
		}
	}
}
