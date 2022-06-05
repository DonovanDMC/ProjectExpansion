package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.tile.TileNBTFilterable;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhilosophersStone.class)
public class PhilosophersStoneMixin {

    @Inject(at = @At("HEAD"), method = "onItemUse(Lnet/minecraft/item/ItemUseContext;)Lnet/minecraft/util/ActionResultType;", cancellable = true)
    public void onItemUse(ItemUseContext ctx, CallbackInfoReturnable<ActionResultType> cir) {
        BlockPos pos = ctx.getPos();
        PlayerEntity player = ctx.getPlayer();
        World world = ctx.getWorld();
        if(!world.isRemote && player != null) {
            RayTraceResult rtr = ((PhilosophersStone)(Object)this).getHitBlock(player);
            if (rtr instanceof BlockRayTraceResult && !((BlockRayTraceResult)rtr).getPos().equals(pos)) pos = ((BlockRayTraceResult)rtr).getPos();
            TileEntity tileEntity = world.getTileEntity(pos);
            if(tileEntity instanceof TileNBTFilterable) {
                ((TileNBTFilterable) tileEntity).toggleFilter(player);
                cir.setReturnValue(ActionResultType.SUCCESS);
            }
        }
    }
}
