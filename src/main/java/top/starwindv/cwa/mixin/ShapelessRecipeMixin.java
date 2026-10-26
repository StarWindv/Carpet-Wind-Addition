package top.starwindv.cwa.mixin;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
	@Inject(
			method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private void cwa$disableObsidianToLavaRecipe(
			CraftingInput input,
			Level level,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (!CwaSettings.ObsidianToLavaRecipe && cwa$isObsidianToLavaRecipe()) {
			cir.setReturnValue(false);
		}
	}

	private boolean cwa$isObsidianToLavaRecipe() {
		ShapelessRecipeAccessor accessor = (ShapelessRecipeAccessor) (Object) this;
		ItemStack result = accessor.cwa$getResult().create();
		if (!result.is(Items.LAVA_BUCKET)) {
			return false;
		}

		List<Ingredient> ingredients = accessor.cwa$getIngredients();
		if (ingredients.size() != 2) {
			return false;
		}

		boolean bucket = false;
		boolean obsidian = false;
		for (Ingredient ingredient : ingredients) {
			bucket |= ingredient.test(new ItemStack(Items.BUCKET));
			obsidian |= ingredient.test(new ItemStack(Items.OBSIDIAN));
		}
		return bucket && obsidian;
	}
}
