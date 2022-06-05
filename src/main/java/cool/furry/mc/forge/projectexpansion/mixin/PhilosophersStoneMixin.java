package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhilosophersStone.class)
public class PhilosophersStoneMixin {

    @Inject(at = @At("HEAD"), method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", cancellable = true)
    public void useOn(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if(!level.isClientSide && player != null) {
            BlockHitResult rtr = ((PhilosophersStone)(Object)this).getHitBlock(player);
            if (!rtr.getBlockPos().equals(pos)) pos = rtr.getBlockPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if(blockEntity instanceof BlockEntityNBTFilterable be) {
                be.toggleFilter(player);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}
