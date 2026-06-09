package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import moze_intel.projecte.network.commands.SetEmcCMD;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

@Mixin(SetEmcCMD.class)
public class SetEmcCMDMixin {
    @ModifyArg(
        method = "setEmc(Lcom/mojang/brigadier/context/CommandContext;Lmoze_intel/projecte/api/nss/NSSItem;J)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V", ordinal = 1),
        index = 0,
        remap = false
    )
    private static Supplier<Component> pex$setEmc(Supplier<Component> original) {
        if (!Config.server.enableReloadEMCCommand.get()) return original;
        return () -> Lang.Commands.RELOAD_NOTICE.translate(Component.literal("/px reloademc").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/px reloademc"))));
    }
}
