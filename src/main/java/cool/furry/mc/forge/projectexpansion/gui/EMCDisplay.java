package cool.furry.mc.forge.projectexpansion.gui;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.math.BigInteger;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EMCDisplay {
    private static BigInteger emc = BigInteger.ZERO;
    private static final BigInteger[] history = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};
    private static BigInteger lastEMC = BigInteger.ZERO;
    private static int tick = 0;

    private static @Nullable
    LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!Config.emcDisplay.get()) return;
        LocalPlayer player = getPlayer();
        tick++;
        if (event.phase == TickEvent.Phase.END && player != null && tick >= 20) {
            tick = 0;
            player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).ifPresent((provider) -> {
                emc = provider.getEmc();
                history[1] = history[0];
                history[0] = emc.subtract(lastEMC);
                lastEMC = emc;
            });
        }
    }

    private static void reset() {
        emc = lastEMC = BigInteger.ZERO;
        tick = 0;
    }

    @SubscribeEvent
    public static void clientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!Config.emcDisplay.get()) return;
        reset();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!Config.emcDisplay.get()) return;
        reset();
    }

    @SubscribeEvent
    public static void onRenderGUI(CustomizeGuiOverlayEvent.DebugText  event) {
        if (!Config.emcDisplay.get()) return;
        BigInteger avg = history[0].add(history[1]);
        String str = EMCFormat.format(emc);
        if (!avg.equals(BigInteger.ZERO)) str += " " + (avg.compareTo(BigInteger.ZERO) > 0 ? (ChatFormatting.GREEN + "+") : (ChatFormatting.RED + "-")) + EMCFormat.format(avg.abs()) + "/s";
        event.getLeft().add(String.format("EMC: %s", str));
    }
}

