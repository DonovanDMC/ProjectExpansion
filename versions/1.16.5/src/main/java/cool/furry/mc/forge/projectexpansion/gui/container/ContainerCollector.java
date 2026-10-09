package cool.furry.mc.forge.projectexpansion.gui.container;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.forge.projectexpansion.registries.MenuTypes;
import moze_intel.projecte.gameObjs.container.PEContainer.BoxedLong;
import moze_intel.projecte.gameObjs.container.slots.SlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IntReferenceHolder;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;

public abstract class ContainerCollector extends ContainerBase {
    public final BlockEntityCollector collector;
    public final IntReferenceHolder sunLevel = IntReferenceHolder.standalone();
    public final BoxedBigInteger emc = new BoxedBigInteger();
    private final IntReferenceHolder kleinChargeProgress = IntReferenceHolder.standalone();
    private final IntReferenceHolder fuelProgress = IntReferenceHolder.standalone();
    public final BoxedLong kleinEmc = new BoxedLong();

    public ContainerCollector(ContainerType<?> type, int windowId, PlayerInventory inv, BlockEntityCollector collector) {
        super(type, windowId, inv);
        this.bigIntegerFields.add(emc);
        addDataSlot(sunLevel);
        addDataSlot(kleinChargeProgress);
        addDataSlot(fuelProgress);
        this.longFields.add(kleinEmc);
        this.collector = collector;
        // each tier is different enough from the last that extracting out variables would be more painful than it's worth
        switch (collector.getMatter()) {
            case BASIC: initSlotsTier1(); break;
            case DARK: initSlotsTier2(); break;
            default: initSlotsTier3(); break;
        }
    }

    void initSlotsTier1() {
        IItemHandler aux = collector.getAux();
        IItemHandler main = collector.getInput();

        //Klein Star Slot
        this.addSlot(new ValidatedSlot(aux, BlockEntityCollector.UPGRADING_SLOT, 124, 58, SlotPredicates.COLLECTOR_INV));
        int counter = 0;
        //Fuel Upgrade storage
        for (int i = 1; i >= 0; i--) {
            for (int j = 3; j >= 0; j--) {
                this.addSlot(new ValidatedSlot(main, counter++, 20 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));
            }
        }
        //Upgrade Result
        this.addSlot(new ValidatedSlot(aux, BlockEntityCollector.UPGRADE_SLOT, 124, 13, SlotPredicates.ALWAYS_FALSE));
        //Upgrade Target
        this.addSlot(new SlotGhost(aux, BlockEntityCollector.LOCK_SLOT, 153, 36, SlotPredicates.COLLECTOR_LOCK));
        addPlayerInventory(8, 84);
    }

    void initSlotsTier2() {
        IItemHandler aux = collector.getAux();
        IItemHandler main = collector.getInput();

        //Klein Star Slot
        this.addSlot(new ValidatedSlot(aux, BlockEntityCollector.UPGRADING_SLOT, 140, 58, SlotPredicates.COLLECTOR_INV));
        int counter = 0;
        //Fuel Upgrade storage
        for (int i = 2; i >= 0; i--) {
            for (int j = 3; j >= 0; j--) {
                this.addSlot(new ValidatedSlot(main, counter++, 18 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));
            }
        }
        //Upgrade Result
        this.addSlot(new ValidatedSlot(aux, BlockEntityCollector.UPGRADE_SLOT, 140, 13, SlotPredicates.ALWAYS_FALSE));
        //Upgrade Target
        this.addSlot(new SlotGhost(aux, BlockEntityCollector.LOCK_SLOT, 169, 36, SlotPredicates.COLLECTOR_LOCK));
        addPlayerInventory(20, 84);
    }

    void initSlotsTier3() {
        IItemHandler aux = collector.getAux();
        IItemHandler main = collector.getInput();

        //Klein Star Slot
        this.addSlot(new ValidatedSlot(aux, BlockEntityCollector.UPGRADING_SLOT, 158, 58, SlotPredicates.COLLECTOR_INV));
        int counter = 0;
        //Fuel Upgrade Slot
        for (int i = 3; i >= 0; i--) {
            for (int j = 3; j >= 0; j--) {
                this.addSlot(new ValidatedSlot(main, counter++, 18 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));
            }
        }
        //Upgrade Result
        this.addSlot(new ValidatedSlot(aux, BlockEntityCollector.UPGRADE_SLOT, 158, 13, SlotPredicates.ALWAYS_FALSE));
        //Upgrade Target
        this.addSlot(new SlotGhost(aux, BlockEntityCollector.LOCK_SLOT, 187, 36, SlotPredicates.COLLECTOR_LOCK));
        addPlayerInventory(30, 84);
    }

    @Override
    public ItemStack clicked(int slotID, int button, ClickType flag, PlayerEntity player) {
        Slot slot = tryGetSlot(slotID);
        if (slot instanceof SlotGhost && !slot.getItem().isEmpty()) {
            slot.set(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
        return super.clicked(slotID, button, flag, player);
    }

    @Override
    protected void broadcastPX(boolean all) {
        emc.set(collector.getStoredEmcBigInteger());
        sunLevel.set(collector.getSunLevel());
        kleinChargeProgress.set((int) (collector.getItemChargeProportion() * 8000));
        fuelProgress.set((int) (collector.getFuelProgress() * 8000));
        kleinEmc.set(collector.getItemCharge());
        super.broadcastPX(all);
    }

    protected Block getValidBlock() {
        return Objects.requireNonNull(collector.getMatter().getCollector());
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return stillValid(player, collector, this::getValidBlock);
    }

    public double getKleinChargeProgress() {
        return kleinChargeProgress.get() / 8000.0;
    }

    public double getFuelProgress() {
        return fuelProgress.get() / 8000.0;
    }

    public static class Tier1 extends ContainerCollector {
        public Tier1(int windowId, PlayerInventory inv, BlockEntityCollector collector) {
            super(MenuTypes.COLLECTOR_TIER_1.get(), windowId, inv, collector);
            this.initSlotsTier1();
        }
    }

    public static class Tier2 extends ContainerCollector {
        public Tier2(int windowId, PlayerInventory inv, BlockEntityCollector collector) {
            super(MenuTypes.COLLECTOR_TIER_2.get(), windowId, inv, collector);
            this.initSlotsTier2();
        }
    }

    public static class Tier3 extends ContainerCollector {
        public Tier3(int windowId, PlayerInventory inv, BlockEntityCollector collector) {
            super(MenuTypes.COLLECTOR_TIER_3.get(), windowId, inv, collector);
            this.initSlotsTier3();
        }
    }
}
