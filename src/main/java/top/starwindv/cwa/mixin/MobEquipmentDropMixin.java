package top.starwindv.cwa.mixin;

import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(Mob.class)
public abstract class MobEquipmentDropMixin {
	@Redirect(
			method = "dropCustomDeathLoot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/DropChances;byEquipment(Lnet/minecraft/world/entity/EquipmentSlot;)F"
			)
	)
	private float cwa$disableNaturalEquipmentDrop(DropChances chances, EquipmentSlot slot) {
		if (CwaSettings.MobsNoEquipmentDrop && !chances.isPreserved(slot)) {
			return 0.0F;
		}
		return chances.byEquipment(slot);
	}
}
