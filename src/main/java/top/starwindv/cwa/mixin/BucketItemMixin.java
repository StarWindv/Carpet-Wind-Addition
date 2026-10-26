package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
public abstract class BucketItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void cwa$obsidianToLava(
			Level level,
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!CwaSettings.BucketTansOsidianToLava) {
			return;
		}

		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.BUCKET)) {
			return;
		}

		if (!(player.pick(5.0D, 0.0F, false) instanceof BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockPos pos = hit.getBlockPos();
		if (!level.getBlockState(pos).is(Blocks.OBSIDIAN) || !level.mayInteract(player, pos)) {
			return;
		}

		if (!level.isClientSide()) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

			ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);
			if (player.hasInfiniteMaterials() || stack.getCount() <= 1) {
				player.setItemInHand(hand, lavaBucket);
			} else {
				stack.shrink(1);
				if (!player.getInventory().add(lavaBucket)) {
					player.drop(lavaBucket, false, Prediction.SERVER_ONLY);
				}
			}

			level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
