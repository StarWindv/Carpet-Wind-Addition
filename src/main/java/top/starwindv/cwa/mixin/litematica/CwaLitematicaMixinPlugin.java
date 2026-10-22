package top.starwindv.cwa.mixin.litematica;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public final class CwaLitematicaMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		// Required by Mixin 0.17.3 (Fabric Loader 0.19.3), which is used by
		// some production servers. Newer Mixin versions provide a default.
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
				&& FabricLoader.getInstance().isModLoaded("litematica");
	}
}
