package top.starwindv.cwa.mixin.litematica;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public final class CwaLitematicaMixinPlugin implements IMixinConfigPlugin {
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
				&& FabricLoader.getInstance().isModLoaded("litematica");
	}
}
