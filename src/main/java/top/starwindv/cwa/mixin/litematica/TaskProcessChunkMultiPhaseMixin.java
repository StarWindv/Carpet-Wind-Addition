package top.starwindv.cwa.mixin.litematica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.starwindv.cwa.paster.PasterClient;

/**
 * Litematica's command based paste task normally sends /setblock and similar
 * commands through the vanilla chat command packet. During a CWA remote paste
 * we forward those commands through CWA instead.
 *
 * <p>The one-argument sendCommand is the entry point used by the task. We
 * increment the shadowed command counter ourselves because the original
 * method would otherwise be cancelled before it increments that counter.</p>
 */
@Mixin(targets = "fi.dy.masa.litematica.scheduler.tasks.TaskProcessChunkMultiPhase")
public abstract class TaskProcessChunkMultiPhaseMixin {
	@Shadow
	protected int sentCommandsThisTick;

	@Inject(method = "sendCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
	private void cwa$routePasterCommand(String command, CallbackInfo ci) {
		if (PasterClient.interceptCommand(command)) {
			this.sentCommandsThisTick++;
			ci.cancel();
		}
	}
}
