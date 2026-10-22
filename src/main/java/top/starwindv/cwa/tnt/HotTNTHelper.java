package top.starwindv.cwa.tnt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;

public final class HotTNTHelper {
	private HotTNTHelper() {
	}

	public static List<ItemStack> smeltDrops(BlockState state, List<ItemStack> drops, ServerLevel level) {
		List<ItemStack> result = new ArrayList<>(drops.size());
		boolean hasDrop = false;
		for (ItemStack drop : drops) {
			if (drop.isEmpty()) {
				continue;
			}
			hasDrop = true;
			ItemStack smelted = smelt(level, drop);
			if (smelted.isEmpty()) {
				result.add(drop);
			} else {
				addSmelted(result, smelted, drop.getCount());
			}
		}

		if (hasDrop) {
			return result;
		}

		// TNT without a tool normally drops nothing from blocks that require a
		// pickaxe. Still allow the block's own smelting recipe to produce the
		// cooked result (for example cobblestone -> stone).
		ItemStack blockItem = new ItemStack(state.getBlock().asItem());
		if (!blockItem.isEmpty()) {
			ItemStack smelted = smelt(level, blockItem);
			if (!smelted.isEmpty()) {
				addSmelted(result, smelted, 1);
			}
		}
		return result;
	}

	private static ItemStack smelt(ServerLevel level, ItemStack input) {
		SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
		Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getServer().getRecipeManager()
				.getRecipeFor(RecipeType.SMELTING, recipeInput, level);
		if (recipe.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return recipe.get().value().assemble(recipeInput, level.registryAccess());
	}

	private static void addSmelted(List<ItemStack> result, ItemStack smelted, int multiplier) {
		int total = smelted.getCount() * multiplier;
		if (total <= 0) {
			return;
		}

		int max = Math.max(1, smelted.getMaxStackSize());
		while (total > 0) {
			int count = Math.min(total, max);
			result.add(smelted.copyWithCount(count));
			total -= count;
		}
	}
}
