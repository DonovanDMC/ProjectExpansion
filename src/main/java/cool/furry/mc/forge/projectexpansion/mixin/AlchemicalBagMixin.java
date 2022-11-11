package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.tile.TileAdvancedAlchemicalChest;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(AlchemicalBag.class)
public class AlchemicalBagMixin {
	@Inject(at = @At("HEAD"), method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", cancellable = true)
	public void ise(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
		if(!world.isClientSide) {
			@Nullable TileEntity blockEntity = getLookingAt(player);
			if(blockEntity instanceof TileAdvancedAlchemicalChest) {
				((TileAdvancedAlchemicalChest) blockEntity).handleActivation(player, hand);
				cir.setReturnValue(ActionResult.success(player.getItemInHand(hand)));
			}
		}
	}

	private @Nullable TileEntity getLookingAt(PlayerEntity player) {
		RayTraceResult hr = player.pick(20.0D, 0.0F, false);
		if(hr instanceof BlockRayTraceResult) {
			BlockPos pos = ((BlockRayTraceResult) hr).getBlockPos();
			return player.level.getBlockEntity(pos);
		}

		return null;
	}
}
