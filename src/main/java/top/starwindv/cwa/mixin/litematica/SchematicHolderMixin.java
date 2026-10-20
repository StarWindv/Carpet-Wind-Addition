package top.starwindv.cwa.mixin.litematica;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.starwindv.cwa.paster.PasterProtection;

@Mixin(targets = "fi.dy.masa.litematica.data.SchematicHolder")
public abstract class SchematicHolderMixin {
	@Redirect(
			method = "removeSchematic",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/List;remove(Ljava/lang/Object;)Z"
			),
			require = 0
	)
	private boolean cwa$preventProtectedSchematicUnload(List<?> schematics, Object schematic) {
		if (PasterProtection.shouldBlockSchematicUnload(schematic)) {
			PasterProtection.notifyUnloadBlocked();
			return false;
		}
		return schematics.remove(schematic);
	}
}
