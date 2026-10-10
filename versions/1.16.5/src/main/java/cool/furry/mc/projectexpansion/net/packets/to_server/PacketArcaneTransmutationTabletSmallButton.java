package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

public final class PacketArcaneTransmutationTabletSmallButton implements IPacket {
    private final Action action;

    public PacketArcaneTransmutationTabletSmallButton(Action action) {
        this.action = action;
    }

    public Action action() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketArcaneTransmutationTabletSmallButton that = (PacketArcaneTransmutationTabletSmallButton) o;
        return Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action);
    }

    @Override
    public String toString() {
        return "PacketArcaneTransmutationTabletSmallButton[action=" + action + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet) {
            ContainerArcaneTransmutationTablet container = ((ContainerArcaneTransmutationTablet) player.containerMenu);
            switch (action) {
                case ROTATE: container.rotateCrafting(true); break;
                case ROTATE_CC: container.rotateCrafting(false); break;
                case BALANCE: container.balanceCrafting(); break;
                case SPREAD: container.spreadCrafting(); break;
                case CLEAR: container.clearCrafting(false); break;
                case CLEAR_FORCE: container.clearCrafting(true); break;
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeEnum(action);
    }

    public static PacketArcaneTransmutationTabletSmallButton decode(PacketBuffer buf) {
        return new PacketArcaneTransmutationTabletSmallButton(buf.readEnum(Action.class));
    }

    public enum Action {
        ROTATE, ROTATE_CC, BALANCE, SPREAD, CLEAR, CLEAR_FORCE
    }
}
