package cool.furry.mc.neoforge.projectexpansion.block.entity;

import com.mojang.serialization.DataResult;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerCondenserMK3Input;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerCondenserMK3Output;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.util.IChestLike;
import cool.furry.mc.neoforge.projectexpansion.util.TagNames;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.event.PlayerAttemptCondenserSetEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.block_entities.WrappedItemHandler;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class BlockEntityCondenserMK3 extends BlockEntityBase implements IChestLike, MenuProvider {
    public static final ICapabilityProvider<BlockEntityCondenserMK3, @Nullable Direction, IItemHandler> ITEM_HANDLER_CAPABILITY = BlockEntityCondenserMK3::getAutomationSidedItemHandler;
    public static final ICapabilityProvider<BlockEntityCondenserMK3, @Nullable Direction, IEmcStorage> EMC_STORAGE_PROVIDER = BlockEntityCondenserMK3::getSidedHandler;
    public static final Direction OUTPUT_DIRECTION = Direction.DOWN;
    private static final int INPUT_SIZE = 91;
    private static final int OUTPUT_SIZE = 180;
    private final HashMap<Direction, SidedHandler> handlers = new HashMap<>();
    private final ChestLidController lidController = new ChestLidController();

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F,
                    level.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F,
                    level.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int openCount) {
            level.blockEvent(pos, state.getBlock(), 1, openCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ContainerCondenserMK3Input input && input.blockEntityMatches(BlockEntityCondenserMK3.this) ||
                    player.containerMenu instanceof ContainerCondenserMK3Output output && output.blockEntityMatches(BlockEntityCondenserMK3.this);
        }
    };

    public BlockEntityCondenserMK3(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.CONDENSER_MK3.get(), pos, state);
        for (Direction dir : Direction.values()) {
            handlers.put(dir, createSidedHandler(dir));
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityTypes.CONDENSER_MK3.get(), ITEM_HANDLER_CAPABILITY);
        event.registerBlockEntity(PECapabilities.EMC_STORAGE_CAPABILITY, BlockEntityTypes.CONDENSER_MK3.get(), EMC_STORAGE_PROVIDER);
    }

    protected SidedHandler createSidedHandler(Direction direction) {
        boolean isOutput = direction == OUTPUT_DIRECTION;
        StackHandler inventory = new StackHandler(isOutput ? OUTPUT_SIZE : INPUT_SIZE);
        return new SidedHandler(direction, inventory, isOutput);
    }


    public SidedHandler getSidedHandler(@Nullable Direction direction) {
        if (direction == null) direction = Direction.UP;
        SidedHandler handler = handlers.get(direction);
        if (handler == null) {
            handler = createSidedHandler(direction);
            this.handlers.put(direction, handler);
        }

        return handler;
    }

    public SidedHandler getOutput() {
        return getSidedHandler(OUTPUT_DIRECTION);
    }

    protected StackHandler getOutputHandler() {
        return getOutput().getInventory();
    }

    public WrappedItemHandler getAutomationSidedItemHandler(@Nullable Direction direction) {
        if (direction == null) direction = Direction.UP;

        SidedHandler handler = getSidedHandler(direction);
        return handler.getAutomationInventory();
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityCondenserMK3 be) be.tickServer(level, pos, state);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            if (direction == OUTPUT_DIRECTION) {
                continue;
            }

            SidedHandler handler = getSidedHandler(direction);
            handler.checkLockAndUpdate(false);
            handler.displayEmc = handler.getStoredEmc();
            if (handler.getLockInfo() != null) {
                handler.condense();
            }
        }
        updateComparators(level, pos);
    }



    @SuppressWarnings("unused")
    public static void tickClient(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if(blockEntity instanceof BlockEntityCondenserMK3 be) {
            be.lidController.tickLid();
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            lidController.shouldBeOpen(type > 0);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public void startOpen(Player player) {
        if (!isRemoved() && !player.isSpectator() && level != null) {
            openersCounter.incrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    public void stopOpen(Player player) {
        if (!isRemoved() && !player.isSpectator() && level != null) {
            openersCounter.decrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    public void recheckOpen() {
        if (!isRemoved() && level != null) {
            openersCounter.recheckOpeners(level, getBlockPos(), getBlockState());
        }
    }

    @Override
    public float getOpenNess(float partialTicks) {
        return lidController.getOpenness(partialTicks);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (Direction dir : Direction.values()) {
            SidedHandler handler = handlers.get(dir);
            CompoundTag t = tag.getCompound(Util.ucwords(dir.getName()));
            handler.load(t, registries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (Direction dir : Direction.values()) {
            SidedHandler handler = handlers.get(dir);
            CompoundTag t = new CompoundTag();
            handler.save(t, registries);
            tag.put(Util.ucwords(dir.getName()), t);

        }
    }

    @Override
    public Component getDisplayName() {
        return TextComponentUtil.build(PEBlocks.CONDENSER);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        @Nullable Direction direction = null;
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHitResult) {
            direction = blockHitResult.getDirection();
        }

        if (direction == OUTPUT_DIRECTION) {
            return new ContainerCondenserMK3Output(windowId, inventory, this);
        }

        return new ContainerCondenserMK3Input(windowId, inventory, this);
    }

    public class SidedHandler implements IEmcStorage {
        private final Direction direction;
        private final StackHandler inventory;
        private final WrappedItemHandler automationInventory;
        private final boolean isOutput;
        private @Nullable ItemInfo lockInfo = null;
        private boolean isAcceptingEmc;
        //Start at one less than actual just to ensure we run initially after loading
        private int loadIndex = EMCMappingHandler.getLoadIndex() - 1;
        private long emc;
        public long displayEmc;
        public long requiredEmc;

        protected SidedHandler(Direction direction, StackHandler inventory, boolean isOutput) {
            this.direction = direction;
            this.inventory = inventory;
            this.isOutput = isOutput;
            this.automationInventory = createAutomationInventory();
        }

        public StackHandler getInventory() {
            return inventory;
        }
        public WrappedItemHandler getAutomationInventory() {
            return automationInventory;
        }

        public void setLockInfoFromPacket(@Nullable ItemInfo lockInfo) {
            this.lockInfo = lockInfo;
        }

        public boolean attemptCondenserSet(Player player) {
            return level != null && attemptCondenserSet(level, worldPosition, player);
        }

        private boolean attemptCondenserSet(@NotNull Level level, @NotNull BlockPos pos, Player player) {
            if (level.isClientSide) {
                return false;
            }
            if (getLockInfo() == null) {
                ItemStack stack = player.containerMenu.getCarried();
                if (!stack.isEmpty()) {
                    ItemInfo sourceInfo = ItemInfo.fromStack(stack);
                    ItemInfo reducedInfo = IEMCProxy.INSTANCE.getPersistentInfo(sourceInfo);
                    if (!NeoForge.EVENT_BUS.post(new PlayerAttemptCondenserSetEvent(player, sourceInfo, reducedInfo)).isCanceled()) {
                        lockInfo = reducedInfo;
                        checkLockAndUpdate(true);
                        markDirty(level, pos, false);
                        return true;
                    }
                    return false;
                }
                //If the lock item is actually null and the player didn't carry anything don't do anything
                // otherwise just fall through as we need to update it to actually being empty
                if (lockInfo == null) {
                    return false;
                }
            }
            lockInfo = null;
            checkLockAndUpdate(true);
            markDirty(level, pos, false);
            return true;
        }

        public @Nullable ItemInfo getLockInfo() {
            return lockInfo;
        }

        public boolean isStackEqualToLock(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            ItemInfo lockInfo = getLockInfo();
            if (lockInfo == null) {
                return false;
            }
            //Compare our lock to the persistent item that the stack would have
            return lockInfo.equals(IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack)));
        }

        protected WrappedItemHandler createAutomationInventory() {
            if (isOutput) {
                return new WrappedItemHandler(inventory, WrappedItemHandler.WriteMode.OUT);
            }

            return new WrappedItemHandler(inventory, WrappedItemHandler.WriteMode.IN) {
                @Override
                public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    return SlotPredicates.HAS_EMC.test(stack) && !isStackEqualToLock(stack) ? super.insertItem(slot, stack, simulate) : stack;
                }
            };
        }

        protected boolean emcAffectsComparators() {
            return true;
        }

        protected boolean hasSpace() {
            IItemHandler output = getOutputHandler();
            for (int i = 0, slots = output.getSlots(); i < slots; i++) {
                ItemStack stack = output.getStackInSlot(i);
                if (stack.isEmpty() || (isStackEqualToLock(stack) && stack.getCount() < stack.getMaxStackSize())) {
                    return true;
                }
            }
            return false;
        }

        protected final void pushStack() {
            ItemInfo lockInfo = getLockInfo();
            IItemHandler output = getOutputHandler();
            if (lockInfo != null) {
                ItemHandlerHelper.insertItemStacked(output, lockInfo.createStack(), false);
            }
        }

        public void condense() {
            while (this.hasSpace() && this.getStoredEmc() >= requiredEmc) {
                pushStack();
                forceExtractEmc(requiredEmc, EmcAction.EXECUTE);
            }
            if (this.hasSpace()) {
                StackHandler input = getInventory();
                for (int i = 0, slots = input.getSlots(); i < slots; i++) {
                    ItemStack stack = input.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        forceInsertEmc(IEMCProxy.INSTANCE.getSellValue(stack) * stack.getCount(), EmcAction.EXECUTE);
                        input.setStackInSlot(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }
        }

        @Override
        public long getStoredEmc() {
            return emc;
        }

        @Override
        public long getMaximumEmc() {
            return Long.MAX_VALUE;
        }

        public boolean canProvideEmc() {
            return false;
        }

        public boolean canAcceptEmc() {
            return isAcceptingEmc;
        }

        @Override
        public long extractEmc(long toExtract, EmcAction action) {
            if (toExtract < 0) {
                return insertEmc(-toExtract, action);
            }
            if (canProvideEmc()) {
                return forceExtractEmc(Math.min(getStoredEmc(), toExtract), action);
            }
            return 0;
        }

        @Override
        public long insertEmc(long toAccept, EmcAction action) {
            if (toAccept < 0) {
                return extractEmc(-toAccept, action);
            }
            if (canAcceptEmc()) {
                return forceInsertEmc(Math.min(getNeededEmc(), toAccept), action);
            }
            return 0;
        }

        protected long forceExtractEmc(long toExtract, EmcAction action) {
            if (toExtract < 0) {
                return forceInsertEmc(-toExtract, action);
            }
            long toRemove = Math.min(getStoredEmc(), toExtract);
            if (action.execute()) {
                emc -= toRemove;
                storedEmcChanged();
            }
            return toRemove;
        }

        protected long forceInsertEmc(long toAccept, EmcAction action) {
            if (toAccept < 0) {
                return forceExtractEmc(-toAccept, action);
            }
            long toAdd = Math.min(getNeededEmc(), toAccept);
            if (action.execute()) {
                emc += toAdd;
                storedEmcChanged();
            }
            return toAdd;
        }


        protected void storedEmcChanged() {
            if (level != null) {
                markDirty(level, worldPosition, emcAffectsComparators());
            }
        }

        private void checkLockAndUpdate(boolean force) {
            if (!force && loadIndex == EMCMappingHandler.getLoadIndex()) {
                // Only update if we are forcing it or are on a different load index
                return;
            }
            loadIndex = EMCMappingHandler.getLoadIndex();
            ItemInfo lockInfo = getLockInfo();
            if (lockInfo != null) {
                long lockEmc = IEMCProxy.INSTANCE.getValue(lockInfo);
                if (lockEmc > 0) {
                    if (requiredEmc != lockEmc) {
                        requiredEmc = lockEmc;
                        isAcceptingEmc = true;
                    }
                    return;
                }
                //Don't reset the lockInfo just because it has no EMC, as if a reload makes it have EMC again
                // then we want to allow it to happen again
            }
            displayEmc = 0;
            requiredEmc = 0;
            isAcceptingEmc = false;
        }

        public void load(CompoundTag tag, HolderLookup.Provider registries) {
            if (isOutput) {
                getInventory().deserializeNBT(registries, tag.getCompound(TagNames.OUTPUT));
            } else {
                getInventory().deserializeNBT(registries, tag.getCompound(TagNames.INPUT));
                if (tag.contains(TagNames.LOCK)) {
                    lockInfo = ItemInfo.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(TagNames.LOCK)).result().orElse(null);
                } else {
                    lockInfo = null;
                }
                emc = tag.getLong(TagNames.STORED_EMC);
            }
        }

        public void save(CompoundTag tag, HolderLookup.Provider registries) {
            if (isOutput) {
                tag.put(TagNames.OUTPUT, getInventory().serializeNBT(registries));
            } else {
                tag.put(TagNames.INPUT, getInventory().serializeNBT(registries));
                if (lockInfo != null) {
                    DataResult<Tag> result = ItemInfo.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), lockInfo);
                    if (result.isSuccess()) {
                        tag.put(TagNames.LOCK, result.getOrThrow());
                    }
                }
                tag.putLong(TagNames.STORED_EMC, emc);
            }
        }
    }
}
