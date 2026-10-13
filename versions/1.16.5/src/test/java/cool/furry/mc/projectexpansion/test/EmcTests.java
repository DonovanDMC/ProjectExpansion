package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.waitUntil;

@IntegrationTestClass("empty")
public class EmcTests {
    @IntegrationTest("empty")
    public void collectorGeneratesEmc(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Objects.requireNonNull(Matter.FINAL.getCollector()));
        waitUntil(helper, () -> check(blockEntity(helper, pos, BlockEntityCollector.class).getStoredEmcBigInteger().signum() > 0, "The collector didn't generate any EMC"));
    }

    @IntegrationTest("empty")
    public void collectorPowersRelay(IntegrationTestHelper helper) {
        BlockPos collector = new BlockPos(1, 1, 1), relay = new BlockPos(2, 1, 1);
        setBlock(helper, collector, Objects.requireNonNull(Matter.FINAL.getCollector()));
        setBlock(helper, relay, Objects.requireNonNull(Matter.BASIC.getRelay()));
        waitUntil(helper, () -> check(blockEntity(helper, relay, BlockEntityRelay.class).getStoredEmcBigInteger().signum() > 0, "The relay didn't receive any EMC from the collector"));
    }

    @IntegrationTest("empty")
    public void powerFlowerStoresEmcWhileOwnerOffline(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        // nobody owns it, so what it generates stays in the flower
        waitUntil(helper, () -> check(blockEntity(helper, pos, BlockEntityPowerFlower.class).emc.signum() > 0, "The power flower didn't store any EMC"));
    }

    @IntegrationTest("empty")
    public void matterTiersGetStronger(IntegrationTestHelper helper) {
        for (Matter matter : Matter.VALUES) {
            if (matter.ordinal() == 0) continue;
            Matter prev = matter.prev();
            check(matter.getCollectorOutput().compareTo(prev.getCollectorOutput()) >= 0, matter + " collectors make less EMC than " + prev);
            check(matter.getPowerFlowerOutput().compareTo(prev.getPowerFlowerOutput()) >= 0, matter + " power flowers make less EMC than " + prev);
            check(matter.getRelayTransfer().compareTo(prev.getRelayTransfer()) >= 0, matter + " relays transfer less EMC than " + prev);
        }
        check(Matter.FINAL.getCollectorOutput().compareTo(Matter.BASIC.getCollectorOutput()) > 0, "Final collectors aren't better than basic ones");
    }

    @IntegrationTest("empty")
    public void itemsHaveIncreasingEmc(IntegrationTestHelper helper) {
        long previous = 0;
        for (Matter matter : Matter.VALUES) {
            if (!matter.hasItem) continue;
            long value = emcValue(Objects.requireNonNull(matter.getMatter()));
            check(value > previous, matter + " matter has " + value + " EMC, which isn't more than the tier before it (" + previous + ")");
            previous = value;
        }
        previous = 0;
        for (Fuel fuel : Fuel.values()) {
            Item item = fuel.getItem();
            if (item == null) continue;
            long value = emcValue(item);
            check(value > previous, fuel + " fuel has " + value + " EMC, which isn't more than the tier before it (" + previous + ")");
            previous = value;
        }
        check(previous > 0, "No fuel had any EMC");
    }
}
