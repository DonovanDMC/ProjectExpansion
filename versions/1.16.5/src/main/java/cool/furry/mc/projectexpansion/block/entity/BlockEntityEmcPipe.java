package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.util.TagNames;
import cool.furry.mc.projectexpansion.util.pipe.Distribution;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import cool.furry.mc.projectexpansion.util.pipe.PipeSide;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class BlockEntityEmcPipe extends BlockEntityBase {
    private static final Direction[] DIRECTIONS = Direction.values();
    // bumped whenever a pipe network might have changed anywhere, every pipe rebuilds its destinations when it next needs them
    private static long networkRevision;

    private final boolean[] disconnected = new boolean[DIRECTIONS.length];
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(DIRECTIONS.length, ItemStack.EMPTY);
    private final int[] roundRobin = new int[DIRECTIONS.length];
    // the rate is per side per tick, shared between pulling and things pushing into the pipe
    private final long[] moved = new long[DIRECTIONS.length];
    private long movedTick = -1;
    private final Input[] inputs = new Input[DIRECTIONS.length];
    @Nullable
    private List<Destination> destinations;
    private long destinationsRevision = -1;

    public BlockEntityEmcPipe() {
        super(BlockEntityTypes.EMC_PIPE.get());
        addCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, this::getInput);
    }

    public static @Nullable IEmcStorage getEmcStorage(World level, BlockPos pos, @Nullable Direction side) {
        TileEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null : blockEntity.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, side).orElse(null);
    }

    public static void invalidateNetworks() {
        networkRevision++;
    }

    /*********
     * Sides *
     *********/

    public PipeSide getSide(Direction side) {
        return getBlockState().getValue(BlockEmcPipe.SIDES.get(side));
    }

    public boolean isDisconnected(Direction side) {
        return disconnected[side.get3DDataValue()];
    }

    public void setDisconnected(Direction side, boolean value) {
        disconnected[side.get3DDataValue()] = value;
        setChanged();
    }

    public ItemStack getUpgradeStack(Direction side) {
        return upgrades.get(side.get3DDataValue());
    }

    public void setUpgradeStack(Direction side, ItemStack stack) {
        upgrades.set(side.get3DDataValue(), stack);
        setChanged();
    }

    public @Nullable PipeUpgrade getUpgrade(Direction side) {
        return PipeUpgrade.fromStack(getUpgradeStack(side));
    }

    // what's stored on the upgrade, even if the upgrade doesn't unlock it
    public PipeSettings getStoredSettings(Direction side) {
        return PipeSettings.get(getUpgradeStack(side));
    }

    public void setStoredSettings(Direction side, PipeSettings settings) {
        ItemStack stack = getUpgradeStack(side);
        if (stack.isEmpty()) return;
        PipeSettings.set(stack, settings);
        setChanged();
    }

    public PipeSettings getSettings(Direction side) {
        return getStoredSettings(side).effective(getUpgrade(side));
    }

    public long getRate(Direction side) {
        return PipeUpgrade.getRate(getUpgrade(side));
    }

    public boolean shouldWork(Direction side) {
        return level != null && getSettings(side).redstoneMode().allows(level.hasNeighborSignal(worldPosition));
    }

    public NonNullList<ItemStack> getUpgrades() {
        return upgrades;
    }

    /*********
     * Moving *
     *********/

    private long remaining(Direction side) {
        if (level == null) return 0;
        if (level.getGameTime() != movedTick) {
            movedTick = level.getGameTime();
            Arrays.fill(moved, 0);
        }
        long rate = getRate(side);
        return rate == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(rate - moved[side.get3DDataValue()], 0);
    }

    private void use(Direction side, long amount) {
        int i = side.get3DDataValue();
        moved[i] = moved[i] > Long.MAX_VALUE - amount ? Long.MAX_VALUE : moved[i] + amount;
    }

    @Override
    protected void serverTick() {
        if (level != null) tickServer(level, worldPosition, getBlockState());
    }

    private void tickServer(World level, BlockPos pos, BlockState state) {
        for (Direction side : DIRECTIONS) {
            if (state.getValue(BlockEmcPipe.SIDES.get(side)) != PipeSide.EXTRACTING || !shouldWork(side)) continue;
            IEmcStorage source = getEmcStorage(level, pos.relative(side), side.getOpposite());
            if (source == null) continue;
            long budget = remaining(side);
            if (budget <= 0) continue;
            long available = source.extractEmc(budget, IEmcStorage.EmcAction.SIMULATE);
            if (available <= 0) continue;
            long sent = distribute(side, available, source, IEmcStorage.EmcAction.EXECUTE);
            if (sent <= 0) continue;
            source.extractEmc(sent, IEmcStorage.EmcAction.EXECUTE);
            use(side, sent);
        }
    }

    // hands emc from the extracting side to the network, returns how much was taken
    private long distribute(Direction from, long amount, @Nullable IEmcStorage source, IEmcStorage.EmcAction action) {
        if (level == null || amount <= 0) return 0;
        BlockPos sourcePos = worldPosition.relative(from);
        List<IEmcStorage> targets = new ArrayList<>();
        for (Destination destination : getDestinations()) {
            if (destination.target().equals(sourcePos)) continue;
            IEmcStorage storage = getEmcStorage(level, destination.target(), destination.side().getOpposite());
            if (storage == null || storage == source || targets.contains(storage)) continue;
            targets.add(storage);
        }
        if (targets.isEmpty()) return 0;

        Distribution distribution = getSettings(from).distribution();
        if (distribution == Distribution.FURTHEST) Collections.reverse(targets);
        else if (distribution == Distribution.RANDOM) Collections.shuffle(targets, new Random(level.random.nextLong()));
        else if (distribution == Distribution.ROUND_ROBIN) return distributeEvenly(from, targets, amount, action);

        long left = amount;
        for (IEmcStorage target : targets) {
            if (left <= 0) break;
            left -= Math.max(target.insertEmc(left, action), 0);
        }
        return amount - left;
    }

    private long distributeEvenly(Direction from, List<IEmcStorage> targets, long amount, IEmcStorage.EmcAction action) {
        int count = targets.size();
        int start = Math.floorMod(roundRobin[from.get3DDataValue()], count);
        long share = Math.max(amount / count, 1);
        long left = amount;
        // an even share each, then anything they couldn't take goes to whoever still has room
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (int i = 0; i < count && left > 0; i++) {
                IEmcStorage target = targets.get((start + i) % count);
                left -= Math.max(target.insertEmc(pass == 0 ? Math.min(share, left) : left, action), 0);
            }
        }
        if (action.execute()) roundRobin[from.get3DDataValue()] = (start + 1) % count;
        return amount - left;
    }

    /***************
     * Destinations *
     ***************/

    // a block the network can send to, through side of the pipe at pos
    public static class Destination {
        private final BlockPos pos;
        private final Direction side;
        private final int distance;

        public Destination(BlockPos pos, Direction side, int distance) {
            this.pos = pos;
            this.side = side;
            this.distance = distance;
        }

        public Direction side() {
            return side;
        }

        public int distance() {
            return distance;
        }

        public BlockPos target() {
            return pos.relative(side);
        }
    }

    private List<Destination> getDestinations() {
        if (destinations == null || destinationsRevision != networkRevision) {
            destinations = findDestinations();
            destinationsRevision = networkRevision;
        }
        return destinations;
    }

    private List<Destination> findDestinations() {
        List<Destination> found = new ArrayList<>();
        if (level == null) return found;
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Queue<Integer> distances = new ArrayDeque<>();
        seen.add(worldPosition);
        queue.add(worldPosition);
        distances.add(0);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int distance = distances.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockEmcPipe)) continue;
            for (Direction side : DIRECTIONS) {
                PipeSide pipeSide = state.getValue(BlockEmcPipe.SIDES.get(side));
                if (!pipeSide.isConnected()) continue;
                BlockPos next = pos.relative(side);
                if (!level.isLoaded(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof BlockEmcPipe) {
                    if (seen.add(next)) {
                        queue.add(next);
                        distances.add(distance + 1);
                    }
                } else if (pipeSide == PipeSide.CONNECTED) {
                    // extracting sides only ever give
                    found.add(new Destination(pos, side, distance + 1));
                }
            }
        }
        found.sort(Comparator.comparingInt(Destination::distance));
        return found;
    }

    /*********
     * Input *
     *********/

    private @Nullable IEmcStorage getInput(@Nullable Direction side) {
        if (side == null || getSide(side) != PipeSide.EXTRACTING) return null;
        int i = side.get3DDataValue();
        if (inputs[i] == null) inputs[i] = new Input(side);
        return inputs[i];
    }

    // lets things like collectors push into an extracting side, same as Pipez's energy pipes
    private class Input implements IEmcStorage {
        private final Direction side;

        private Input(Direction side) {
            this.side = side;
        }

        @Override
        public long getStoredEmc() {
            return 0;
        }

        @Override
        public long getMaximumEmc() {
            return remaining(side);
        }

        @Override
        public long extractEmc(long toExtract, EmcAction action) {
            return 0;
        }

        @Override
        public long insertEmc(long toAccept, EmcAction action) {
            if (level == null || toAccept <= 0 || getSide(side) != PipeSide.EXTRACTING || !shouldWork(side)) return 0;
            long amount = Math.min(toAccept, remaining(side));
            IEmcStorage pusher = getEmcStorage(level, worldPosition.relative(side), side.getOpposite());
            long sent = distribute(side, amount, pusher, action);
            if (action.execute()) use(side, sent);
            return sent;
        }
    }

    /*************
     * Upgrades *
     *************/

    // the upgrade slot of one side, for the menu
    public IInventory getUpgradeContainer(Direction side) {
        return new IInventory() {
            @Override
            public int getContainerSize() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return getUpgradeStack(side).isEmpty();
            }

            @Override
            public ItemStack getItem(int slot) {
                return getUpgradeStack(side);
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                ItemStack stack = getUpgradeStack(side);
                if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
                ItemStack taken = stack.split(amount);
                setChanged();
                return taken;
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                ItemStack stack = getUpgradeStack(side);
                setUpgradeStack(side, ItemStack.EMPTY);
                return stack;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                setUpgradeStack(side, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return PipeUpgrade.fromStack(stack) != null;
            }

            @Override
            public void setChanged() {
                BlockEntityEmcPipe.this.setChanged();
            }

            @Override
            public boolean stillValid(PlayerEntity player) {
                return !isRemoved() && getSide(side) == PipeSide.EXTRACTING && level != null && level.getBlockEntity(worldPosition) == BlockEntityEmcPipe.this && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
            }

            @Override
            public void clearContent() {
                setUpgradeStack(side, ItemStack.EMPTY);
            }
        };
    }

    /********
     * Data *
     ********/

    @Override
    protected void loadData(CompoundNBT tag) {
        byte[] flags = tag.getByteArray(TagNames.DISCONNECTED);
        for (int i = 0; i < disconnected.length; i++) disconnected[i] = i < flags.length && flags[i] != 0;
        upgrades.clear();
        ItemStackHelper.loadAllItems(tag, upgrades);
    }

    @Override
    protected void saveData(CompoundNBT tag) {
        byte[] flags = new byte[disconnected.length];
        for (int i = 0; i < disconnected.length; i++) flags[i] = (byte) (disconnected[i] ? 1 : 0);
        tag.putByteArray(TagNames.DISCONNECTED, flags);
        ItemStackHelper.saveAllItems(tag, upgrades);
    }
}
