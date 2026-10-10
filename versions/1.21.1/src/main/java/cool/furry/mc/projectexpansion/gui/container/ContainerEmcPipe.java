package cool.furry.mc.projectexpansion.gui.container;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import cool.furry.mc.projectexpansion.util.pipe.Distribution;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import cool.furry.mc.projectexpansion.util.pipe.RedstoneMode;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ContainerEmcPipe extends AbstractContainerMenu {
    public static final int BUTTON_REDSTONE = 0;
    public static final int BUTTON_DISTRIBUTION = 1;
    private static final int DATA_REDSTONE = 0;
    private static final int DATA_DISTRIBUTION = 1;
    private static final int DATA_UPGRADE = 2;

    @Nullable
    private final BlockEntityEmcPipe pipe;
    public final Direction side;
    private final Container upgrade;
    private final ContainerData data;

    public ContainerEmcPipe(int id, Inventory inventory, BlockEntityEmcPipe pipe, Direction side) {
        this(id, inventory, pipe, side, pipe.getUpgradeContainer(side), new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_REDSTONE -> pipe.getStoredSettings(side).redstoneMode().ordinal();
                    case DATA_DISTRIBUTION -> pipe.getStoredSettings(side).distribution().ordinal();
                    case DATA_UPGRADE -> {
                        PipeUpgrade upgrade = pipe.getUpgrade(side);
                        yield upgrade == null ? 0 : upgrade.ordinal() + 1;
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 3;
            }
        });
    }

    private ContainerEmcPipe(int id, Inventory inventory, @Nullable BlockEntityEmcPipe pipe, Direction side, Container upgrade, ContainerData data) {
        super(MenuTypes.EMC_PIPE.get(), id);
        this.pipe = pipe;
        this.side = side;
        this.upgrade = upgrade;
        this.data = data;

        addSlot(new Slot(upgrade, 0, 152, 6) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PipeUpgrade.fromStack(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }

    // the client only needs the side, the upgrade slot and settings are synced
    public static ContainerEmcPipe fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBlockPos();
        Direction side = buf.readEnum(Direction.class);
        return new ContainerEmcPipe(id, inventory, null, side, new SimpleContainer(1), new SimpleContainerData(3));
    }

    public RedstoneMode getRedstoneMode() {
        return RedstoneMode.VALUES[Math.floorMod(data.get(DATA_REDSTONE), RedstoneMode.VALUES.length)];
    }

    public Distribution getDistribution() {
        return Distribution.VALUES[Math.floorMod(data.get(DATA_DISTRIBUTION), Distribution.VALUES.length)];
    }

    public @Nullable PipeUpgrade getUpgrade() {
        int value = data.get(DATA_UPGRADE);
        return value <= 0 || value > PipeUpgrade.VALUES.length ? null : PipeUpgrade.VALUES[value - 1];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pipe == null) return false;
        PipeUpgrade upgrade = pipe.getUpgrade(side);
        PipeSettings settings = pipe.getStoredSettings(side);
        if (id == BUTTON_REDSTONE && PipeUpgrade.canChangeRedstoneMode(upgrade)) {
            pipe.setStoredSettings(side, settings.withRedstoneMode(settings.redstoneMode().next()));
            return true;
        }
        if (id == BUTTON_DISTRIBUTION && PipeUpgrade.canChangeDistribution(upgrade)) {
            pipe.setStoredSettings(side, settings.withDistribution(settings.distribution().next()));
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (PipeUpgrade.fromStack(stack) == null || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return upgrade.stillValid(player);
    }
}
