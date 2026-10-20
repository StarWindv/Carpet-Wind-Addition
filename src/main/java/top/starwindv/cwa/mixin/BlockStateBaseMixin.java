package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.noupdate.NoUpdateManager;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Inject(method = "handleNeighborChanged", at = @At("HEAD"), cancellable = true)
	private void cwa$suppressNeighborUpdate(Level level, BlockPos pos, Block block, Orientation orientation,
			boolean movedByPiston, CallbackInfo ci) {
		if (NoUpdateManager.shouldSuppress(level, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void cwa$suppressShapeUpdate(LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random,
			CallbackInfoReturnable<BlockState> cir) {
		if (NoUpdateManager.shouldSuppress(level, pos)) {
			cir.setReturnValue((BlockState) (Object) this);
		}
	}
}
