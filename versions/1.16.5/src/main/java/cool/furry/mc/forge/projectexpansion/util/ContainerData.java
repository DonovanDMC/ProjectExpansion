package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;

import java.util.Objects;
import java.util.Optional;

public final class ContainerData {
    private final boolean inHand;
    private final Optional<Hand> hand;
    private final int selected;

    public ContainerData(boolean inHand, Optional<Hand> hand, int selected) {
        this.inHand = inHand;
        this.hand = hand;
        this.selected = selected;
    }

    public boolean inHand() {
        return inHand;
    }

    public Optional<Hand> hand() {
        return hand;
    }

    public int selected() {
        return selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerData that = (ContainerData) o;
        return Objects.equals(inHand, that.inHand) && Objects.equals(hand, that.hand) && Objects.equals(selected, that.selected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inHand, hand, selected);
    }

    @Override
    public String toString() {
        return "ContainerData[inHand=" + inHand + ", hand=" + hand + ", selected=" + selected + "]";
    }

    public void encode(PacketBuffer buf) {
        buf.writeBoolean(inHand);
        BufferHelper.writeOptional(buf, hand, PacketBuffer::writeEnum);
        buf.writeInt(selected);
    }

    public static ContainerData inHand(Hand hand, int selected) {
        return new ContainerData(true, Optional.of(hand), selected);
    }

    public static void inHand(PacketBuffer buf, Hand hand, int selected) {
        new ContainerData(true, Optional.of(hand), selected).encode(buf);
    }

    public static ContainerData noHand() {
        return new ContainerData(false, Optional.empty(), 0);
    }

    public static void noHand(PacketBuffer buf) {
        new ContainerData(false, Optional.empty(), 0).encode(buf);
    }

    public static ContainerData decode(PacketBuffer buf) {
        return new ContainerData(buf.readBoolean(), BufferHelper.readOptional(buf, b -> b.readEnum(Hand.class)), buf.readInt());
    }
}
