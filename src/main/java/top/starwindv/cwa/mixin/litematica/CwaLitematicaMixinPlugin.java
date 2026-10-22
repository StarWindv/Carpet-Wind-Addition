package top.starwindv.cwa.mixin.litematica;

import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CwaLitematicaMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		// Required by Mixin 0.17.2/0.17.3, which are used by some production
		// servers and launchers. Newer Mixin versions provide defaults.
	}

	@Override
	public String getRefMapperConfig() {
		// Older Mixin versions require this method even when no refmap is used.
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
				&& FabricLoader.getInstance().isModLoaded("litematica");
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(
			String targetClassName,
			ClassNode targetClass,
			String mixinClassName,
			IMixinInfo mixinInfo
	) {
	}

	@Override
	public void postApply(
			String targetClassName,
			ClassNode targetClass,
			String mixinClassName,
			IMixinInfo mixinInfo
	) {
	}
}
