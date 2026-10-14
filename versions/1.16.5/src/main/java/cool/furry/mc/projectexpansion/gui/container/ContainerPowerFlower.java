package cool.furry.mc.projectexpansion.gui.container;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import cool.furry.mc.projectexpansion.util.PowerFlowerMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.math.BlockPos;

public class ContainerPowerFlower extends Container {
    public static final int BUTTON_MODE = 0;

    // on the client this is its own copy of the flower, kept up to date by block updates
    public final BlockEntityPowerFlower flower;
    private final IntReferenceHolder mode = IntReferenceHolder.standalone();

    public ContainerPowerFlower(int id, PlayerInventory inventory, BlockEntityPowerFlower flower) {
        super(MenuTypes.POWER_FLOWER.get(), id);
        this.flower = flower;
        mode.set(flower.getMode().ordinal());
        addDataSlot(mode);
    }

    public static ContainerPowerFlower fromNetwork(int id, PlayerInventory inventory, PacketBuffer buf) {
        BlockPos pos = buf.readBlockPos();
        TileEntity blockEntity = inventory.player.getCommandSenderWorld().getBlockEntity(pos);
        if (blockEntity instanceof BlockEntityPowerFlower) return new ContainerPowerFlower(id, inventory, (BlockEntityPowerFlower) blockEntity);
        throw new IllegalStateException("Client could not locate the power flower at " + pos);
    }

    public PowerFlowerMode getMode() {
        return PowerFlowerMode.byOrdinal(mode.get());
    }

    @Override
    public void broadcastChanges() {
        mode.set(flower.getMode().ordinal());
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(PlayerEntity player, int id) {
        if (id != BUTTON_MODE || !flower.owner.equals(player.getUUID())) return false;
        flower.setMode(flower.getMode().next());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        BlockPos pos = flower.getBlockPos();
        return !flower.isRemoved() && flower.owner.equals(player.getUUID()) && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
