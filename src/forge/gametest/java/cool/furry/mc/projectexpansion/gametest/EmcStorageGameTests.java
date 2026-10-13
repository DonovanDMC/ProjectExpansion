package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.gui.container.inventory.PXCraftingContainer;
import cool.furry.mc.projectexpansion.integrations.EmcServerData;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.IEmcStorageBigInteger;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import cool.furry.mc.projectexpansion.util.storage.StorageSide;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class EmcStorageGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static BlockEntityEmcStorage place(GameTestHelper helper, EmcStorageTier tier) {
        helper.setBlock(POS, Blocks.EMC_STORAGE.get(tier).get());
        return blockEntity(helper, POS, BlockEntityEmcStorage.class);
    }

    private static void setSide(GameTestHelper helper, Direction side, StorageSide mode) {
        BlockState state = helper.getBlockState(POS);
        helper.setBlock(POS, state.setValue(BlockEmcStorage.SIDES.get(side), mode));
    }

    private static IEmcStorageBigInteger side(GameTestHelper helper, Direction side) {
        return blockEntity(helper, POS, BlockEntityEmcStorage.class).getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, side).resolve().orElse(null);
    }

    private static BigInteger fill(BlockEntityEmcStorage storage, long amount) {
        return storage.insertEmcBigInteger(BigInteger.valueOf(amount), IEmcStorage.EmcAction.EXECUTE);
    }

    @GameTest(template = EMPTY)
    public static void fillsFromCollector(GameTestHelper helper) {
        place(helper, EmcStorageTier.BASIC);
        helper.setBlock(POS.east(), Objects.requireNonNull(Matter.FINAL.getCollector()));
        helper.succeedWhen(() -> check(blockEntity(helper, POS, BlockEntityEmcStorage.class).getStoredEmcBigInteger().signum() > 0, "The storage didn't take any EMC from the collector"));
    }

    @GameTest(template = EMPTY)
    public static void sidesFollowTheirMode(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        fill(storage, 1000);
        setSide(helper, Direction.EAST, StorageSide.OUTPUT);
        setSide(helper, Direction.WEST, StorageSide.OFF);

        IEmcStorageBigInteger input = side(helper, Direction.NORTH), output = side(helper, Direction.EAST);
        check(input != null && output != null, "An input or output side has no emc storage");
        check(side(helper, Direction.WEST) == null, "An off side still has an emc storage");
        check(input.extractEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).signum() == 0, "EMC was taken out of an input side");
        check(input.insertEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).equals(BigInteger.TEN), "An input side didn't take EMC");
        check(output.insertEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).signum() == 0, "EMC was put in through an output side");
        check(output.extractEmcBigInteger(BigInteger.TEN, IEmcStorage.EmcAction.EXECUTE).equals(BigInteger.TEN), "EMC couldn't be taken out of an output side");
        check(storage.getStoredEmcBigInteger().equals(BigInteger.valueOf(1000)), "The storage has " + storage.getStoredEmcBigInteger() + " EMC instead of 1000");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wrenchCyclesSides(GameTestHelper helper) {
        place(helper, EmcStorageTier.BASIC);
        ServerPlayer player = mockPlayer(helper);
        try {
            ItemStack wrench = new ItemStack(Items.WRENCH.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            BlockPos pos = helper.absolutePos(POS);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
            List<StorageSide> seen = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                helper.getLevel().getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
                seen.add(BlockEmcStorage.getSide(helper.getBlockState(POS), Direction.UP));
            }
            check(seen.equals(List.of(StorageSide.OUTPUT, StorageSide.OFF, StorageSide.INPUT)), "The wrench went through " + seen + " instead of output, off, input");
            check(BlockEmcStorage.getSide(helper.getBlockState(POS), Direction.NORTH) == StorageSide.INPUT, "Using the wrench on the top changed another side");
            player.setShiftKeyDown(true);
            helper.getLevel().getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            check(BlockEmcStorage.getSide(helper.getBlockState(POS), Direction.UP) == StorageSide.OFF, "Sneaking with the wrench didn't go back a mode");
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void capacityIsLimited(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        BigInteger capacity = EmcStorageTier.BASIC.getCapacity();
        BigInteger taken = storage.insertEmcBigInteger(capacity.add(BigInteger.valueOf(500)), IEmcStorage.EmcAction.EXECUTE);
        check(taken.equals(capacity), "The storage took " + taken + " EMC with room for " + capacity);
        check(storage.getStoredEmcBigInteger().equals(capacity), "The storage holds " + storage.getStoredEmcBigInteger() + " EMC instead of " + capacity);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tiersHoldMore(GameTestHelper helper) {
        for (EmcStorageTier tier : EmcStorageTier.VALUES) {
            if (tier.ordinal() == 0) continue;
            EmcStorageTier previous = EmcStorageTier.VALUES[tier.ordinal() - 1];
            check(tier.getCapacity().compareTo(previous.getCapacity()) > 0, tier + " storage doesn't hold more than " + previous);
            check(tier.getRate().compareTo(previous.getRate()) > 0, tier + " storage doesn't output more than " + previous);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void infinityIsUnlimited(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.INFINITY);
        BigInteger huge = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1000));
        check(storage.insertEmcBigInteger(huge, IEmcStorage.EmcAction.EXECUTE).equals(huge), "Infinity storage didn't take " + huge + " EMC");
        check(storage.insertEmcBigInteger(huge, IEmcStorage.EmcAction.EXECUTE).equals(huge), "Infinity storage filled up");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void outputSidePushes(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        fill(storage, 100_000);
        helper.setBlock(POS.east(), Objects.requireNonNull(Matter.BASIC.getRelay()));
        setSide(helper, Direction.EAST, StorageSide.OUTPUT);
        long rate = EmcStorageTier.BASIC.getRate().longValueExact();
        AtomicReference<BigInteger> before = new AtomicReference<>();
        helper.startSequence()
            .thenExecute(() -> before.set(blockEntity(helper, POS, BlockEntityEmcStorage.class).getStoredEmcBigInteger()))
            .thenIdle(1)
            .thenExecute(() -> {
                BigInteger sent = before.get().subtract(blockEntity(helper, POS, BlockEntityEmcStorage.class).getStoredEmcBigInteger());
                check(sent.signum() > 0, "The output side didn't send any EMC");
                check(sent.longValueExact() <= rate, "The storage sent " + sent + " EMC in a tick, more than its rate of " + rate);
            })
            .thenWaitUntil(() -> check(blockEntity(helper, POS.east(), BlockEntityRelay.class).getStoredEmcBigInteger().signum() > 0, "The relay didn't get any EMC"))
            .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void inputSidesDontPush(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        fill(storage, 1000);
        helper.setBlock(POS.east(), Objects.requireNonNull(Matter.BASIC.getRelay()));
        helper.runAfterDelay(40, () -> {
            check(blockEntity(helper, POS, BlockEntityEmcStorage.class).getStoredEmcBigInteger().equals(BigInteger.valueOf(1000)), "EMC left through an input side");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void keepsEmcWhenBroken(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        fill(storage, 12_345);
        BlockPos pos = helper.absolutePos(POS);
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, storage);
        ItemStack drop = drops.stream().filter(stack -> stack.is(Items.EMC_STORAGE.get(EmcStorageTier.BASIC).get())).findFirst().orElse(ItemStack.EMPTY);
        check(!drop.isEmpty(), "The storage dropped " + drops);
        check(BlockEmcStorage.getStoredEmc(drop).equals(BigInteger.valueOf(12_345)), "The dropped storage has " + BlockEmcStorage.getStoredEmc(drop) + " EMC instead of 12345");

        helper.setBlock(POS.east(), Blocks.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        BlockEntityEmcStorage placed = blockEntity(helper, POS.east(), BlockEntityEmcStorage.class);
        BlockItem.updateCustomBlockEntityTag(helper.getLevel(), null, helper.absolutePos(POS.east()), drop);
        check(placed.getStoredEmcBigInteger().equals(BigInteger.valueOf(12_345)), "The placed storage has " + placed.getStoredEmcBigInteger() + " EMC instead of 12345");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgradeKeepsEmc(GameTestHelper helper) {
        var found = helper.getLevel().getRecipeManager().byKey(Main.rl("emc_storage/improved")).orElse(null);
        check(found instanceof CraftingRecipe, "The improved storage recipe didn't load");
        CraftingRecipe recipe = (CraftingRecipe) found;
        ItemStack basic = new ItemStack(Items.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        BlockEmcStorage.setStoredEmc(basic, BigInteger.valueOf(54_321));
        // a crafting grid needs a menu to tell about changes
        PXCraftingContainer grid = new PXCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override
            public boolean stillValid(Player player) {
                return true;
            }

            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }
        }, 3, 3);
        for (int i = 0; i < 9; i++) {
            ItemStack ingredient = i == 4 ? basic : i % 2 == 0 ? new ItemStack(Items.IANTHIUM_INGOT.get()) : new ItemStack(moze_intel.projecte.gameObjs.registries.PEItems.RED_MATTER.get());
            grid.setItem(i, ingredient);
        }
        check(recipe.matches(grid, helper.getLevel()), "The improved storage recipe doesn't match its own pattern");
        ItemStack result = cool.furry.mc.projectexpansion.util.Platform.assemble(recipe, grid, helper.getLevel());
        check(result.is(Items.EMC_STORAGE.get(EmcStorageTier.IMPROVED).get()), "The recipe made " + result);
        check(BlockEmcStorage.getStoredEmc(result).equals(BigInteger.valueOf(54_321)), "The improved storage has " + BlockEmcStorage.getStoredEmc(result) + " EMC instead of 54321");
        helper.succeed();
    }

    // an off side has no emc storage of its own, the overlay still shows what the block holds
    @GameTest(template = EMPTY)
    public static void overlayShowsEmcOnOffSides(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        fill(storage, 777);
        setSide(helper, Direction.UP, StorageSide.OFF);
        for (Direction side : List.of(Direction.UP, Direction.NORTH)) {
            CompoundTag data = new CompoundTag();
            EmcServerData.append(data, helper.getLevel(), helper.absolutePos(POS), blockEntity(helper, POS, BlockEntityEmcStorage.class), side);
            check(EmcServerData.has(data), "No emc was written when looking at the " + side + " side");
            check(EmcServerData.stored(data).equals(BigInteger.valueOf(777)), "Sent " + EmcServerData.stored(data) + " emc for the " + side + " side instead of 777");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void comparatorShowsFill(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        check(storage.getComparatorSignal() == 0, "An empty storage gives a signal of " + storage.getComparatorSignal());
        fill(storage, 1);
        check(storage.getComparatorSignal() == 1, "A storage with a little EMC gives a signal of " + storage.getComparatorSignal());
        storage.insertEmcBigInteger(EmcStorageTier.BASIC.getCapacity(), IEmcStorage.EmcAction.EXECUTE);
        check(storage.getComparatorSignal() == 15, "A full storage gives a signal of " + storage.getComparatorSignal());
        helper.succeed();
    }

    // a comparator only reads again when it's told the storage changed
    @GameTest(template = EMPTY)
    public static void comparatorFollowsEmc(GameTestHelper helper) {
        BlockEntityEmcStorage storage = place(helper, EmcStorageTier.BASIC);
        helper.setBlock(POS.east().below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(POS.east(), net.minecraft.world.level.block.Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
        storage.insertEmcBigInteger(EmcStorageTier.BASIC.getCapacity(), IEmcStorage.EmcAction.EXECUTE);
        helper.succeedWhen(() -> {
            int signal = blockEntity(helper, POS.east(), ComparatorBlockEntity.class).getOutputSignal();
            check(signal == 15, "The comparator next to a full storage gives a signal of " + signal);
        });
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void tiersHaveIncreasingEmc(GameTestHelper helper) {
        long previous = 0;
        for (EmcStorageTier tier : EmcStorageTier.VALUES) {
            long value = Platform.emcValue(Items.EMC_STORAGE.get(tier).get());
            check(value > previous, tier + " storage is worth " + value + " EMC, which isn't more than the tier before it (" + previous + ")");
            previous = value;
        }
        helper.succeed();
    }
}
