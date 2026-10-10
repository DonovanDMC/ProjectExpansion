package cool.furry.mc.projectexpansion.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

import java.util.Optional;

public record ContainerData(boolean inHand, Optional<InteractionHand> hand, int selected) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(inHand);
        buf.writeOptional(hand, FriendlyByteBuf::writeEnum);
        buf.writeInt(selected);
    }

    public static ContainerData inHand(InteractionHand hand, int selected) {
        return new ContainerData(true, Optional.of(hand), selected);
    }

    public static void inHand(FriendlyByteBuf buf, InteractionHand hand, int selected) {
        new ContainerData(true, Optional.of(hand), selected).encode(buf);
    }

    public static ContainerData noHand() {
        return new ContainerData(false, Optional.empty(), 0);
    }

    public static void noHand(FriendlyByteBuf buf) {
        new ContainerData(false, Optional.empty(), 0).encode(buf);
    }

    public static ContainerData decode(FriendlyByteBuf buf) {
        return new ContainerData(buf.readBoolean(), buf.readOptional(b -> b.readEnum(InteractionHand.class)), buf.readInt());
    }
}
