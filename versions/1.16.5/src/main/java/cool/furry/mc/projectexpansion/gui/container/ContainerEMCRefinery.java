package cool.furry.mc.projectexpansion.gui.container;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import moze_intel.projecte.gameObjs.container.PEContainer.BoxedLong;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.IntReferenceHolder;

public class ContainerEMCRefinery extends ContainerBase {
    public final BlockEntityEMCRefinery refinery;
    public final BoxedLong emc = new BoxedLong();
    public final BoxedLong cost = new BoxedLong();
    private final IntReferenceHolder progress = IntReferenceHolder.standalone();
    private final IntReferenceHolder time = IntReferenceHolder.standalone();

    public ContainerEMCRefinery(int id, PlayerInventory inventory, BlockEntityEMCRefinery refinery) {
        super(MenuTypes.EMC_REFINERY.get(), id, inventory);
        this.refinery = refinery;
        longFields.add(emc);
        longFields.add(cost);
        addDataSlot(progress);
        addDataSlot(time);
        addSlot(new ValidatedSlot(refinery.getItems(), BlockEntityEMCRefinery.INPUT_SLOT, 56, 35, refinery::canRefine));
        addSlot(new ValidatedSlot(refinery.getItems(), BlockEntityEMCRefinery.OUTPUT_SLOT, 116, 35, SlotPredicates.ALWAYS_FALSE));
        addSlot(new ValidatedSlot(refinery.getItems(), BlockEntityEMCRefinery.STAR_SLOT, 8, 53, BlockEntityEMCRefinery::isStar));
        addPlayerInventory(8, 84);
    }

    @Override
    protected void broadcastPX(boolean all) {
        emc.set(refinery.getStoredEmc());
        RefiningRecipe recipe = refinery.getRecipe().orElse(null);
        cost.set(recipe == null ? 0 : recipe.emc());
        // vanilla only syncs shorts, so the progress is sent as a fraction of 1000
        time.set(recipe == null ? 0 : 1000);
        progress.set(recipe == null ? 0 : (int) ((long) refinery.getProgress() * 1000 / recipe.time()));
        super.broadcastPX(all);
    }

    public double getProgress() {
        return time.get() == 0 ? 0 : (double) progress.get() / time.get();
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return stillValid(player, refinery, Blocks.EMC_REFINERY::get);
    }
}
