package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.gui.GUIAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketOpenAlchemicalBookGUI;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketSyncAlchemicalBookLocations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

import javax.annotation.Nullable;
import java.util.Objects;

// we have to move these here, thanks to shenanigans with loading the Screen class
// on the DEDICATED_SERVER dist
public class ClientSideHandler {
    public static void handleAlchemicalBookOpen(PacketOpenAlchemicalBookGUI packet) {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        GUIAlchemicalBook gui = new GUIAlchemicalBook(player, packet.hand(), packet.locations(), packet.canEdit());
        Minecraft.getInstance().setScreen(gui);
    }

    public static void handleSyncAlchemicalBookLocations(PacketSyncAlchemicalBookLocations packet) {
        @Nullable Screen currentScreen = Minecraft.getInstance().screen;
        if(currentScreen instanceof GUIAlchemicalBook gui) {
            gui.setLocations(packet.locations(), packet.canEdit());
        }
    }
}
