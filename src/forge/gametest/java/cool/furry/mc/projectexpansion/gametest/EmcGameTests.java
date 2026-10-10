package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class EmcGameTests {
    @GameTest(template = EMPTY)
    public static void collectorGeneratesEmc(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Objects.requireNonNull(Matter.FINAL.getCollector()));
        helper.succeedWhen(() -> check(blockEntity(helper, pos, BlockEntityCollector.class).getStoredEmcBigInteger().signum() > 0, "The collector didn't generate any EMC"));
    }

    @GameTest(template = EMPTY)
    public static void collectorPowersRelay(GameTestHelper helper) {
        BlockPos collector = new BlockPos(1, 1, 1), relay = new BlockPos(2, 1, 1);
        helper.setBlock(collector, Objects.requireNonNull(Matter.FINAL.getCollector()));
        helper.setBlock(relay, Objects.requireNonNull(Matter.BASIC.getRelay()));
        helper.succeedWhen(() -> check(blockEntity(helper, relay, BlockEntityRelay.class).getStoredEmcBigInteger().signum() > 0, "The relay didn't receive any EMC from the collector"));
    }

    @GameTest(template = EMPTY)
    public static void powerFlowerStoresEmcWhileOwnerOffline(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        // nobody owns it, so what it generates stays in the flower
        helper.succeedWhen(() -> check(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc.signum() > 0, "The power flower didn't store any EMC"));
    }

    @GameTest(template = EMPTY)
    public static void matterTiersGetStronger(GameTestHelper helper) {
        for (Matter matter : Matter.VALUES) {
            if (matter.ordinal() == 0) continue;
            Matter prev = matter.prev();
            check(matter.getCollectorOutput().compareTo(prev.getCollectorOutput()) >= 0, matter + " collectors make less EMC than " + prev);
            check(matter.getPowerFlowerOutput().compareTo(prev.getPowerFlowerOutput()) >= 0, matter + " power flowers make less EMC than " + prev);
            check(matter.getRelayTransfer().compareTo(prev.getRelayTransfer()) >= 0, matter + " relays transfer less EMC than " + prev);
        }
        check(Matter.FINAL.getCollectorOutput().compareTo(Matter.BASIC.getCollectorOutput()) > 0, "Final collectors aren't better than basic ones");
        helper.succeed();
    }

    @BeforeBatch(batch = "emc")
    public static void prepareEmc(ServerLevel level) {
        Platform.prepareEmc(level.getServer());
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void itemsHaveIncreasingEmc(GameTestHelper helper) {
        long previous = 0;
        for (Matter matter : Matter.VALUES) {
            if (!matter.hasItem) continue;
            long value = Platform.emcValue(Objects.requireNonNull(matter.getMatter()));
            check(value > previous, matter + " matter has " + value + " EMC, which isn't more than the tier before it (" + previous + ")");
            previous = value;
        }
        previous = 0;
        for (Fuel fuel : Fuel.values()) {
            Item item = fuel.getItem();
            if (item == null) continue;
            long value = Platform.emcValue(item);
            check(value > previous, fuel + " fuel has " + value + " EMC, which isn't more than the tier before it (" + previous + ")");
            previous = value;
        }
        check(previous > 0, "No fuel had any EMC");
        helper.succeed();
    }
}
