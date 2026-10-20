package top.starwindv.cwa.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientSuggestionProvider.class)
public abstract class ClientSuggestionProviderMixin {
	@Inject(method = "permissions", at = @At("RETURN"), cancellable = true)
	private void cwa$allowEntitySelectors(CallbackInfoReturnable<PermissionSet> cir) {
		PermissionSet permissions = cir.getReturnValue();

		if (permissions == null
				|| permissions == PermissionSet.NO_PERMISSIONS
				|| permissions.hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)) {
			return;
		}

		cir.setReturnValue(permission -> Permissions.COMMANDS_ENTITY_SELECTORS.equals(permission)
				|| permissions.hasPermission(permission));
	}
}
