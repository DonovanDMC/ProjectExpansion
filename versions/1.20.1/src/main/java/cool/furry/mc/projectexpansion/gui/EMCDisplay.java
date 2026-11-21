package cool.furry.mc.projectexpansion.gui;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.utils.text.ILangEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.math.BigInteger;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EMCDisplay {
    public static final Overlay INSTANCE = new Overlay();
    private static BigInteger emc = BigInteger.ZERO;
    private static final BigInteger[] history = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};
    private static @Nullable BigInteger lastEMC = null;
    private static int tick = 0;
    private static int repeatedFailures = 0;

    private static @Nullable LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Config.client.Spec.isLoaded() && !Config.client.emcDisplay.get()) return;
        LocalPlayer player = getPlayer();
        tick++;
        if (player != null && tick >= 20) {
            tick = 0;
            IKnowledgeProvider provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
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
            if (lastEMC != null) {
                history[1] = history[0];
                history[0] = emc.subtract(lastEMC);
            }
            lastEMC = emc;
        }
    }

    public static String getText() {
        BigInteger avg = history[0].add(history[1]).divide(BigInteger.valueOf(history.length));
        String str = EMCFormat.format(emc);
        if (!avg.equals(BigInteger.ZERO)) str += " " + (avg.compareTo(BigInteger.ZERO) > 0 ? (ChatFormatting.GREEN + "+") : (ChatFormatting.RED + "-")) + EMCFormat.format(avg.abs()) + "/s";
        return String.format("EMC: %s", str);
    }

    private static void reset() {
        emc = BigInteger.ZERO;
        lastEMC = null;
        history[0] = history[1] = BigInteger.ZERO;
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

    @Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Overlay implements IGuiOverlay {
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("emc_display", INSTANCE);
        }

        @Override
        public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
            if (Config.client.Spec.isLoaded() && !Config.client.emcDisplay.get()) return;
            Minecraft mc = Minecraft.getInstance();
            // hidden with the rest of the hud (F1)
            if (mc.options.hideGui) return;
            // the editor draws its own copy while it's open
            if (mc.screen instanceof GUIEmcDisplayEditor) return;
            String text = getText();
            int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
            EmcDisplayPosition position = Config.client.emcDisplayPosition.get();
            int x = EmcDisplayLayout.toScreen(position.isLeft(), Config.client.emcDisplayOffsetX.get(), mc.font.width(text), width);
            int y = EmcDisplayLayout.toScreen(position.isTop(), Config.client.emcDisplayOffsetY.get(), mc.font.lineHeight, height);

            guiGraphics.drawString(mc.font, text, x, y, 0xffffff);
        }
    }

    public enum EmcDisplayPosition implements ILangEntry {
        TOP_LEFT(Lang.Configuration.EMC_DISPLAY_POSITION_TOP_LEFT, true, true),
        TOP_RIGHT(Lang.Configuration.EMC_DISPLAY_POSITION_TOP_RIGHT, true, false),
        BOTTOM_LEFT(Lang.Configuration.EMC_DISPLAY_POSITION_BOTTOM_LEFT, false, true),
        BOTTOM_RIGHT(Lang.Configuration.EMC_DISPLAY_POSITION_BOTTOM_RIGHT, false, false);

        private final ILangEntry translation;
        private final boolean top;
        private final boolean left;

        EmcDisplayPosition(ILangEntry translation, boolean top, boolean left) {
            this.translation = translation;
            this.top = top;
            this.left = left;
        }
        public Component getTranslatedName() {
            return this.translation.translate();
        }

        @Override
        public String getTranslationKey() {
            return this.translation.getTranslationKey();
        }

        public static EmcDisplayPosition of(boolean top, boolean left) {
            for (EmcDisplayPosition position : values()) {
                if (position.top == top && position.left == left) return position;
            }
            return TOP_LEFT;
        }

        public boolean isTop() {
            return top;
        }

        public boolean isBottom() {
            return !top;
        }

        public boolean isLeft() {
            return left;
        }

        public boolean isRight() {
            return !left;
        }
    }
}

