package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.waitUntil;

@IntegrationTestClass("empty")
public class BlockTests {
    private static IItemHandler items(IntegrationTestHelper helper, BlockPos pos, Direction side) {
        return Objects.requireNonNull(helper.getTileEntity(pos), "No block entity at " + pos).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).resolve().orElseThrow(() -> new AssertionError("No item handler at " + pos));
    }

    private static int count(IItemHandler handler, Item item) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).getItem() == item) count += handler.getStackInSlot(i).getCount();
        }
        return count;
    }

    @IntegrationTest("empty")
    public void condenserMk3Condenses(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Blocks.CONDENSER_MK3.get());
        blockEntity(helper, pos, BlockEntityCondenserMK3.class).getSidedHandler(Direction.UP).setLockInfoFromPacket(ItemInfo.fromItem(Items.IRON_INGOT));
        ItemStack left = items(helper, pos, Direction.UP).insertItem(0, new ItemStack(Items.DIAMOND), false);
        check(left.isEmpty(), "The condenser didn't take the diamond");
        waitUntil(helper, () -> check(count(items(helper, pos, Direction.DOWN), Items.IRON_INGOT) > 0, "The condenser didn't make any iron, or it can't be taken from the bottom"));
    }

    // each side has its own lock, but they all output to the bottom
    @IntegrationTest("empty")
    public void condenserMk3SidesAreSeparate(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Blocks.CONDENSER_MK3.get());
        BlockEntityCondenserMK3 condenser = blockEntity(helper, pos, BlockEntityCondenserMK3.class);
        condenser.getSidedHandler(Direction.UP).setLockInfoFromPacket(ItemInfo.fromItem(Items.IRON_INGOT));
        condenser.getSidedHandler(Direction.NORTH).setLockInfoFromPacket(ItemInfo.fromItem(Items.GOLD_INGOT));
        items(helper, pos, Direction.NORTH).insertItem(0, new ItemStack(Items.DIAMOND), false);
        waitUntil(helper, () -> {
            IItemHandler output = items(helper, pos, Direction.DOWN);
            check(count(output, Items.GOLD_INGOT) > 0, "The north side didn't make any gold");
            check(count(output, Items.IRON_INGOT) == 0, "The top side made iron from the north side's diamond");
        });
    }

    @IntegrationTest("empty")
    public void compactSunBoostsCollector(IntegrationTestHelper helper) {
        BlockPos plain = new BlockPos(1, 1, 1), boosted = new BlockPos(3, 1, 1);
        setBlock(helper, plain, Objects.requireNonNull(Matter.BASIC.getCollector()));
        setBlock(helper, boosted, Objects.requireNonNull(Matter.BASIC.getCollector()));
        setBlock(helper, boosted.above(), Blocks.COMPACT_SUN.get());
        BlockEntityCollector without = blockEntity(helper, plain, BlockEntityCollector.class), with = blockEntity(helper, boosted, BlockEntityCollector.class);
        check(!without.hasSunBonus(), "A collector without a compact sun has the sun bonus");
        check(with.hasSunBonus(), "A collector under a compact sun doesn't have the sun bonus");
        check(with.getGeneratedEMC().compareTo(without.getGeneratedEMC()) > 0, "A collector under a compact sun makes " + with.getGeneratedEMC() + " EMC, no more than one without (" + without.getGeneratedEMC() + ")");
    }

    @IntegrationTest("empty")
    public void compactSunBoostsPowerFlower(IntegrationTestHelper helper) {
        BlockPos plain = new BlockPos(1, 2, 1), boosted = new BlockPos(3, 2, 1);
        setBlock(helper, plain, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        setBlock(helper, boosted, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        setBlock(helper, boosted.below(), Blocks.COMPACT_SUN.get());
        BlockEntityPowerFlower without = blockEntity(helper, plain, BlockEntityPowerFlower.class), with = blockEntity(helper, boosted, BlockEntityPowerFlower.class);
        check(!without.hasSunBonus(), "A power flower without a compact sun has the sun bonus");
        check(with.hasSunBonus(), "A power flower on a compact sun doesn't have the sun bonus");
        check(with.getGeneratedEMC().compareTo(without.getGeneratedEMC()) > 0, "A power flower on a compact sun makes " + with.getGeneratedEMC() + " EMC, no more than one without (" + without.getGeneratedEMC() + ")");
    }
}
