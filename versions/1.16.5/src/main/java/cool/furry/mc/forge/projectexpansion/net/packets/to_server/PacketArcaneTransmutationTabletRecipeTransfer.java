package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.BufferHelper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PacketArcaneTransmutationTabletRecipeTransfer implements IPacket {
    private final List<List<ItemStack>> recipe;
    private final boolean transferAll;
    private final Optional<ResourceLocation> recipeId;

    public PacketArcaneTransmutationTabletRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll, Optional<ResourceLocation> recipeId) {
        this.recipe = recipe;
        this.transferAll = transferAll;
        this.recipeId = recipeId;
    }

    public List<List<ItemStack>> recipe() {
        return recipe;
    }

    public boolean transferAll() {
        return transferAll;
    }

    public Optional<ResourceLocation> recipeId() {
        return recipeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketArcaneTransmutationTabletRecipeTransfer that = (PacketArcaneTransmutationTabletRecipeTransfer) o;
        return Objects.equals(recipe, that.recipe) && Objects.equals(transferAll, that.transferAll) && Objects.equals(recipeId, that.recipeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipe, transferAll, recipeId);
    }

    @Override
    public String toString() {
        return "PacketArcaneTransmutationTabletRecipeTransfer[recipe=" + recipe + ", transferAll=" + transferAll + ", recipeId=" + recipeId + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet) {
            ContainerArcaneTransmutationTablet container = ((ContainerArcaneTransmutationTablet) player.containerMenu);
            container.onRecipeTransfer(recipe, transferAll, recipeId);
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        BufferHelper.writeCollection(buf, recipe, (b, stacks) -> BufferHelper.writeCollection(b, stacks, PacketBuffer::writeItem));
        buf.writeBoolean(transferAll);
        BufferHelper.writeOptional(buf, recipeId, PacketBuffer::writeResourceLocation);
    }

    public static PacketArcaneTransmutationTabletRecipeTransfer decode(PacketBuffer buf) {
        return new PacketArcaneTransmutationTabletRecipeTransfer(BufferHelper.readList(buf, b -> BufferHelper.readList(b, PacketBuffer::readItem)), buf.readBoolean(), BufferHelper.readOptional(buf, PacketBuffer::readResourceLocation));
    }
}
