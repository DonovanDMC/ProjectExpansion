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
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.math.BigInteger;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EMCDisplay {
    private static BigInteger change = BigInteger.ZERO;
    private static BigInteger emc = BigInteger.ZERO;
    private static int tick = 0;

    private static @Nullable LocalPlayer getPlayer() {
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
                BigInteger lastEMC = emc;
                emc = provider.getEmc();
                change = emc.subtract(lastEMC);
            });
        }
    }

    private static void reset() {
        if (!Config.emcDisplay.get())
            return;
        emc = change = BigInteger.ZERO;
        tick = 0;
    }

    @SubscribeEvent
    public static void clientDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        reset();
    }

    @SubscribeEvent
    public static void worldUnload(WorldEvent.Unload event) {
        reset();
    }

    @SubscribeEvent
    public static void onRenderGUI(RenderGameOverlayEvent.Text event) {
        if (!Config.emcDisplay.get())
            return;
        String str = EMCFormat.INSTANCE.format(emc.doubleValue());
        if (!change.equals(BigInteger.ZERO))
            str += " " + (change.compareTo(BigInteger.ZERO) > 0 ? (ChatFormatting.GREEN + "+") : (ChatFormatting.RED + "-")) + EMCFormat.INSTANCE.format(Math.abs(change.doubleValue())) + "/s";
        event.getLeft().add(String.format("EMC: %s", str));
    }
}
