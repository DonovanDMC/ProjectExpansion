package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

public record PacketArcaneTransmutationTabletRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll, Optional<ResourceLocation> recipeId) implements IPacket {
    public static final CustomPacketPayload.Type<PacketArcaneTransmutationTabletRecipeTransfer> TYPE = new CustomPacketPayload.Type<>(Main.rl("arcane_transmutation_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneTransmutationTabletRecipeTransfer> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), PacketArcaneTransmutationTabletRecipeTransfer::recipe,
            ByteBufCodecs.BOOL, PacketArcaneTransmutationTabletRecipeTransfer::transferAll,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), PacketArcaneTransmutationTabletRecipeTransfer::recipeId,
            PacketArcaneTransmutationTabletRecipeTransfer::new
    );

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.onRecipeTransfer(recipe, transferAll, recipeId);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
