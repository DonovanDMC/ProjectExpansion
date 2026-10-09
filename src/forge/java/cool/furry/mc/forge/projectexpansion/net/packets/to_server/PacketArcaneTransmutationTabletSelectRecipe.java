package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record PacketArcaneTransmutationTabletSelectRecipe(ResourceLocation recipe) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.selectRecipe(recipe);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipe);
    }

    public static PacketArcaneTransmutationTabletSelectRecipe decode(FriendlyByteBuf buf) {
        return new PacketArcaneTransmutationTabletSelectRecipe(buf.readResourceLocation());
    }
}
