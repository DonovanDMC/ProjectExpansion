package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// This mixin enables the Philosophers Stone to toggle NBT filters
@Mixin(value = PhilosophersStone.class, remap = false)
public class PhilosophersStoneMixin {
    @Inject(at = @At("HEAD"), method = "onItemUseFirst(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", cancellable = true)
    public void onItemUseFirst(ItemStack stack, UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if(!level.isClientSide && player != null) {
            BlockHitResult rtr = this.getHitBlock(ctx.getLevel(), player, ctx.isSecondaryUseActive());
            if (!rtr.getBlockPos().equals(pos)) pos = rtr.getBlockPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if(blockEntity instanceof BlockEntityNBTFilterable be) {
                be.toggleFilter(player);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    @Shadow
    public BlockHitResult getHitBlock(Level level, Player player, boolean isSneaking) {
        throw new IllegalStateException("Mixin failed to shadow getHitBlock()");
    }
}
