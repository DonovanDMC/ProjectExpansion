package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketArcaneTransmutationTabletSelectRecipe(ResourceLocation recipe) implements IPacket {
    public static final CustomPacketPayload.Type<PacketArcaneTransmutationTabletSelectRecipe> TYPE = new CustomPacketPayload.Type<>(Main.rl("arcane_transmutation_tablet_select_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneTransmutationTabletSelectRecipe> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PacketArcaneTransmutationTabletSelectRecipe::recipe,
            PacketArcaneTransmutationTabletSelectRecipe::new
    );

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.selectRecipe(recipe);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
