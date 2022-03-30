package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;

/* Graciously borrowed from Mob Grinding Utils
 * https://github.com/vadis365/Mob-Grinding-Utils/blob/7ed8a22343e0ef0498941472c6eb5dc0ea329af3/MobGrindingUtils/MobGrindingUtils/src/main/java/mob_grinding_utils/tile/TileEntityInventoryHelper.java
 */

public abstract class TileEntityInventoryHelper extends TileEntity implements ISidedInventory {

    private NonNullList<ItemStack> inventory;

    public TileEntityInventoryHelper(TileEntityType<?> tileEntityTypeIn, int invtSize) {
        super(tileEntityTypeIn);
        inventory = NonNullList.<ItemStack>withSize(invtSize, ItemStack.EMPTY);
    }

    @Override
    public int getSizeInventory() {
        return inventory.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.get(slot);
    }

    protected NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack itemstack = ItemStackHelper.getAndSplit(inventory, index, count);
        if (!itemstack.isEmpty())
            this.markDirty();
        return itemstack;
    }

    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        inventory.set(index, stack);
        if (stack.getCount() > this.getInventoryStackLimit())
            stack.setCount(this.getInventoryStackLimit());
        this.markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : inventory) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        super.read(state, compound);
        this.loadFromNbt(compound);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        return this.saveToNbt(compound);
    }

    public void loadFromNbt(CompoundNBT compound) {
        inventory = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);
        if (compound.contains("Items", 9))
            ItemStackHelper.loadAllItems(compound, inventory);
    }

    public CompoundNBT saveToNbt(CompoundNBT compound) {
        ItemStackHelper.saveAllItems(compound, inventory, false);
        return compound;
    }

    @Override
    public void openInventory(PlayerEntity playerIn) {
    }

    @Override
    public void closeInventory(PlayerEntity playerIn) {
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        return false;
    }
}
