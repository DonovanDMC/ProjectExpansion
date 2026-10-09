package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// This mixin enables the Philosophers Stone to toggle NBT filters
@Mixin(PhilosophersStone.class)
public class PhilosophersStoneMixin {
    @Inject(at = @At("HEAD"), method = "useOn(Lnet/minecraft/item/ItemUseContext;)Lnet/minecraft/util/ActionResultType;", cancellable = true)
    public void useOn(ItemUseContext ctx, CallbackInfoReturnable<ActionResultType> cir) {
        BlockPos pos = ctx.getClickedPos();
        PlayerEntity player = ctx.getPlayer();
        World level = ctx.getLevel();
        if(!level.isClientSide && player != null) {
            BlockRayTraceResult rtr = this.getHitBlock(player);
            if (!rtr.getBlockPos().equals(pos)) pos = rtr.getBlockPos();
            TileEntity blockEntity = level.getBlockEntity(pos);
            if(blockEntity instanceof BlockEntityNBTFilterable) {
                BlockEntityNBTFilterable be = ((BlockEntityNBTFilterable) blockEntity);
                be.toggleFilter(player);
                cir.setReturnValue(ActionResultType.SUCCESS);
            }
        }
    }

    @Shadow(remap = false)
    public BlockRayTraceResult getHitBlock(PlayerEntity player) {
        throw new IllegalStateException("Mixin failed to shadow getHitBlock()");
    }
}
