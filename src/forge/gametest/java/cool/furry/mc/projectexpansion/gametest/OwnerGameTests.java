package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

// things tied to a player that has to be online, see GameTestUtil.mockPlayer
@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class OwnerGameTests {
    private static BigInteger emc(ServerPlayer player) {
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        check(provider != null, "No knowledge provider for " + player.getScoreboardName());
        return provider.getEmc();
    }

    // EMC is handed over once a second, on top of the flower's own delay
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void powerFlowerPaysOnlineOwner(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper);
        helper.setBlock(pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        blockEntity(helper, pos, BlockEntityPowerFlower.class).setOwner(player);
        helper.startSequence()
            .thenWaitUntil(() -> {
                check(emc(player).signum() > 0, "The owner didn't receive any EMC");
                check(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc.signum() == 0, "The flower kept EMC while its owner was online");
            })
            .thenExecute(() -> removePlayer(player))
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void powerFlowerPaysOwnerOnceOnline(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        UUID owner = UUID.randomUUID();
        helper.setBlock(pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        blockEntity(helper, pos, BlockEntityPowerFlower.class).owner = owner;
        AtomicReference<ServerPlayer> player = new AtomicReference<>();
        AtomicReference<BigInteger> stored = new AtomicReference<>();
        helper.startSequence()
            .thenWaitUntil(() -> check(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc.signum() > 0, "The flower didn't store any EMC for its offline owner"))
            .thenExecute(() -> {
                stored.set(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc);
                player.set(mockPlayer(helper, owner, Commands.LEVEL_ALL));
            })
            .thenWaitUntil(() -> check(emc(player.get()).compareTo(stored.get()) >= 0, "The owner got " + emc(player.get()) + " EMC after joining, the flower had stored " + stored.get()))
            .thenExecute(() -> removePlayer(player.get()))
            .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void onlyOwnerPassesOwnershipCheck(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer owner = mockPlayer(helper), other = mockPlayer(helper);
        try {
            helper.setBlock(pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
            BlockEntityPowerFlower flower = blockEntity(helper, pos, BlockEntityPowerFlower.class);
            flower.setOwner(owner);
            check(flower.handleActivation(owner, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP), "The owner failed the ownership check");
            check(!flower.handleActivation(other, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP), "Someone else passed the ownership check");
            check(flower.handleActivation(other, BlockEntityOwnable.ActivationType.DISPLAY_NAME), "Displaying the owner's name failed");
        } finally {
            removePlayer(owner);
            removePlayer(other);
        }
        helper.succeed();
    }
}
