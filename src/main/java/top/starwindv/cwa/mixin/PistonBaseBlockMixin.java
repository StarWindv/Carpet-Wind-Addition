package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
	@Redirect(
			method = "moveBlocks",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
			)
	)
	private static void cwa$dropAmethystCluster(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity) {
		if (CwaSettings.PistonHarvestAmethyst && state.is(Blocks.AMETHYST_CLUSTER) && level instanceof Level actualLevel) {
			Block.popResource(actualLevel, pos, new ItemStack(Blocks.AMETHYST_CLUSTER));
		} else {
			Block.dropResources(state, level, pos, blockEntity);
		}
	}
}
