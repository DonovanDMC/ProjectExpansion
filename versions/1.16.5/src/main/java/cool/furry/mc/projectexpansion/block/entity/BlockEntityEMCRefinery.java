package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.gui.container.ContainerEMCRefinery;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.TagNames;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.Inventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Optional;

public class BlockEntityEMCRefinery extends BlockEntityEMC implements INamedContainerProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int STAR_SLOT = 2;
    // enough to keep refining between pushes from collectors, which only come once a second
    public static final long CAPACITY = 1_000_000;

    private final StackHandler items = new StackHandler(3);
    private final IItemHandler automation = new Automation();
    // emc pushed in for the next tick, kept out of the buffer so the stored amount holds steady while it's supplied
    private long incoming;
    private long nextDue;
    private int progress;
    @Nullable
    private ResourceLocation progressRecipe;

    public BlockEntityEMCRefinery() {
        super(BlockEntityTypes.EMC_REFINERY.get(), BigInteger.valueOf(CAPACITY));
        addCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, side -> this);
        addCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, side -> this);
        addCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side -> automation);
    }

    // it only ever uses emc, nothing should be able to pull it back out
    @Override
    protected boolean canProvideEmc() {
        return false;
    }

    // pushed emc first covers the next tick's cost, anything past that goes into the buffer
    @Override
    public BigInteger insertEmcBigInteger(BigInteger toAccept, EmcAction action) {
        if (toAccept.signum() <= 0) return super.insertEmcBigInteger(toAccept, action);
        long wanted = Math.max(nextDue - incoming, 0);
        long taken = toAccept.min(BigInteger.valueOf(wanted)).longValue();
        BigInteger buffered = toAccept.subtract(BigInteger.valueOf(taken)).min(getMaximumEmcBigInteger().subtract(getStoredEmcBigInteger()).max(BigInteger.ZERO));
        if (action.execute() && buffered.signum() > 0) forceInsertEmcBigInteger(buffered, action);
        if (action.execute() && taken > 0) {
            incoming += taken;
            setChanged();
        }
        return buffered.add(BigInteger.valueOf(taken));
    }

    @Override
    public BigInteger getNeededEmcBigInteger() {
        return super.getNeededEmcBigInteger().add(BigInteger.valueOf(Math.max(nextDue - incoming, 0)));
    }

    public IItemHandlerModifiable getItems() {
        return items;
    }

    public int getProgress() {
        return progress;
    }

    public Optional<RefiningRecipe> getRecipe() {
        ItemStack input = items.getStackInSlot(INPUT_SLOT);
        if (level == null || input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeTypes.REFINING.get(), new Inventory(input), level);
    }

    @Override
    protected void serverTick() {
        if (level != null) tickServer(level);
    }

    private void tickServer(World level) {
        int before = progress;
        refine(level);
        setLit(level, progress != before);
        // leftovers and the star top the buffer back up
        if (incoming > 0) incoming -= forceInsertEmc(incoming, EmcAction.EXECUTE);
        long fromStar = drainStar(getMaximumEmc() - getStoredEmc(), true);
        if (fromStar > 0) forceInsertEmc(fromStar, EmcAction.EXECUTE);
        long due = getRecipe().filter(recipe -> canOutput(recipe, level))
                .map(recipe -> recipe.emcForTick(recipe.getId().equals(progressRecipe) ? progress : 0)).orElse(0L);
        if (due != nextDue) {
            nextDue = due;
            setChanged();
        }
    }

    private void setLit(World level, boolean lit) {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockEMCRefinery.LIT) && state.getValue(BlockEMCRefinery.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(BlockEMCRefinery.LIT, lit), 3);
        }
    }

    private void refine(World level) {
        Optional<RefiningRecipe> found = getRecipe();
        if (!found.isPresent() || !canOutput(found.get(), level)) {
            if (progress != 0) {
                progress = 0;
                progressRecipe = null;
                markDirty();
            }
            return;
        }
        RefiningRecipe recipe = found.get();
        // switching to a different input starts over
        if (!recipe.getId().equals(progressRecipe)) {
            progress = 0;
            progressRecipe = recipe.getId();
        }
        if (!pay(recipe.emcForTick(progress))) return;
        progress++;
        if (progress >= recipe.time()) {
            items.extractItem(INPUT_SLOT, recipe.count(), false);
            items.insertItem(OUTPUT_SLOT, recipe.result().copy(), false);
            progress = 0;
            progressRecipe = null;
        }
        markDirty();
    }

    private boolean canOutput(RefiningRecipe recipe, World level) {
        return items.insertItem(OUTPUT_SLOT, recipe.result().copy(), true).isEmpty();
    }

    // incoming emc first, then the star, and only the buffer once both of those run dry
    private boolean pay(long due) {
        long fromIncoming = Math.min(incoming, due);
        long fromStar = drainStar(due - fromIncoming, false);
        if (fromIncoming + fromStar + getStoredEmc() < due) return false;
        incoming -= fromIncoming;
        drainStar(fromStar, true);
        forceExtractEmc(due - fromIncoming - fromStar, EmcAction.EXECUTE);
        return true;
    }

    private long drainStar(long amount, boolean execute) {
        ItemStack star = items.getStackInSlot(STAR_SLOT);
        if (star.isEmpty() || amount <= 0) return 0;
        IItemEmcHolder holder = star.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).orElse(null);
        if (holder == null) return 0;
        return holder.extractEmc(star, amount, execute ? EmcAction.EXECUTE : EmcAction.SIMULATE);
    }

    public static boolean isStar(ItemStack stack) {
        return stack.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).isPresent();
    }

    public boolean canRefine(ItemStack stack) {
        return level != null && !stack.isEmpty() && level.getRecipeManager().getRecipeFor(RecipeTypes.REFINING.get(), new Inventory(ItemHandlerHelper.copyStackWithSize(stack, stack.getMaxStackSize())), level).isPresent();
    }

    // inputs and stars go in, only the results come out
    private class Automation implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) return stack;
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            switch (slot) {
                case INPUT_SLOT: return canRefine(stack);
                case STAR_SLOT: return isStar(stack);
                default: return false;
            }
        }
    }

    /********
     * Menu *
     ********/

    @Override
    public ITextComponent getDisplayName() {
        return Lang.Blocks.EMC_REFINERY.translate();
    }

    @Override
    public @Nullable Container createMenu(int id, PlayerInventory inventory, PlayerEntity player) {
        return new ContainerEMCRefinery(id, inventory, this);
    }

    /********
     * Data *
     ********/

    @Override
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.put(TagNames.ITEMS, items.serializeNBT());
        tag.putInt(TagNames.PROGRESS, progress);
        tag.putLong(TagNames.INCOMING, incoming);
        if (progressRecipe != null) tag.putString(TagNames.RECIPE, progressRecipe.toString());
    }

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        items.deserializeNBT(tag.getCompound(TagNames.ITEMS));
        progress = tag.getInt(TagNames.PROGRESS);
        incoming = tag.getLong(TagNames.INCOMING);
        progressRecipe = tag.contains(TagNames.RECIPE) ? ResourceLocation.tryParse(tag.getString(TagNames.RECIPE)) : null;
    }
}
