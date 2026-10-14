package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.gui.container.ContainerPowerFlower;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.PowerFlowerMode;
import cool.furry.mc.projectexpansion.util.pipe.PipeSide;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.math.BigInteger;
import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.steps;
import static cool.furry.mc.projectexpansion.test.TestUtil.waitUntil;

@IntegrationTestClass("empty")
public class PowerFlowerTests {
    private static final BlockPos FLOWER = new BlockPos(1, 1, 1), NEXT = new BlockPos(2, 1, 1), AFTER = new BlockPos(3, 1, 1);

    // nobody owns these, so player mode only ever stores
    private static BlockEntityPowerFlower flower(IntegrationTestHelper helper, PowerFlowerMode mode) {
        setBlock(helper, FLOWER, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        BlockEntityPowerFlower flower = blockEntity(helper, FLOWER, BlockEntityPowerFlower.class);
        flower.setMode(mode);
        return flower;
    }

    private static IEmcStorage storage(IntegrationTestHelper helper) {
        return BlockEntityEmcPipe.getEmcStorage(helper.getWorld(), absolute(helper, FLOWER), Direction.EAST);
    }

    private static PipeSide pipeSide(IntegrationTestHelper helper) {
        return helper.getBlockState(NEXT).getValue(BlockEmcPipe.SIDES.get(Direction.WEST));
    }

    @IntegrationTest("empty")
    public void playerModeKeepsItsEmc(IntegrationTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.PLAYER);
        flower.emc = BigInteger.valueOf(1_000);
        check(storage(helper) == null, "A flower in player mode offers its emc to other blocks");
        check(flower.extractEmc(500, IEmcStorage.EmcAction.EXECUTE) == 0, "Emc was taken from a flower in player mode");
        check(flower.emc.equals(BigInteger.valueOf(1_000)), "The flower lost emc: " + flower.emc);
    }

    @IntegrationTest("empty")
    public void storageModeLetsEmcBeTaken(IntegrationTestHelper helper) {
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
    }

    @IntegrationTest("empty")
    public void storageModeKeepsEmcWithOwnerOnline(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.STORAGE);
        flower.setOwner(player);
        steps(helper,
            () -> check(flower.emc.signum() > 0, "The flower didn't keep any emc"),
            () -> removePlayer(player)
        );
    }

    @IntegrationTest(value = "empty", timeoutTicks = 200)
    public void pipeTakesEmcFromFlower(IntegrationTestHelper helper) {
        flower(helper, PowerFlowerMode.STORAGE);
        setBlock(helper, NEXT, Blocks.EMC_PIPE.get());
        setBlock(helper, AFTER, Objects.requireNonNull(Matter.BASIC.getRelay()));
        BlockPos pipe = absolute(helper, NEXT);
        BlockEmcPipe.useWrench(helper.getWorld(), pipe, helper.getWorld().getBlockState(pipe), Direction.WEST, false);
        check(pipeSide(helper) == PipeSide.EXTRACTING, "The wrench didn't make the pipe extract from the flower");
        waitUntil(helper, () -> check(blockEntity(helper, AFTER, BlockEntityRelay.class).getStoredEmc() > 0, "No emc reached the relay"));
    }

    @IntegrationTest("empty")
    public void pipeFollowsModeChanges(IntegrationTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.PLAYER);
        setBlock(helper, NEXT, Blocks.EMC_PIPE.get());
        check(pipeSide(helper) == PipeSide.NONE, "The pipe connected to a flower in player mode");
        flower.setMode(PowerFlowerMode.STORAGE);
        check(pipeSide(helper) == PipeSide.CONNECTED, "The pipe didn't connect once the flower was in storage mode");
        flower.setMode(PowerFlowerMode.PLAYER);
        check(pipeSide(helper) == PipeSide.NONE, "The pipe stayed connected to a flower back in player mode");
    }

    @IntegrationTest(value = "empty", timeoutTicks = 200)
    public void ejectModePushesEmcOut(IntegrationTestHelper helper) {
        flower(helper, PowerFlowerMode.EJECT);
        setBlock(helper, NEXT, Objects.requireNonNull(Matter.BASIC.getRelay()));
        waitUntil(helper, () -> check(blockEntity(helper, NEXT, BlockEntityRelay.class).getStoredEmc() > 0, "No emc reached the relay"));
    }

    @IntegrationTest("empty")
    public void ejectModeKeepsWhatNothingTakes(IntegrationTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.EJECT);
        waitUntil(helper, () -> check(flower.emc.signum() > 0, "The flower lost emc with nothing to push it into"));
    }

    @IntegrationTest("empty")
    public void modeIsSaved(IntegrationTestHelper helper) {
        BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.EJECT);
        TileEntity loaded = TileEntity.loadStatic(flower.getBlockState(), flower.save(new CompoundNBT()));
        check(loaded instanceof BlockEntityPowerFlower, "Couldn't load " + flower);
        PowerFlowerMode mode = ((BlockEntityPowerFlower) loaded).getMode();
        check(mode == PowerFlowerMode.EJECT, "The mode changed from eject to " + mode);
    }

    @IntegrationTest("empty")
    public void onlyOwnerChangesMode(IntegrationTestHelper helper) {
        ServerPlayerEntity owner = mockPlayer(helper), other = mockPlayer(helper);
        try {
            BlockEntityPowerFlower flower = flower(helper, PowerFlowerMode.PLAYER);
            flower.setOwner(owner);
            ContainerPowerFlower menu = new ContainerPowerFlower(0, owner.inventory, flower);
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
    }
}
