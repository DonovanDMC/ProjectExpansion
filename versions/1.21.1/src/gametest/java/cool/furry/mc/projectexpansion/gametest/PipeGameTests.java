package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import cool.furry.mc.projectexpansion.util.pipe.PipeSide;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import cool.furry.mc.projectexpansion.util.pipe.RedstoneMode;
import cool.furry.mc.projectexpansion.util.pipe.Wrench;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class PipeGameTests {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 1), FIRST_PIPE = new BlockPos(2, 1, 1), DESTINATION = new BlockPos(5, 1, 1);

    // relay -> three pipes -> relay, with the first pipe extracting from the source relay
    private static void line(GameTestHelper helper) {
        helper.setBlock(SOURCE, Objects.requireNonNull(Matter.BASIC.getRelay()));
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 1, 1), Blocks.EMC_PIPE.get());
        helper.setBlock(DESTINATION, Objects.requireNonNull(Matter.BASIC.getRelay()));
        blockEntity(helper, SOURCE, BlockEntityRelay.class).insertEmc(10_000, IEmcStorage.EmcAction.EXECUTE);
        BlockPos pipe = helper.absolutePos(FIRST_PIPE);
        BlockEmcPipe.useWrench(helper.getLevel(), pipe, helper.getLevel().getBlockState(pipe), Direction.WEST, false);
        check(side(helper, FIRST_PIPE, Direction.WEST) == PipeSide.EXTRACTING, "The wrench didn't make the pipe extract from the source");
    }

    private static PipeSide side(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getBlockState(pos).getValue(BlockEmcPipe.SIDES.get(side));
    }

    private static long stored(GameTestHelper helper, BlockPos pos) {
        return blockEntity(helper, pos, BlockEntityRelay.class).getStoredEmc();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeMovesEmc(GameTestHelper helper) {
        line(helper);
        helper.succeedWhen(() -> {
            check(stored(helper, DESTINATION) > 0, "No emc reached the destination relay");
            check(stored(helper, SOURCE) < 10_000, "The source relay didn't lose any emc");
        });
    }

    @GameTest(template = EMPTY)
    public static void alwaysOffStopsPipe(GameTestHelper helper) {
        line(helper);
        ItemStack upgrade = new ItemStack(Items.BASIC_PIPE_UPGRADE.get());
        upgrade.set(DataComponentTypes.PIPE_SETTINGS.get(), PipeSettings.DEFAULT.withRedstoneMode(RedstoneMode.ALWAYS_OFF));
        blockEntity(helper, FIRST_PIPE, BlockEntityEmcPipe.class).setUpgradeStack(Direction.WEST, upgrade);
        // the source relay pushes into the pipe on its own too, so this covers both directions
        helper.runAfterDelay(60, () -> {
            check(stored(helper, DESTINATION) == 0, "Emc went through a pipe that's always off: " + stored(helper, DESTINATION));
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void wrenchConfiguresSides(GameTestHelper helper) {
        helper.setBlock(SOURCE, Objects.requireNonNull(Matter.BASIC.getRelay()));
        helper.setBlock(FIRST_PIPE, Blocks.EMC_PIPE.get());
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.EMC_PIPE.get());
        check(side(helper, FIRST_PIPE, Direction.WEST) == PipeSide.CONNECTED, "The pipe didn't connect to the relay");
        check(side(helper, FIRST_PIPE, Direction.EAST) == PipeSide.CONNECTED, "The pipes didn't connect to each other");

        ServerPlayer player = mockPlayer(helper);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WRENCH.get()));
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
        helper.succeed();
    }

    // clicks the arm on that side of the pipe, like a player looking at it
    private static void clickSide(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
        BlockPos absolute = helper.absolutePos(pos);
        Vec3 hit = Vec3.atCenterOf(absolute).add(side.getStepX() * 0.4, side.getStepY() * 0.4, side.getStepZ() * 0.4);
        BlockState state = helper.getLevel().getBlockState(absolute);
        state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, side, absolute, false));
    }

    @GameTest(template = EMPTY)
    public static void upgradeTiers(GameTestHelper helper) {
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
        helper.succeed();
    }
}
