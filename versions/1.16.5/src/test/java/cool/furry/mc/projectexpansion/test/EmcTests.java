package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.steps;
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

    private static long storedIn(ItemStack stack) {
        return stack.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).map(holder -> holder.getStoredEmc(stack)).orElse(0L);
    }

    // charging used to take the wrong amount out of the collector, rather than what the item took
    @IntegrationTest("empty")
    public void collectorChargesKleinStars(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getCollector()));
        BlockEntityCollector collector = blockEntity(helper, pos, BlockEntityCollector.class);
        ItemStack left = collector.getAux().insertItem(BlockEntityCollector.UPGRADING_SLOT, new ItemStack(PEItems.KLEIN_STAR_OMEGA.get()), false);
        check(left.isEmpty(), "The collector didn't take the klein star");
        AtomicLong before = new AtomicLong();
        steps(helper,
                // it only ticks once a second, and only takes emc in while it has something to charge
                () -> check(collector.hasChargeableItem, "The collector doesn't see the klein star as chargeable"),
                () -> {
                    IEmcStorage storage = collector.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, Direction.UP).resolve().orElseThrow(() -> new AssertionError("The collector stores no emc"));
                    storage.insertEmc(collector.getNeededEmc(), IEmcStorage.EmcAction.EXECUTE);
                    before.set(collector.getStoredEmc() + storedIn(collector.getAux().getStackInSlot(BlockEntityCollector.UPGRADING_SLOT)));
                },
                () -> {
                    long star = storedIn(collector.getAux().getStackInSlot(BlockEntityCollector.UPGRADING_SLOT));
                    long total = collector.getStoredEmc() + star;
                    check(total >= before.get(), "The collector & star went from " + before.get() + " EMC to " + total);
                    check(star > 0, "The klein star hasn't been charged, the collector has " + collector.getStoredEmc() + " EMC");
                });
    }

    // a basic collector makes less than one emc a tick, which used to round down to never sending any out
    // relays get a bonus from collectors on top of what they're sent, so storage is used to only see what's sent
    @IntegrationTest("empty")
    public void basicCollectorFillsStorage(IntegrationTestHelper helper) {
        BlockPos collector = new BlockPos(1, 1, 1), storage = new BlockPos(2, 1, 1);
        setBlock(helper, collector, Objects.requireNonNull(Matter.BASIC.getCollector()));
        setBlock(helper, storage, Blocks.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        waitUntil(helper, () -> check(blockEntity(helper, storage, BlockEntityEmcStorage.class).getStoredEmcBigInteger().signum() > 0, "The storage didn't receive any EMC from the basic collector"));
    }
}
