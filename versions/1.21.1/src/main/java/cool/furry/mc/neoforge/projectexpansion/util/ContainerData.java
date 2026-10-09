package cool.furry.mc.neoforge.projectexpansion.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Optional;

public record ContainerData(boolean inHand, Optional<InteractionHand> hand, int selected) {
    public static StreamCodec<FriendlyByteBuf, ContainerData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ContainerData::inHand,
            ByteBufCodecs.optional(NeoForgeStreamCodecs.enumCodec(InteractionHand.class)), ContainerData::hand,
            ByteBufCodecs.INT, ContainerData::selected,
            ContainerData::new
    );

    public void encode(FriendlyByteBuf buf) {
        STREAM_CODEC.encode(buf, this);
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
        return STREAM_CODEC.decode(buf);
    }
}
