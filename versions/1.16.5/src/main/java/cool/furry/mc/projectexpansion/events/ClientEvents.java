package cool.furry.mc.projectexpansion.events;

import cool.furry.mc.projectexpansion.gui.GUIEMCRefinery;
import cool.furry.mc.projectexpansion.gui.GUIEmcDisplayEditor;
import cool.furry.mc.projectexpansion.gui.GUIEmcPipe;
import cool.furry.mc.projectexpansion.gui.GUIPowerFlower;
import cool.furry.mc.projectexpansion.registries.Blocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.client.Keybinds;
import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.GUICollector;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Input;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Output;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketOpenTransmutationTablet;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketToggleEmcLock;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import cool.furry.mc.projectexpansion.util.EmcLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Pattern;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ScreenManager.register(MenuTypes.COLLECTOR_TIER_1.get(), GUICollector.Tier1::new);
                ScreenManager.register(MenuTypes.COLLECTOR_TIER_2.get(), GUICollector.Tier2::new);
                ScreenManager.register(MenuTypes.COLLECTOR_TIER_3.get(), GUICollector.Tier3::new);
                ScreenManager.register(MenuTypes.CONDENSER_MK3_INPUT.get(), GUICondenserMK3Input::new);
                ScreenManager.register(MenuTypes.CONDENSER_MK3_OUTPUT.get(), GUICondenserMK3Output::new);
                ScreenManager.register(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), GUIArcaneTransmutationTablet::new);
                ScreenManager.register(MenuTypes.EMC_PIPE.get(), GUIEmcPipe::new);
                ScreenManager.register(MenuTypes.POWER_FLOWER.get(), GUIPowerFlower::new);
                ScreenManager.register(MenuTypes.EMC_REFINERY.get(), GUIEMCRefinery::new);
                // the ore is an overlay on stone, and so are an emc storage's output and off sides
                RenderTypeLookup.setRenderLayer(Blocks.IANTHIUM_ORE.get(), RenderType.cutout());
                Blocks.EMC_STORAGE.values().forEach(block -> RenderTypeLookup.setRenderLayer(block.get(), RenderType.cutout()));
                // key mappings are registered here before 1.19
                Keybinds.register();
            });
        }
    }

    @Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GameEvents {
        private static final Pattern EMC_DISPLAY_COMMAND = Pattern.compile("^/(px|pex|projectexpansion) emcdisplay$", Pattern.CASE_INSENSITIVE);

        @SubscribeEvent
        public static void clientTickEvent(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            if (!Keybinds.REGISTERED) return;

            boolean openTransmutationTablet = Keybinds.OPEN_TRANSMUTATION_TABLET.consumeClick();
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;

            if (openTransmutationTablet) PacketHandler.sendToServer(PacketOpenTransmutationTablet.INSTANCE);
        }

        @SubscribeEvent
        public static void mouseButtonPressedEvent(GuiScreenEvent.MouseClickedEvent.Pre event) {
            if (!Keybinds.REGISTERED || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
            // the creative inventory doesn't use real slots
            if (!(event.getGui() instanceof ContainerScreen<?>) || ((ContainerScreen<?>) event.getGui()) instanceof CreativeScreen) return;
            ContainerScreen<?> screen = (ContainerScreen<?>) event.getGui();

            // key mappings don't update while a screen is open, so check the key directly
            InputMappings.Input key = Keybinds.TOGGLE_EMC_LOCK.getKey();
            Minecraft mc = Minecraft.getInstance();
            if (key.getType() != InputMappings.Type.KEYSYM || key == InputMappings.UNKNOWN || !InputMappings.isKeyDown(mc.getWindow().getWindow(), key.getValue())) return;

            Slot slot = screen.getSlotUnderMouse();
            if (mc.player == null || slot == null || slot.container != mc.player.inventory || !EmcLock.canToggle(slot.getItem())) return;

            PacketHandler.sendToServer(new PacketToggleEmcLock(screen.getMenu().containerId, slot.index));
            event.setCanceled(true);
        }

        // there are no client commands before 1.18, so the editor's command is caught before it's sent
        @SubscribeEvent
        public static void clientChatEvent(ClientChatEvent event) {
            if (!EMC_DISPLAY_COMMAND.matcher(event.getMessage().trim()).matches()) return;
            Minecraft mc = Minecraft.getInstance();
            // the chat screen closes after this, so the editor has to open after that
            mc.tell(() -> mc.setScreen(new GUIEmcDisplayEditor(null)));
            event.setCanceled(true);
        }
    }
}
