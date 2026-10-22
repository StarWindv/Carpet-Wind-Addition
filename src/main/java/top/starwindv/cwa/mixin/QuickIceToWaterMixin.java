package top.starwindv.cwa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.rule.CwaSettings;

@Mixin(BlockItem.class)
public abstract class QuickIceToWaterMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void cwa$quickIceToWater(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!CwaSettings.QuickIceToWater) {
			return;
		}

		ItemStack ice = context.getItemInHand();
		if (!ice.is(Items.ICE)) {
			return;
		}

		Player player = context.getPlayer();
		if (player == null || !context.canPlace()) {
			return;
		}

		InteractionHand iceHand = context.getHand();
		InteractionHand pickaxeHand = iceHand == InteractionHand.MAIN_HAND
				? InteractionHand.OFF_HAND
				: InteractionHand.MAIN_HAND;
		ItemStack pickaxe = player.getItemInHand(pickaxeHand);
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return;
		}

		if (CwaSettings.QuickITWCheck && !pickaxe.getHoverName().getString().contains("_water")) {
			return;
		}

		// Silk Touch means the pickaxe should not turn the ice into water.
		boolean silkTouch = pickaxe.getEnchantments().keySet().stream()
				.anyMatch(enchantment -> enchantment.is(Enchantments.SILK_TOUCH));
		if (silkTouch) {
			return;
		}

		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if (!level.isClientSide()) {
			// The clicked position may already contain water (for example after
			// an earlier use). It is still a valid conversion, so do not fall
			// through to the vanilla ice placement just because the block did
			// not change.
			level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());

			ice.consume(1, player);
			if (!player.hasInfiniteMaterials()) {
				EquipmentSlot pickaxeSlot = pickaxeHand == InteractionHand.MAIN_HAND
						? EquipmentSlot.MAINHAND
						: EquipmentSlot.OFFHAND;
				pickaxe.hurtAndBreak(1, player, pickaxeSlot);
			}
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
