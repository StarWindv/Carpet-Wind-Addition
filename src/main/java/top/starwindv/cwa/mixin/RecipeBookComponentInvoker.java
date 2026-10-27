package top.starwindv.cwa.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentInvoker {
	@Invoker("updateStackedContents")
	void cwa$updateStackedContents();
}
