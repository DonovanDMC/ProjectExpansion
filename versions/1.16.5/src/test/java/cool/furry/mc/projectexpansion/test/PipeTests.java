package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import cool.furry.mc.projectexpansion.util.pipe.PipeSide;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import cool.furry.mc.projectexpansion.util.pipe.RedstoneMode;
import cool.furry.mc.projectexpansion.util.pipe.Wrench;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.waitUntil;

@IntegrationTestClass("empty")
public class PipeTests {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 1), FIRST_PIPE = new BlockPos(2, 1, 1), DESTINATION = new BlockPos(5, 1, 1);

    // relay -> three pipes -> relay, with the first pipe extracting from the source relay
    private static void line(IntegrationTestHelper helper) {
        setBlock(helper, SOURCE, Objects.requireNonNull(Matter.BASIC.getRelay()));
        for (int x = 2; x <= 4; x++) setBlock(helper, new BlockPos(x, 1, 1), Blocks.EMC_PIPE.get());
        setBlock(helper, DESTINATION, Objects.requireNonNull(Matter.BASIC.getRelay()));
        blockEntity(helper, SOURCE, BlockEntityRelay.class).insertEmc(10_000, IEmcStorage.EmcAction.EXECUTE);
        BlockPos pipe = absolute(helper, FIRST_PIPE);
        BlockEmcPipe.useWrench(helper.getWorld(), pipe, helper.getWorld().getBlockState(pipe), Direction.WEST, false);
        check(side(helper, FIRST_PIPE, Direction.WEST) == PipeSide.EXTRACTING, "The wrench didn't make the pipe extract from the source");
    }

    private static PipeSide side(IntegrationTestHelper helper, BlockPos pos, Direction side) {
        return helper.getBlockState(pos).getValue(BlockEmcPipe.SIDES.get(side));
    }

    private static long stored(IntegrationTestHelper helper, BlockPos pos) {
        return blockEntity(helper, pos, BlockEntityRelay.class).getStoredEmc();
    }

    @IntegrationTest("empty")
    public void pipeMovesEmc(IntegrationTestHelper helper) {
        line(helper);
        waitUntil(helper, () -> {
            check(stored(helper, DESTINATION) > 0, "No emc reached the destination relay");
            check(stored(helper, SOURCE) < 10_000, "The source relay didn't lose any emc");
        });
    }

    @IntegrationTest("empty")
    public void alwaysOffStopsPipe(IntegrationTestHelper helper) {
        line(helper);
        ItemStack upgrade = new ItemStack(Items.BASIC_PIPE_UPGRADE.get());
        PipeSettings.set(upgrade, PipeSettings.DEFAULT.withRedstoneMode(RedstoneMode.ALWAYS_OFF));
        blockEntity(helper, FIRST_PIPE, BlockEntityEmcPipe.class).setUpgradeStack(Direction.WEST, upgrade);
        // the source relay pushes into the pipe on its own too, so this covers both directions
        helper.runAfter(60, () -> check(stored(helper, DESTINATION) == 0, "Emc went through a pipe that's always off: " + stored(helper, DESTINATION)));
    }

    @IntegrationTest("empty")
    public void wrenchConfiguresSides(IntegrationTestHelper helper) {
        setBlock(helper, SOURCE, Objects.requireNonNull(Matter.BASIC.getRelay()));
        setBlock(helper, FIRST_PIPE, Blocks.EMC_PIPE.get());
        setBlock(helper, new BlockPos(3, 1, 1), Blocks.EMC_PIPE.get());
        check(side(helper, FIRST_PIPE, Direction.WEST) == PipeSide.CONNECTED, "The pipe didn't connect to the relay");
        check(side(helper, FIRST_PIPE, Direction.EAST) == PipeSide.CONNECTED, "The pipes didn't connect to each other");

        ServerPlayerEntity player = mockPlayer(helper);
        try {
            player.setItemInHand(Hand.MAIN_HAND, new ItemStack(Items.WRENCH.get()));
            clickSide(helper, player, FIRST_PIPE, Direction.WEST);
            check(side(helper, FIRST_PIPE, Direction.WEST) == PipeSide.EXTRACTING, "Using the wrench on the relay side didn't make it extract");
            clickSide(helper, player, FIRST_PIPE, Direction.WEST);
            check(side(helper, FIRST_PIPE, Direction.WEST) == PipeSide.CONNECTED, "Using the wrench again didn't stop extracting");

            player.setShiftKeyDown(true);
            clickSide(helper, player, FIRST_PIPE, Direction.EAST);
            check(side(helper, FIRST_PIPE, Direction.EAST) == PipeSide.NONE, "Sneaking with the wrench didn't disconnect the side");
            check(side(helper, new BlockPos(3, 1, 1), Direction.WEST) == PipeSide.NONE, "The other pipe stayed connected to a disconnected side");
            clickSide(helper, player, FIRST_PIPE, Direction.EAST);
            check(side(helper, FIRST_PIPE, Direction.EAST) == PipeSide.CONNECTED, "Sneaking with the wrench again didn't reconnect the side");
        } finally {
            removePlayer(player);
        }
    }

    // clicks the arm on that side of the pipe, like a player looking at it
    private static void clickSide(IntegrationTestHelper helper, ServerPlayerEntity player, BlockPos pos, Direction side) {
        BlockPos absolute = absolute(helper, pos);
        Vector3d hit = Vector3d.atCenterOf(absolute).add(side.getStepX() * 0.4, side.getStepY() * 0.4, side.getStepZ() * 0.4);
        BlockState state = helper.getWorld().getBlockState(absolute);
        state.use(helper.getWorld(), player, Hand.MAIN_HAND, new BlockRayTraceResult(hit, side, absolute, false));
    }

    @IntegrationTest("empty")
    public void upgradeTiers(IntegrationTestHelper helper) {
        check(PipeUpgrade.fromStack(new ItemStack(Items.BASIC_PIPE_UPGRADE.get())) == PipeUpgrade.BASIC, "The basic upgrade isn't basic");
        check(PipeUpgrade.fromStack(new ItemStack(Items.IMPROVED_PIPE_UPGRADE.get())) == PipeUpgrade.IMPROVED, "The improved upgrade isn't improved");
        check(PipeUpgrade.fromStack(new ItemStack(Items.ADVANCED_PIPE_UPGRADE.get())) == PipeUpgrade.ADVANCED, "The advanced upgrade isn't advanced");
        check(PipeUpgrade.fromStack(new ItemStack(Items.ULTIMATE_PIPE_UPGRADE.get())) == PipeUpgrade.ULTIMATE, "The ultimate upgrade isn't ultimate");
        check(PipeUpgrade.fromStack(new ItemStack(Items.INFINITY_PIPE_UPGRADE.get())) == PipeUpgrade.INFINITY, "The infinity upgrade isn't infinity");
        check(PipeUpgrade.fromStack(new ItemStack(Items.WRENCH.get())) == null, "The wrench counts as an upgrade");
        check(Wrench.isWrench(new ItemStack(Items.WRENCH.get())), "The wrench isn't in the wrench tags");

        long previous = PipeUpgrade.getRate(null);
        for (PipeUpgrade upgrade : PipeUpgrade.VALUES) {
            check(upgrade.getRate() > previous, upgrade + " doesn't move more emc than the tier before it");
            previous = upgrade.getRate();
        }
    }
}
