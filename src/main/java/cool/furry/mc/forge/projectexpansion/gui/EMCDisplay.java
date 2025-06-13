package cool.furry.mc.neoforge.projectexpansion.gui;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class EMCDisplay {
    public static final Overlay INSTANCE = new Overlay();
    public static final int PADDING_X = 2;
    public static final int PADDING_Y = 2;
    public static final List<String> POSITIONS = List.of("TOPLEFT", "TOPRIGHT", "BOTTOMLEFT", "BOTTOMRIGHT");
    private static BigInteger emc = BigInteger.ZERO;
    private static final BigInteger[] history = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};
    private static BigInteger lastEMC = BigInteger.ZERO;
    private static int tick = 0;
    private static int repeatedFailures = 0;

    private static @Nullable LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!Config.client.emcDisplay.get()) return;
        LocalPlayer player = getPlayer();
        tick++;
        if (player != null && tick >= 20) {
            tick = 0;
            IKnowledgeProvider provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
            if (provider == null) {
                ++repeatedFailures;
                if (repeatedFailures < 10) {
                    Main.Logger.warn("Failed to get provider in EMCDisplay");
                } else if (repeatedFailures == 10) {
                    Main.Logger.error("Failed to get provider in EMCDisplay 10 times in a row, assuming error and no longer logging.");
                }
                return;
            }

            repeatedFailures = 0;
            emc = provider.getEmc();
            history[1] = history[0];
            history[0] = emc.subtract(lastEMC);
            lastEMC = emc;
        }
    }

    private static void reset() {
        emc = lastEMC = BigInteger.ZERO;
        tick = 0;
    }

    @SubscribeEvent
    public static void clientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!Config.client.emcDisplay.get()) return;
        reset();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!Config.client.emcDisplay.get()) return;
        reset();
    }

    @EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Overlay implements LayeredDraw.Layer {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(Main.rl("emc_display"), INSTANCE);
        }

        @Override
        public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
            if (!Config.client.emcDisplay.get()) return;
            Minecraft mc = Minecraft.getInstance();
            BigInteger avg = history[0].add(history[1]);
            String str = EMCFormat.format(emc);
            if (!avg.equals(BigInteger.ZERO)) str += " " + (avg.compareTo(BigInteger.ZERO) > 0 ? (ChatFormatting.GREEN + "+") : (ChatFormatting.RED + "-")) + EMCFormat.format(avg.abs()) + "/s";
            String text = String.format("EMC: %s", str);
            int x = PADDING_X, y = PADDING_Y, width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight(), fontWidth = mc.font.width(text), fontHeight = mc.font.lineHeight;

            String position = Config.client.emcDisplayPosition.get();

            if (position.endsWith("RIGHT")) {
                x = width - fontWidth - PADDING_X;
            }
            if (position.startsWith("BOTTOM")) {
                y = height - fontHeight - PADDING_Y;
            }

            guiGraphics.drawString(mc.font, text, x, y, 0xffffff);
        }
    }
}

