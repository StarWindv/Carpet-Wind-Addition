package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
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
			CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
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

		ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);
		ItemStack result = lavaBucket;
		if (!level.isClientSide) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

			if (player.hasInfiniteMaterials() || stack.getCount() <= 1) {
				player.setItemInHand(hand, lavaBucket);
			} else {
				stack.shrink(1);
				if (!player.getInventory().add(lavaBucket)) {
					player.drop(lavaBucket, false);
				}
				// Do not return the lava bucket here: in 1.21.1 the caller
				// replaces the hand stack with the returned stack, which would
				// discard the remaining empty buckets. Returning the same stack
				// keeps the hand stack intact.
				result = stack;
			}

			level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.PLAYERS, 1.0F, 1.0F);
		} else if (!player.hasInfiniteMaterials() && stack.getCount() > 1) {
			result = stack;
		}

		cir.setReturnValue(InteractionResultHolder.success(result));
	}
}
