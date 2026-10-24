package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(BucketItem.class)
public abstract class QuickPlaceObsidianMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void cwa$quickPlaceObsidian(
			Level level,
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!CwaSettings.QuickPlaceObsidian || hand != InteractionHand.MAIN_HAND) {
			return;
		}

		ItemStack lavaBucket = player.getItemInHand(hand);
		if (!lavaBucket.is(Items.LAVA_BUCKET)
				|| !player.getOffhandItem().is(Items.WATER_BUCKET)) {
			return;
		}

		if (!(player.pick(5.0D, 0.0F, false) instanceof BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockPlaceContext placeContext = new BlockPlaceContext(new UseOnContext(player, hand, hit));
		if (!placeContext.canPlace()) {
			return;
		}
		BlockPos pos = placeContext.getClickedPos();

		if (!level.isClientSide()) {
			level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
			level.playSound(
					null,
					pos,
					Blocks.OBSIDIAN.defaultBlockState().getSoundType().getPlaceSound(),
					SoundSource.BLOCKS,
					1.0F,
					1.0F
			);

			if (!player.hasInfiniteMaterials()) {
				// Consume exactly one lava bucket, whether or not the stack is
				// custom-stacked. The empty bucket goes back to the inventory and
				// is dropped only when there is no room left.
				lavaBucket.shrink(1);
				ItemStack emptyBucket = new ItemStack(Items.BUCKET);
				if (!player.getInventory().add(emptyBucket)) {
					player.drop(emptyBucket, false);
				}
			}
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
