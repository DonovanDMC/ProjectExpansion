package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.gui.container.ContainerPowerFlower;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.PowerFlowerMode;
import cool.furry.mc.projectexpansion.util.pipe.PipeSide;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;
import java.util.Objects;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class PowerFlowerGameTests {
    private static final BlockPos FLOWER = new BlockPos(1, 1, 1), NEXT = new BlockPos(2, 1, 1), AFTER = new BlockPos(3, 1, 1);

    // nobody owns these, so player mode only ever stores
    private static BlockEntityPowerFlower flower(GameTestHelper helper, PowerFlowerMode mode) {
        helper.setBlock(FLOWER, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        BlockEntityPowerFlower flower = blockEntity(helper, FLOWER, BlockEntityPowerFlower.class);
        flower.setMode(mode);
        return flower;
    }

    private static IEmcStorage storage(GameTestHelper helper) {
        return BlockEntityEmcPipe.getEmcStorage(helper.getLevel(), helper.absolutePos(FLOWER), Direction.EAST);
    }

    private static PipeSide pipeSide(GameTestHelper helper) {
        return helper.getBlockState(NEXT).getValue(BlockEmcPipe.SIDES.get(Direction.WEST));
    }

    @GameTest(template = EMPTY)
    public static void playerModeKeepsItsEmc(GameTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.PLAYER);
        flower.emc = BigInteger.valueOf(1_000);
        check(storage(helper) == null, "A flower in player mode offers its emc to other blocks");
        check(flower.extractEmc(500, IEmcStorage.EmcAction.EXECUTE) == 0, "Emc was taken from a flower in player mode");
        check(flower.emc.equals(BigInteger.valueOf(1_000)), "The flower lost emc: " + flower.emc);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void storageModeLetsEmcBeTaken(GameTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.STORAGE);
        flower.emc = BigInteger.valueOf(1_000);
        IEmcStorage storage = storage(helper);
        check(storage != null, "A flower in storage mode doesn't offer its emc to other blocks");
        check(storage.extractEmc(400, IEmcStorage.EmcAction.SIMULATE) == 400, "Simulating didn't offer the emc asked for");
        check(flower.emc.equals(BigInteger.valueOf(1_000)), "Simulating took emc: " + flower.emc);
        check(storage.extractEmc(400, IEmcStorage.EmcAction.EXECUTE) == 400, "The emc asked for wasn't given");
        check(flower.emc.equals(BigInteger.valueOf(600)), "The flower has " + flower.emc + " emc left, not 600");
        check(storage.extractEmc(5_000, IEmcStorage.EmcAction.EXECUTE) == 600, "More emc was given than the flower had");
        check(storage.insertEmc(100, IEmcStorage.EmcAction.EXECUTE) == 0, "The flower took emc in");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void storageModeKeepsEmcWithOwnerOnline(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.STORAGE);
        flower.setOwner(player);
        helper.succeedWhen(() -> {
            check(flower.emc.signum() > 0, "The flower didn't keep any emc");
            removePlayer(player);
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeTakesEmcFromFlower(GameTestHelper helper) {
        flower(helper, PowerFlowerMode.STORAGE);
        helper.setBlock(NEXT, Blocks.EMC_PIPE.get());
        helper.setBlock(AFTER, Objects.requireNonNull(Matter.BASIC.getRelay()));
        BlockPos pipe = helper.absolutePos(NEXT);
        BlockEmcPipe.useWrench(helper.getLevel(), pipe, helper.getLevel().getBlockState(pipe), Direction.WEST, false);
        check(pipeSide(helper) == PipeSide.EXTRACTING, "The wrench didn't make the pipe extract from the flower");
        helper.succeedWhen(() -> check(blockEntity(helper, AFTER, BlockEntityRelay.class).getStoredEmc() > 0, "No emc reached the relay"));
    }

    @GameTest(template = EMPTY)
    public static void pipeFollowsModeChanges(GameTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.PLAYER);
        helper.setBlock(NEXT, Blocks.EMC_PIPE.get());
        check(pipeSide(helper) == PipeSide.NONE, "The pipe connected to a flower in player mode");
        flower.setMode(PowerFlowerMode.STORAGE);
        check(pipeSide(helper) == PipeSide.CONNECTED, "The pipe didn't connect once the flower was in storage mode");
        flower.setMode(PowerFlowerMode.PLAYER);
        check(pipeSide(helper) == PipeSide.NONE, "The pipe stayed connected to a flower back in player mode");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void ejectModePushesEmcOut(GameTestHelper helper) {
        flower(helper, PowerFlowerMode.EJECT);
        helper.setBlock(NEXT, Objects.requireNonNull(Matter.BASIC.getRelay()));
        helper.succeedWhen(() -> check(blockEntity(helper, NEXT, BlockEntityRelay.class).getStoredEmc() > 0, "No emc reached the relay"));
    }

    @GameTest(template = EMPTY)
    public static void ejectModeKeepsWhatNothingTakes(GameTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.EJECT);
        helper.succeedWhen(() -> check(flower.emc.signum() > 0, "The flower lost emc with nothing to push it into"));
    }

    @GameTest(template = EMPTY)
    public static void modeIsSaved(GameTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.EJECT);
        BlockEntity loaded = BlockEntity.loadStatic(flower.getBlockPos(), flower.getBlockState(), flower.saveWithFullMetadata());
        check(loaded instanceof BlockEntityPowerFlower, "Couldn't load " + flower);
        PowerFlowerMode mode = ((BlockEntityPowerFlower) loaded).getMode();
        check(mode == PowerFlowerMode.EJECT, "The mode changed from eject to " + mode);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void onlyOwnerChangesMode(GameTestHelper helper) {
        ServerPlayer owner = mockPlayer(helper), other = mockPlayer(helper);
        try {
            BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.PLAYER);
            flower.setOwner(owner);
            ContainerPowerFlower menu = new ContainerPowerFlower(0, owner.getInventory(), flower);
            check(!menu.clickMenuButton(other, ContainerPowerFlower.BUTTON_MODE), "Someone else's click was accepted");
            check(flower.getMode() == PowerFlowerMode.PLAYER, "Someone else changed the mode to " + flower.getMode());
            check(menu.clickMenuButton(owner, ContainerPowerFlower.BUTTON_MODE), "The owner's click wasn't accepted");
            check(flower.getMode() == PowerFlowerMode.STORAGE, "The owner's click left the mode at " + flower.getMode());
            menu.broadcastChanges();
            check(menu.getMode() == PowerFlowerMode.STORAGE, "The menu still shows " + menu.getMode());
        } finally {
            removePlayer(owner);
            removePlayer(other);
        }
        helper.succeed();
    }
}
