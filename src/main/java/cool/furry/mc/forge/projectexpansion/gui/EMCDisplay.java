package cool.furry.mc.forge.projectexpansion.gui;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EMCDisplay {
    private static BigInteger emc = BigInteger.ZERO;
    private static BigInteger lastEMC = BigInteger.ZERO;
    private static BigInteger change = BigInteger.ZERO;
    private static BigInteger[] emcHistory = new BigInteger[5];
    private static int tick = 0;

    private static @Nullable ClientPlayerEntity getPlayer() {
        return Minecraft.getInstance().player;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if(!Config.emcDisplay.get()) return;
        ClientPlayerEntity player = getPlayer();
        tick++;
        if(event.phase == TickEvent.Phase.END && player != null && tick >= 20) {
            tick = 0;
            @Nullable IKnowledgeProvider provider = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).orElse(null);
            emc = provider == null ? BigInteger.ZERO : provider.getEmc();
            System.arraycopy(emcHistory, 1, emcHistory, 0, emcHistory.length - 1);
            emcHistory[emcHistory.length - 1] = emc.subtract(lastEMC);
            lastEMC = emc;

            change = BigInteger.ZERO;
            for(BigInteger h : emcHistory) change = change.add(h);
            change = change.divide(BigInteger.valueOf(emcHistory.length));
        }
    }

    @SubscribeEvent
    public static void clientDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        if(!Config.emcDisplay.get()) return;
        emc = lastEMC = change = BigInteger.ZERO;
        Arrays.fill(emcHistory, BigInteger.ZERO);
        tick = 0;
    }

    @SubscribeEvent
    public static void onRenderGUI(RenderGameOverlayEvent.Text event) {
        if(!Config.emcDisplay.get()) return;
        String str = EMCFormat.INSTANCE.format(emc);
        if(!change.equals(BigInteger.ZERO)) str += " " + (change.compareTo(BigInteger.ZERO) > 0 ? (TextFormatting.GREEN + "+") : (TextFormatting.RED + "-")) + EMCFormat.INSTANCE.format(change) + "/s";
        event.getLeft().add(String.format("EMC: %s", str));
    }
}
