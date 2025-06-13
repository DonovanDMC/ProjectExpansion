package cool.furry.mc.neoforge.projectexpansion.gui.container;

import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.PacketUpdateCondenserLock;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.gameObjs.container.PEContainer;
import moze_intel.projecte.gameObjs.container.slots.SlotCondenserLock;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public class ContainerCondenserMK3Output extends ContainerBase {
    private final BlockEntityCondenserMK3.SidedHandler handler;
    BlockEntityCondenserMK3 blockEntity;
    public ContainerCondenserMK3Output(int windowId, Inventory playerInv, BlockEntityCondenserMK3 blockEntity) {
        this(MenuTypes.CONDENSER_MK3_OUTPUT.get(), windowId, playerInv, blockEntity);
    }

    public ContainerCondenserMK3Output(MenuType<?> type, int windowId, Inventory playerInv, BlockEntityCondenserMK3 blockEntity) {
        super(type, windowId, playerInv);
        this.blockEntity = blockEntity;
        this.handler = blockEntity.getOutput();
        blockEntity.startOpen(this.playerInv.player);
        initSlots();
    }

    protected void initSlots() {
        int rows = 9, columns = 20;
        IItemHandler inventory = handler.getInventory();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                this.addSlot(new ValidatedSlot(inventory,  j + i * columns, 12 + j * 18, 8 + i * 18, SlotPredicates.ALWAYS_FALSE));
            }
        }
        addPlayerInventory(111, 172);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        blockEntity.stopOpen(player);
    }

    public boolean blockEntityMatches(BlockEntityCondenserMK3 be) {
        return blockEntity == be;
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerBase.stillValid(player, blockEntity, () -> blockEntity.getBlockState().getBlock());
    }
}
