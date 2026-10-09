package cool.furry.mc.neoforge.projectexpansion.gui.container.slots;

import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotOutput;

public class PXOutputSlot extends SlotOutput {
    protected final TransmutationInventory inv;
    public PXOutputSlot(TransmutationInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
        this.inv = inv;
    }
}
