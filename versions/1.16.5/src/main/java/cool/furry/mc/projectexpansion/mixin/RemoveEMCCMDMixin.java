package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.Lang;
import moze_intel.projecte.network.commands.RemoveEmcCMD;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RemoveEmcCMD.class)
public class RemoveEMCCMDMixin {
    @ModifyArg(
        method = "removeEmc(Lcom/mojang/brigadier/context/CommandContext;Lmoze_intel/projecte/network/commands/parser/NSSItemParser$NSSItemResult;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;sendSuccess(Lnet/minecraft/util/text/ITextComponent;Z)V", ordinal = 1, remap = true),
        index = 0,
        remap = false
    )
    private static ITextComponent pex$removeEmc(ITextComponent original) {
        if (!Config.server.enableReloadEMCCommand.get()) return original;
        return Lang.Commands.RELOAD_NOTICE.translate(new StringTextComponent("/px reloademc").withStyle(Style.EMPTY.withColor(TextFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/px reloademc"))));
    }
}
