package top.starwindv.cwa.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(ServerLevel.class)
public abstract class MobsNoSpearMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void cwa$removeSpearsFromSpawnedMobs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!CwaSettings.MobsNoSpear || !(entity instanceof Mob mob)) {
			return;
		}

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (mob.getItemBySlot(slot).is(ItemTags.SPEARS)) {
				mob.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}
}
