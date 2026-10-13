package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.IEmcStorageBigInteger;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import cool.furry.mc.projectexpansion.util.storage.StorageSide;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.ComparatorTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.waitUntil;

@IntegrationTestClass("empty")
public class EmcStorageTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static BlockEntityEmcStorage place(IntegrationTestHelper helper, EmcStorageTier tier) {
        setBlock(helper, POS, Blocks.EMC_STORAGE.get(tier).get());
        return blockEntity(helper, POS, BlockEntityEmcStorage.class);
    }

    private static void setSide(IntegrationTestHelper helper, Direction side, StorageSide mode) {
        helper.setBlockState(POS, helper.getBlockState(POS).setValue(BlockEmcStorage.SIDES.get(side), mode));
    }

    private static IEmcStorageBigInteger side(IntegrationTestHelper helper, Direction side) {
        return blockEntity(helper, POS, BlockEntityEmcStorage.class).getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, side).resolve().orElse(null);
    }

    private static BigInteger fill(BlockEntityEmcStorage storage, long amount) {
        return storage.insertEmcBigInteger(BigInteger.valueOf(amount), IEmcStorage.EmcAction.EXECUTE);
    }

    private static BigInteger stored(IntegrationTestHelper helper) {
        return blockEntity(helper, POS, BlockEntityEmcStorage.class).getStoredEmcBigInteger();
    }

    @IntegrationTest("empty")
    public void fillsFromCollector(IntegrationTestHelper helper) {
        place(helper, EmcStorageTier.BASIC);
        setBlock(helper, POS.east(), Objects.requireNonNull(Matter.FINAL.getCollector()));
        waitUntil(helper, () -> check(stored(helper).signum() > 0, "The storage didn't take any EMC from the collector"));
    }

    @IntegrationTest("empty")
    public void sidesFollowTheirMode(IntegrationTestHelper helper) {
        fill(place(helper, EmcStorageTier.BASIC), 1000);
        setSide(helper, Direction.EAST, StorageSide.OUTPUT);
        setSide(helper, Direction.WEST, StorageSide.OFF);

        IEmcStorageBigInteger input = side(helper, Direction.NORTH), output = side(helper, Direction.EAST);
        check(input != null && output != null, "An input or output side has no emc storage");
        check(side(helper, Direction.WEST) == null, "An off side still has an emc storage");
        check(input.extractEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).signum() == 0, "EMC was taken out of an input side");
        check(input.insertEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).equals(BigInteger.TEN), "An input side didn't take EMC");
        check(output.insertEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).signum() == 0, "EMC was put in through an output side");
        check(output.extractEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).equals(BigInteger.TEN), "EMC couldn't be taken out of an output side");
        check(stored(helper).equals(BigInteger.valueOf(1000)), "The storage has " + stored(helper) + " EMC instead of 1000");
    }

    @IntegrationTest("empty")
    public void wrenchCyclesSides(IntegrationTestHelper helper) {
        place(helper, EmcStorageTier.BASIC);
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            player.setItemInHand(Hand.MAIN_HAND, new ItemStack(Items.WRENCH.get()));
            BlockPos pos = absolute(helper, POS);
            BlockRayTraceResult hit = new BlockRayTraceResult(Vector3d.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
            List<StorageSide> seen = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                helper.getWorld().getBlockState(pos).use(helper.getWorld(), player, Hand.MAIN_HAND, hit);
                seen.add(BlockEmcStorage.getSide(helper.getBlockState(POS), Direction.UP));
            }
            check(seen.equals(Arrays.asList(StorageSide.OUTPUT, StorageSide.OFF, StorageSide.INPUT)), "The wrench went through " + seen + " instead of output, off, input");
            check(BlockEmcStorage.getSide(helper.getBlockState(POS), Direction.NORTH) == StorageSide.INPUT, "Using the wrench on the top changed another side");
            player.setShiftKeyDown(true);
            helper.getWorld().getBlockState(pos).use(helper.getWorld(), player, Hand.MAIN_HAND, hit);
            check(BlockEmcStorage.getSide(helper.getBlockState(POS), Direction.UP) == StorageSide.OFF, "Sneaking with the wrench didn't go back a mode");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void capacityIsLimited(IntegrationTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        BigInteger capacity = EmcStorageTier.BASIC.getCapacity();
        BigInteger taken = storage.insertEmcBigInteger(capacity.add(BigInteger.valueOf(500)), IEmcStorage.EmcAction.EXECUTE);
        check(taken.equals(capacity), "The storage took " + taken + " EMC with room for " + capacity);
        check(storage.getStoredEmcBigInteger().equals(capacity), "The storage holds " + storage.getStoredEmcBigInteger() + " EMC instead of " + capacity);
    }

    @IntegrationTest("empty")
    public void tiersHoldMore(IntegrationTestHelper helper) {
        for (EmcStorageTier tier : EmcStorageTier.VALUES) {
            if (tier.ordinal() == 0) continue;
            EmcStorageTier previous = EmcStorageTier.VALUES[tier.ordinal() - 1];
            check(tier.getCapacity().compareTo(previous.getCapacity()) > 0, tier + " storage doesn't hold more than " + previous);
            check(tier.getRate().compareTo(previous.getRate()) > 0, tier + " storage doesn't output more than " + previous);
        }
    }

    @IntegrationTest("empty")
    public void infinityIsUnlimited(IntegrationTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.INFINITY);
        BigInteger huge = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1000));
        check(storage.insertEmcBigInteger(huge, IEmcStorage.EmcAction.EXECUTE).equals(huge), "Infinity storage didn't take " + huge + " EMC");
        check(storage.insertEmcBigInteger(huge, IEmcStorage.EmcAction.EXECUTE).equals(huge), "Infinity storage filled up");
    }

    @IntegrationTest("empty")
    public void outputSidePushes(IntegrationTestHelper helper) {
        fill(place(helper, EmcStorageTier.BASIC), 100_000);
        setBlock(helper, POS.east(), Objects.requireNonNull(Matter.BASIC.getRelay()));
        setSide(helper, Direction.EAST, StorageSide.OUTPUT);
        waitUntil(helper, () -> {
            check(stored(helper).compareTo(BigInteger.valueOf(100_000)) < 0, "The output side didn't send any EMC");
            check(blockEntity(helper, POS.east(), BlockEntityRelay.class).getStoredEmcBigInteger().signum() > 0, "The relay didn't get any EMC");
        });
    }

    @IntegrationTest("empty")
    public void outputIsLimitedToTheRate(IntegrationTestHelper helper) {
        fill(place(helper, EmcStorageTier.BASIC), 1_000_000);
        setBlock(helper, POS.east(), Blocks.EMC_STORAGE.get(EmcStorageTier.INFINITY).get());
        setSide(helper, Direction.EAST, StorageSide.OUTPUT);
        long rate = EmcStorageTier.BASIC.getRate().longValueExact(), start = helper.getWorld().getGameTime();
        BigInteger before = stored(helper);
        helper.runAfter(30, () -> {
            long ticks = helper.getWorld().getGameTime() - start;
            long sent = before.subtract(stored(helper)).longValueExact();
            check(sent > 0, "The output side didn't send any EMC");
            check(sent <= rate * ticks, "The storage sent " + sent + " EMC in " + ticks + " ticks, more than its rate of " + rate);
        });
    }

    @IntegrationTest("empty")
    public void inputSidesDontPush(IntegrationTestHelper helper) {
        fill(place(helper, EmcStorageTier.BASIC), 1000);
        setBlock(helper, POS.east(), Objects.requireNonNull(Matter.BASIC.getRelay()));
        helper.runAfter(40, () -> check(stored(helper).equals(BigInteger.valueOf(1000)), "EMC left through an input side"));
    }

    @IntegrationTest("empty")
    public void keepsEmcWhenBroken(IntegrationTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        fill(storage, 12_345);
        BlockPos pos = absolute(helper, POS);
        List<ItemStack> drops = Block.getDrops(helper.getWorld().getBlockState(pos), helper.getWorld(), pos, storage);
        ItemStack drop = drops.stream().filter(stack -> stack.getItem() == Items.EMC_STORAGE.get(EmcStorageTier.BASIC).get()).findFirst().orElse(ItemStack.EMPTY);
        check(!drop.isEmpty(), "The storage dropped " + drops);
        check(BlockEmcStorage.getStoredEmc(drop).equals(BigInteger.valueOf(12_345)), "The dropped storage has " + BlockEmcStorage.getStoredEmc(drop) + " EMC instead of 12345");

        setBlock(helper, POS.east(), Blocks.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        BlockItem.updateCustomBlockEntityTag(helper.getWorld(), null, absolute(helper, POS.east()), drop);
        BigInteger placed = blockEntity(helper, POS.east(), BlockEntityEmcStorage.class).getStoredEmcBigInteger();
        check(placed.equals(BigInteger.valueOf(12_345)), "The placed storage has " + placed + " EMC instead of 12345");
    }

    @IntegrationTest("empty")
    public void upgradeKeepsEmc(IntegrationTestHelper helper) {
        IRecipe<?> found = helper.getWorld().getRecipeManager().byKey(Main.rl("emc_storage/improved")).orElse(null);
        check(found instanceof ICraftingRecipe, "The improved storage recipe didn't load");
        ICraftingRecipe recipe = (ICraftingRecipe) found;
        ItemStack basic = new ItemStack(Items.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        BlockEmcStorage.setStoredEmc(basic, BigInteger.valueOf(54_321));
        // a crafting grid needs a menu to tell about changes
        CraftingInventory grid = new CraftingInventory(new Container(null, -1) {
            @Override
            public boolean stillValid(PlayerEntity player) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < 9; i++) {
            grid.setItem(i, i == 4 ? basic : i % 2 == 0 ? new ItemStack(Items.IANTHIUM_INGOT.get()) : new ItemStack(PEItems.RED_MATTER.get()));
        }
        check(recipe.matches(grid, helper.getWorld()), "The improved storage recipe doesn't match its own pattern");
        ItemStack result = recipe.assemble(grid);
        check(result.getItem() == Items.EMC_STORAGE.get(EmcStorageTier.IMPROVED).get(), "The recipe made " + result);
        check(BlockEmcStorage.getStoredEmc(result).equals(BigInteger.valueOf(54_321)), "The improved storage has " + BlockEmcStorage.getStoredEmc(result) + " EMC instead of 54321");
    }

    @IntegrationTest("empty")
    public void comparatorShowsFill(IntegrationTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        check(storage.getComparatorSignal() == 0, "An empty storage gives a signal of " + storage.getComparatorSignal());
        fill(storage, 1);
        check(storage.getComparatorSignal() == 1, "A storage with a little EMC gives a signal of " + storage.getComparatorSignal());
        storage.insertEmcBigInteger(EmcStorageTier.BASIC.getCapacity(), IEmcStorage.EmcAction.EXECUTE);
        check(storage.getComparatorSignal() == 15, "A full storage gives a signal of " + storage.getComparatorSignal());
    }

    // a comparator only reads again when it's told the storage changed
    @IntegrationTest("empty")
    public void comparatorFollowsEmc(IntegrationTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        setBlock(helper, POS.east().below(), net.minecraft.block.Blocks.STONE);
        BlockState comparator = net.minecraft.block.Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST);
        helper.setBlockState(POS.east(), comparator);
        storage.insertEmcBigInteger(EmcStorageTier.BASIC.getCapacity(), IEmcStorage.EmcAction.EXECUTE);
        waitUntil(helper, () -> {
            int signal = blockEntity(helper, POS.east(), ComparatorTileEntity.class).getOutputSignal();
            check(signal == 15, "The comparator next to a full storage gives a signal of " + signal);
        });
    }

    @IntegrationTest("empty")
    public void tiersHaveIncreasingEmc(IntegrationTestHelper helper) {
        long previous = 0;
        for (EmcStorageTier tier : EmcStorageTier.VALUES) {
            long value = emcValue(Items.EMC_STORAGE.get(tier).get());
            check(value > previous, tier + " storage is worth " + value + " EMC, which isn't more than the tier before it (" + previous + ")");
            previous = value;
        }
    }
}
