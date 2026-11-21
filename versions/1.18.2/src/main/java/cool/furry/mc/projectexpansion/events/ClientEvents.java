package cool.furry.mc.projectexpansion.events;

import com.mojang.blaze3d.platform.InputConstants;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.client.Keybinds;
import cool.furry.mc.projectexpansion.commands.CommandEmcDisplay;
import cool.furry.mc.projectexpansion.gui.EMCDisplay;
import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.GUICollector;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Input;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Output;
import cool.furry.mc.projectexpansion.gui.GUIEMCRefinery;
import cool.furry.mc.projectexpansion.gui.GUIEmcPipe;
import cool.furry.mc.projectexpansion.gui.GUIPowerFlower;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketOpenTransmutationTablet;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketToggleEmcLock;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import cool.furry.mc.projectexpansion.util.EmcLock;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(MenuTypes.COLLECTOR_TIER_1.get(), GUICollector.Tier1::new);
                MenuScreens.register(MenuTypes.EMC_PIPE.get(), GUIEmcPipe::new);
                MenuScreens.register(MenuTypes.POWER_FLOWER.get(), GUIPowerFlower::new);
                // the ores are an overlay on stone, 1.18 doesn't read render types from models
                ItemBlockRenderTypes.setRenderLayer(Blocks.IANTHIUM_ORE.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(Blocks.DEEPSLATE_IANTHIUM_ORE.get(), RenderType.cutout());
                // same for the overlays on an emc storage's output and off sides
                Blocks.EMC_STORAGE.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout()));
                // and an emc link's star on its base
                for (Matter matter : Matter.VALUES) ItemBlockRenderTypes.setRenderLayer(matter.getEMCLink(), RenderType.cutout());
                MenuScreens.register(MenuTypes.EMC_REFINERY.get(), GUIEMCRefinery::new);
                MenuScreens.register(MenuTypes.COLLECTOR_TIER_2.get(), GUICollector.Tier2::new);
                MenuScreens.register(MenuTypes.COLLECTOR_TIER_3.get(), GUICollector.Tier3::new);
                MenuScreens.register(MenuTypes.CONDENSER_MK3_INPUT.get(), GUICondenserMK3Input::new);
                MenuScreens.register(MenuTypes.CONDENSER_MK3_OUTPUT.get(), GUICondenserMK3Output::new);
                MenuScreens.register(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), GUIArcaneTransmutationTablet::new);
                // key mappings and overlays are registered here before 1.19
                Keybinds.register();
                EMCDisplay.Overlay.register();
            });
        }
    }

    @Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GameEvents {
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
        public static void mouseButtonPressedEvent(ScreenEvent.MouseClickedEvent.Pre event) {
            if (!Keybinds.REGISTERED || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
            if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

            // key mappings don't update while a screen is open, so check the key directly
            InputConstants.Key key = Keybinds.TOGGLE_EMC_LOCK.getKey();
            Minecraft mc = Minecraft.getInstance();
            if (key.getType() != InputConstants.Type.KEYSYM || key == InputConstants.UNKNOWN || !InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue())) return;

            Slot slot = screen.getSlotUnderMouse();
            if (mc.player == null || slot == null || slot.container != mc.player.getInventory() || !EmcLock.canToggle(slot.getItem())) return;

            PacketHandler.sendToServer(new PacketToggleEmcLock(slot.getSlotIndex()));
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            CommandEmcDisplay.register(event.getDispatcher());
        }
    }
}
