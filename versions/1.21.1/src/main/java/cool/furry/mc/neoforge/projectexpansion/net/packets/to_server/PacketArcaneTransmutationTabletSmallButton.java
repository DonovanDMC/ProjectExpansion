package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketArcaneTransmutationTabletSmallButton(Action action) implements IPacket {
    public static final CustomPacketPayload.Type<PacketArcaneTransmutationTabletSmallButton> TYPE = new CustomPacketPayload.Type<>(Main.rl("arcane_transmutation_tablet_small_button"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneTransmutationTabletSmallButton> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(Action.class), PacketArcaneTransmutationTabletSmallButton::action,
            PacketArcaneTransmutationTabletSmallButton::new
    );

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
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
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        ROTATE, ROTATE_CC, BALANCE, SPREAD, CLEAR, CLEAR_FORCE
    }
}
