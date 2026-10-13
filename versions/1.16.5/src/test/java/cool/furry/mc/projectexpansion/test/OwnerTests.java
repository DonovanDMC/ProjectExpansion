package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emc;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.steps;

// things tied to a player that has to be online, see TestUtil.mockPlayer
@IntegrationTestClass("empty")
public class OwnerTests {
    // EMC is handed over once a second, on top of the flower's own delay
    @IntegrationTest(value = "empty", timeoutTicks = 200)
    public void powerFlowerPaysOnlineOwner(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayerEntity player = mockPlayer(helper);
        setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        blockEntity(helper, pos, BlockEntityPowerFlower.class).setOwner(player);
        steps(helper,
            () -> {
                check(emc(player).signum() > 0, "The owner didn't receive any EMC");
                check(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc.signum() == 0, "The flower kept EMC while its owner was online");
            },
            () -> removePlayer(player)
        );
    }

    @IntegrationTest(value = "empty", timeoutTicks = 200)
    public void powerFlowerPaysOwnerOnceOnline(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        UUID owner = UUID.randomUUID();
        setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        blockEntity(helper, pos, BlockEntityPowerFlower.class).owner = owner;
        AtomicReference<ServerPlayerEntity> player = new AtomicReference<>();
        AtomicReference<BigInteger> stored = new AtomicReference<>();
        steps(helper,
            () -> check(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc.signum() > 0, "The flower didn't store any EMC for its offline owner"),
            () -> {
                stored.set(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc);
                player.set(mockPlayer(helper, owner, false));
            },
            () -> check(emc(player.get()).compareTo(stored.get()) >= 0, "The owner got " + emc(player.get()) + " EMC after joining, the flower had stored " + stored.get()),
            () -> removePlayer(player.get())
        );
    }

    @IntegrationTest("empty")
    public void onlyOwnerPassesOwnershipCheck(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayerEntity owner = mockPlayer(helper), other = mockPlayer(helper);
        try {
            setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
            BlockEntityPowerFlower flower = blockEntity(helper, pos, BlockEntityPowerFlower.class);
            flower.setOwner(owner);
            check(flower.handleActivation(owner, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP), "The owner failed the ownership check");
            check(!flower.handleActivation(other, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP), "Someone else passed the ownership check");
            check(flower.handleActivation(other, BlockEntityOwnable.ActivationType.DISPLAY_NAME), "Displaying the owner's name failed");
        } finally {
            removePlayer(owner);
            removePlayer(other);
        }
    }
}
