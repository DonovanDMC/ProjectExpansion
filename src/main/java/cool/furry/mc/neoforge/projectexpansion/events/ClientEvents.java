package cool.furry.mc.neoforge.projectexpansion.events;

import com.mojang.blaze3d.platform.InputConstants;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.client.Keybinds;
import cool.furry.mc.neoforge.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICollector;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICondenserMK3Input;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICondenserMK3Output;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketOpenTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketToggleEmcLock;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import cool.furry.mc.neoforge.projectexpansion.util.EmcLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {
    @EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(MenuTypes.COLLECTOR_TIER_1.get(), GUICollector.Tier1::new);
            event.register(MenuTypes.COLLECTOR_TIER_2.get(), GUICollector.Tier2::new);
            event.register(MenuTypes.COLLECTOR_TIER_3.get(), GUICollector.Tier3::new);
            event.register(MenuTypes.CONDENSER_MK3_INPUT.get(), GUICondenserMK3Input::new);
            event.register(MenuTypes.CONDENSER_MK3_OUTPUT.get(), GUICondenserMK3Output::new);
            event.register(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), GUIArcaneTransmutationTablet::new);
        }

        @SubscribeEvent
        public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
            Keybinds.register(event);
        }
    }

    @EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        @SubscribeEvent
        public static void clientTickEvent(ClientTickEvent.Pre event) {
            if (!Keybinds.REGISTERED) return;

            boolean openTransmutationTablet = Keybinds.OPEN_TRANSMUTATION_TABLET.consumeClick();
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;

            if (openTransmutationTablet) PacketDistributor.sendToServer(PacketOpenTransmutationTablet.INSTANCE);
        }

        @SubscribeEvent
        public static void mouseButtonPressedEvent(ScreenEvent.MouseButtonPressed.Pre event) {
            if (!Keybinds.REGISTERED || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
            // the creative inventory doesn't use real slots
            if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen) return;

            // key mappings don't update while a screen is open, so check the key directly
            InputConstants.Key key = Keybinds.TOGGLE_EMC_LOCK.getKey();
            Minecraft mc = Minecraft.getInstance();
            if (key.getType() != InputConstants.Type.KEYSYM || key == InputConstants.UNKNOWN || !InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue())) return;

            Slot slot = screen.getSlotUnderMouse();
            if (mc.player == null || slot == null || slot.container != mc.player.getInventory() || !EmcLock.canToggle(slot.getItem())) return;

            PacketDistributor.sendToServer(new PacketToggleEmcLock(screen.getMenu().containerId, slot.index));
            event.setCanceled(true);
        }
    }
}
