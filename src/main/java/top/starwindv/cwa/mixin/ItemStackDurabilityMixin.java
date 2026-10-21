package top.starwindv.cwa.mixin;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.starwindv.cwa.stack.StackSizeManager;

@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {
	@Inject(
			method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
			at = @At("HEAD")
	)
	private void cwa$splitDamageableStackBeforeDamage(
			int amount,
			ServerLevel level,
			ServerPlayer player,
			Consumer<Item> onBreak,
			CallbackInfo ci
	) {
		if (amount > 0) {
			StackSizeManager.splitDamageableStack((ItemStack) (Object) this, player);
		}
	}
}
