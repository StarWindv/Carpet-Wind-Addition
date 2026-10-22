package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(FlowingFluid.class)
public abstract class WaterproofBlocksMixin {
	@Inject(method = "canHoldFluid", at = @At("HEAD"), cancellable = true)
	private void cwa$waterproofBlocks(
			BlockGetter level,
			BlockPos pos,
			BlockState state,
			Fluid fluid,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (cwa$shouldBlock(state, fluid)) {
			cir.setReturnValue(false);
		}
	}

	private static boolean cwa$shouldBlock(BlockState state, Fluid fluid) {
		if (!fluid.is(FluidTags.WATER)) {
			return false;
		}
		if (CwaSettings.WaterproofRedstoneComponents && cwa$isRedstoneComponent(state)) {
			return true;
		}
		return CwaSettings.WaterproofCarpet
				&& (state.getBlock() instanceof CarpetBlock || state.is(BlockTags.WOOL_CARPETS));
	}

	private static boolean cwa$isRedstoneComponent(BlockState state) {
		var block = state.getBlock();
		return block instanceof LeverBlock
				|| block instanceof ButtonBlock
				|| block instanceof TripWireHookBlock
				|| block instanceof RedstoneTorchBlock
				|| block instanceof RedStoneWireBlock
				|| block instanceof DiodeBlock;
	}
}
