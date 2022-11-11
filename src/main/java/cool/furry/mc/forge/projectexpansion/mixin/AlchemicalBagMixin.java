package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AlchemicalBag.class)
public class AlchemicalBagMixin {
	@Inject(at = @At("HEAD"), method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;", cancellable = true)
	public void use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand, CallbackInfoReturnable<@NotNull InteractionResultHolder<ItemStack>> cir) {
		if(!level.isClientSide) {
			@Nullable BlockEntity blockEntity = getLookingAt(player);
			if(blockEntity instanceof BlockEntityAdvancedAlchemicalChest be) {
				be.handleActivation(player, hand);
				cir.setReturnValue(InteractionResultHolder.success(player.getItemInHand(hand)));
			}
		}
	}

	private @Nullable BlockEntity getLookingAt(Player player) {
		HitResult hr = player.pick(20.0D, 0.0F, false);
		if(hr instanceof BlockHitResult bhr) {
			BlockPos pos = bhr.getBlockPos();
			return player.level.getBlockEntity(pos);
		}

		return null;
	}
}
