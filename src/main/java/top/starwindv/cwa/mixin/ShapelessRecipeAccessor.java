package top.starwindv.cwa.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShapelessRecipe.class)
public interface ShapelessRecipeAccessor {
	@Accessor("result")
	ItemStack cwa$getResult();

	@Accessor("ingredients")
	NonNullList<Ingredient> cwa$getIngredients();
}
