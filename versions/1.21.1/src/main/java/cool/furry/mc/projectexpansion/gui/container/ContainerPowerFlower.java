package cool.furry.mc.projectexpansion.gui.container;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import cool.furry.mc.projectexpansion.util.PowerFlowerMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class ContainerPowerFlower extends AbstractContainerMenu {
    public static final int BUTTON_MODE = 0;

    // on the client this is its own copy of the flower, kept up to date by block updates
    public final BlockEntityPowerFlower flower;
    private final DataSlot mode = DataSlot.standalone();

    public ContainerPowerFlower(int id, Inventory inventory, BlockEntityPowerFlower flower) {
        super(MenuTypes.POWER_FLOWER.get(), id);
        this.flower = flower;
        mode.set(flower.getMode().ordinal());
        addDataSlot(mode);
    }

    public static ContainerPowerFlower fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof BlockEntityPowerFlower flower) return new ContainerPowerFlower(id, inventory, flower);
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
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_MODE || !flower.owner.equals(player.getUUID())) return false;
        flower.setMode(flower.getMode().next());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = flower.getBlockPos();
        return !flower.isRemoved() && flower.owner.equals(player.getUUID()) && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
