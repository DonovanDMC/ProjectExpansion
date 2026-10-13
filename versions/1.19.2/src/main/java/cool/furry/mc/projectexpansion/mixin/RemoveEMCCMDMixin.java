package cool.furry.mc.projectexpansion.mixin;

import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.projectexpansion.commands.CommandReloadEMC;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.Lang;
import moze_intel.projecte.network.commands.RemoveEmcCMD;
import moze_intel.projecte.network.commands.parser.NSSItemParser;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RemoveEmcCMD.class)
public class RemoveEMCCMDMixin {
    @ModifyArg(
        method = "removeEmc(Lcom/mojang/brigadier/context/CommandContext;Lmoze_intel/projecte/network/commands/parser/NSSItemParser$NSSItemResult;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Lnet/minecraft/network/chat/Component;Z)V", ordinal = 1, remap = true),
        index = 0,
        remap = false
    )
    private static Component pex$removeEmc(Component original) {
        if (CommandReloadEMC.reloadsAfterCommands()) return Lang.Commands.RELOADING_EMC.translate();
        if (!Config.server.enableReloadEMCCommand.get()) return original;
        return Lang.Commands.RELOAD_NOTICE.translate(Component.literal("/px reloademc").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/px reloademc"))));
    }

    @Inject(method = "removeEmc(Lcom/mojang/brigadier/context/CommandContext;Lmoze_intel/projecte/network/commands/parser/NSSItemParser$NSSItemResult;)I", at = @At("RETURN"), remap = false)
    private static void pex$removeEmcReload(CommandContext<CommandSourceStack> ctx, NSSItemParser.NSSItemResult stack, CallbackInfoReturnable<Integer> cir) {
        CommandReloadEMC.reloadAfterCommand(ctx.getSource(), cir.getReturnValue());
    }
}
