package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.block.BlockMatter;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ToolHelper.class)
public class ToolHelperMixin {
    @Inject(at = @At("HEAD"), method = "canMatterMine(Lmoze_intel/projecte/gameObjs/EnumMatterType;Lnet/minecraft/world/level/block/Block;)Z", cancellable = true, remap = false)
    private static void canMatterMine(EnumMatterType matterType, Block block, CallbackInfoReturnable<Boolean> cir) {
        if (block instanceof BlockMatter && matterType.equals(EnumMatterType.RED_MATTER)) {
            cir.setReturnValue(true);
        }
    }
}
