package cool.furry.mc.forge.projectexpansion.mixin;

import moze_intel.projecte.PECore;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// shuts up ProjectE's "** RECEIVED TRANSMUTATION EMC DATA CLIENTSIDE **" spam
@Mixin(PECore.class)
public class ShutTheFuckUpMixin {
    @Inject(at = @At("HEAD"), method = "debugLog(Ljava/lang/String;[Ljava/lang/Object;)V", cancellable = true, remap = false)
    private static void debugLog(String msg, Object[] args, CallbackInfo ci) {
        if(FMLEnvironment.production) return;
        if(msg.equals("** RECEIVED TRANSMUTATION EMC DATA CLIENTSIDE **")) {
            ci.cancel();
        }
    }
}
