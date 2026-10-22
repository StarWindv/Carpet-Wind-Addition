package top.starwindv.cwa.mixin;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.starwindv.cwa.rule.CwaSettings;
import top.starwindv.cwa.tnt.HotTNTHelper;

@Mixin(BlockBehaviour.class)
public abstract class HotTNTMixin {
	@Redirect(
			method = "onExplosionHit(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"
			)
	)
	private static List<ItemStack> cwa$smeltTntDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> drops = state.getDrops(builder);
		if (!CwaSettings.HotTNT) {
			return drops;
		}

		Entity source = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (source == null
				|| (source.getType() != EntityType.TNT && source.getType() != EntityType.TNT_MINECART)) {
			return drops;
		}

		return HotTNTHelper.smeltDrops(state, drops, builder.getLevel());
	}
}
