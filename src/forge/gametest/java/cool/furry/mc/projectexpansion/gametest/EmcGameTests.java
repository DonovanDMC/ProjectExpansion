package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    private static long storedIn(ItemStack stack) {
        return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).map(holder -> holder.getStoredEmc(stack)).orElse(0L);
    }

    // charging used to take the wrong amount out of the collector, rather than what the item took
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void collectorChargesKleinStars(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Objects.requireNonNull(Matter.BASIC.getCollector()));
        BlockEntityCollector collector = blockEntity(helper, pos, BlockEntityCollector.class);
        ItemStack left = collector.getAux().insertItem(BlockEntityCollector.UPGRADING_SLOT, new ItemStack(PEItems.KLEIN_STAR_OMEGA.get()), false);
        check(left.isEmpty(), "The collector didn't take the klein star");
        // it only ticks once a second, and only takes emc in while it has something to charge
        helper.runAfterDelay(25, () -> {
            check(collector.hasChargeableItem, "The collector doesn't see the klein star as chargeable");
            IEmcStorage storage = collector.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, Direction.UP).resolve().orElseThrow(() -> new GameTestAssertException("The collector stores no emc"));
            storage.insertEmc(collector.getNeededEmc(), IEmcStorage.EmcAction.EXECUTE);
            long before = collector.getStoredEmc() + storedIn(collector.getAux().getStackInSlot(BlockEntityCollector.UPGRADING_SLOT));
            helper.succeedWhen(() -> {
                long star = storedIn(collector.getAux().getStackInSlot(BlockEntityCollector.UPGRADING_SLOT));
                long total = collector.getStoredEmc() + star;
                check(total >= before, "The collector & star went from " + before + " EMC to " + total);
                check(star > 0, "The klein star hasn't been charged, the collector has " + collector.getStoredEmc() + " EMC");
            });
        });
    }

    // a basic collector makes less than one emc a tick, which used to round down to never sending any out
    // relays get a bonus from collectors on top of what they're sent, so storage is used to only see what's sent
    @GameTest(template = EMPTY)
    public static void basicCollectorFillsStorage(GameTestHelper helper) {
        BlockPos collector = new BlockPos(1, 1, 1), storage = new BlockPos(2, 1, 1);
        helper.setBlock(collector, Objects.requireNonNull(Matter.BASIC.getCollector()));
        helper.setBlock(storage, Blocks.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        helper.succeedWhen(() -> check(blockEntity(helper, storage, BlockEntityEmcStorage.class).getStoredEmcBigInteger().signum() > 0, "The storage didn't receive any EMC from the basic collector"));
    }
}
