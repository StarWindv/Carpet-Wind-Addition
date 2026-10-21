package top.starwindv.cwa.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientSuggestionProvider.class)
public abstract class ClientSuggestionProviderMixin {
	@Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true)
	private void cwa$allowEntitySelectors(int permissionLevel, CallbackInfoReturnable<Boolean> cir) {
		if (permissionLevel <= 2) {
			cir.setReturnValue(true);
		}
	}
}
