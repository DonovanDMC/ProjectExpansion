package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Optional;

public record PacketArcaneTransmutationTabletRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll, Optional<ResourceLocation> recipeId) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.onRecipeTransfer(recipe, transferAll, recipeId);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(recipe, (b, stacks) -> b.writeCollection(stacks, FriendlyByteBuf::writeItem));
        buf.writeBoolean(transferAll);
        buf.writeOptional(recipeId, FriendlyByteBuf::writeResourceLocation);
    }

    public static PacketArcaneTransmutationTabletRecipeTransfer decode(FriendlyByteBuf buf) {
        return new PacketArcaneTransmutationTabletRecipeTransfer(buf.readList(b -> b.readList(FriendlyByteBuf::readItem)), buf.readBoolean(), buf.readOptional(FriendlyByteBuf::readResourceLocation));
    }
}
