package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// This mixin enables shift clicking on an Advanced Alchemical Chest to set the color
@Mixin(AlchemicalBag.class)
public class AlchemicalBagMixin {
	@Inject(at = @At("HEAD"), method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", cancellable = true)
	public void use(World level, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
		if(!level.isClientSide) {
			@Nullable TileEntity blockEntity = getLookingAt(player);
			if(blockEntity instanceof BlockEntityAdvancedAlchemicalChest) {
			    BlockEntityAdvancedAlchemicalChest be = ((BlockEntityAdvancedAlchemicalChest) blockEntity);
				ItemStack stack = player.getItemInHand(hand);
				be.handleItemActivation(player, stack);
				cir.setReturnValue(ActionResult.success(player.getItemInHand(hand)));
			}
		}
	}

	@Unique
	private @Nullable TileEntity getLookingAt(PlayerEntity player) {
		RayTraceResult hr = player.pick(20.0D, 0.0F, false);
		if(hr instanceof BlockRayTraceResult) {
		    BlockRayTraceResult bhr = ((BlockRayTraceResult) hr);
			BlockPos pos = bhr.getBlockPos();
			return player.level.getBlockEntity(pos);
		}

		return null;
	}
}
