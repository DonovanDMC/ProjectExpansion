package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.TagNames;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.waitUntil;

@IntegrationTestClass("empty")
public class PersistenceTests {
    // block entities loaded from nbt have no world to read their state from before 1.17, so it's passed along
    private static TileEntity reload(TileEntity blockEntity, BlockState state) {
        TileEntity loaded = TileEntity.loadStatic(state, blockEntity.save(new CompoundNBT()));
        check(loaded != null, "Couldn't load " + blockEntity);
        return loaded;
    }

    @IntegrationTest("empty")
    public void collectorKeepsEmc(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Objects.requireNonNull(Matter.FINAL.getCollector()));
        waitUntil(helper, () -> {
            BlockEntityCollector collector = blockEntity(helper, pos, BlockEntityCollector.class);
            check(collector.getStoredEmcBigInteger().signum() > 0, "The collector didn't generate any EMC");
            BlockEntityCollector loaded = (BlockEntityCollector) reload(collector, collector.getBlockState());
            check(loaded.getStoredEmcBigInteger().equals(collector.getStoredEmcBigInteger()), "Stored EMC changed from " + collector.getStoredEmcBigInteger() + " to " + loaded.getStoredEmcBigInteger());
        });
    }

    @IntegrationTest("empty")
    public void condenserKeepsLock(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Blocks.CONDENSER_MK3.get());
        TileEntity condenser = Objects.requireNonNull(helper.getTileEntity(pos));
        CompoundNBT lock = ItemInfo.fromItem(Items.DIAMOND).write(new CompoundNBT());
        CompoundNBT tag = condenser.save(new CompoundNBT());
        tag.getCompound("Up").put("Lock", lock);
        BlockState state = condenser.getBlockState();
        TileEntity edited = TileEntity.loadStatic(state, tag);
        check(edited != null, "Couldn't load the condenser");
        CompoundNBT saved = reload(edited, state).save(new CompoundNBT());
        check(lock.equals(saved.getCompound("Up").getCompound("Lock")), "The condenser's lock wasn't kept: " + saved.getCompound("Up"));
    }

    // data written before a field existed, or by hand, won't have it
    @IntegrationTest("empty")
    public void relayLoadsWithoutStoredEmc(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getRelay()));
        BlockEntityRelay relay = blockEntity(helper, pos, BlockEntityRelay.class);
        CompoundNBT tag = relay.save(new CompoundNBT());
        tag.remove(TagNames.STORED_EMC);
        tag.remove(TagNames.BONUS_EMC);
        TileEntity loaded = TileEntity.loadStatic(relay.getBlockState(), tag);
        check(loaded instanceof BlockEntityRelay && ((BlockEntityRelay) loaded).getStoredEmcBigInteger().signum() == 0, "A relay saved without its EMC didn't load");
    }
}
