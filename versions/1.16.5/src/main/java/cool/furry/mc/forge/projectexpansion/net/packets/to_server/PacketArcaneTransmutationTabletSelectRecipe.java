package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

public final class PacketArcaneTransmutationTabletSelectRecipe implements IPacket {
    private final ResourceLocation recipe;

    public PacketArcaneTransmutationTabletSelectRecipe(ResourceLocation recipe) {
        this.recipe = recipe;
    }

    public ResourceLocation recipe() {
        return recipe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketArcaneTransmutationTabletSelectRecipe that = (PacketArcaneTransmutationTabletSelectRecipe) o;
        return Objects.equals(recipe, that.recipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipe);
    }

    @Override
    public String toString() {
        return "PacketArcaneTransmutationTabletSelectRecipe[recipe=" + recipe + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet) {
            ContainerArcaneTransmutationTablet container = ((ContainerArcaneTransmutationTablet) player.containerMenu);
            container.selectRecipe(recipe);
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeResourceLocation(recipe);
    }

    public static PacketArcaneTransmutationTabletSelectRecipe decode(PacketBuffer buf) {
        return new PacketArcaneTransmutationTabletSelectRecipe(buf.readResourceLocation());
    }
}
