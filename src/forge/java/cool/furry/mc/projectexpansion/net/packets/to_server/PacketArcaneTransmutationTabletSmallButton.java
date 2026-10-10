package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record PacketArcaneTransmutationTabletSmallButton(Action action) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            switch (action) {
                case ROTATE -> container.rotateCrafting(true);
                case ROTATE_CC -> container.rotateCrafting(false);
                case BALANCE -> container.balanceCrafting();
                case SPREAD -> container.spreadCrafting();
                case CLEAR -> container.clearCrafting(false);
                case CLEAR_FORCE -> container.clearCrafting(true);
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
    }

    public static PacketArcaneTransmutationTabletSmallButton decode(FriendlyByteBuf buf) {
        return new PacketArcaneTransmutationTabletSmallButton(buf.readEnum(Action.class));
    }

    public enum Action {
        ROTATE, ROTATE_CC, BALANCE, SPREAD, CLEAR, CLEAR_FORCE
    }
}
