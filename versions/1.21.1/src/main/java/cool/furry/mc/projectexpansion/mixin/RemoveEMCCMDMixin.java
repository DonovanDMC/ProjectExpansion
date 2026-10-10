package cool.furry.mc.projectexpansion.mixin;

import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.projectexpansion.commands.CommandReloadEMC;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.Lang;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.network.commands.RemoveEmcCMD;
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

import java.util.function.Supplier;

@Mixin(RemoveEmcCMD.class)
public class RemoveEMCCMDMixin {
    @ModifyArg(
        method = "removeEmc(Lcom/mojang/brigadier/context/CommandContext;Lmoze_intel/projecte/api/nss/NSSItem;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V", ordinal = 1),
        index = 0,
        remap = false
    )
    private static Supplier<Component> pex$removeEmc(Supplier<Component> original) {
        if (Config.server.reloadEMCAfterCommands.get()) return () -> Lang.Commands.RELOADING_EMC.translate();
        if (!Config.server.enableReloadEMCCommand.get()) return original;
        return () -> Lang.Commands.RELOAD_NOTICE.translate(Component.literal("/px reloademc").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/px reloademc"))));
    }

    @Inject(method = "removeEmc(Lcom/mojang/brigadier/context/CommandContext;Lmoze_intel/projecte/api/nss/NSSItem;)I", at = @At("RETURN"), remap = false)
    private static void pex$removeEmcReload(CommandContext<CommandSourceStack> ctx, NSSItem stack, CallbackInfoReturnable<Integer> cir) {
        CommandReloadEMC.reloadAfterCommand(ctx.getSource(), cir.getReturnValue());
    }
}
