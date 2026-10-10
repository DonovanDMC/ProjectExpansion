package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;

// block entity data has to survive being saved and loaded again
@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class PersistenceGameTests {
    private static BlockEntity reload(BlockEntity blockEntity) {
        HolderLookup.Provider registries = Objects.requireNonNull(blockEntity.getLevel()).registryAccess();
        BlockEntity loaded = BlockEntity.loadStatic(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.saveWithFullMetadata(registries), registries);
        check(loaded != null, "Couldn't load " + blockEntity);
        return loaded;
    }

    @GameTest(template = EMPTY)
    public static void collectorKeepsEmc(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Objects.requireNonNull(Matter.FINAL.getCollector()));
        helper.succeedWhen(() -> {
            BlockEntityCollector collector = blockEntity(helper, pos, BlockEntityCollector.class);
            check(collector.getStoredEmcBigInteger().signum() > 0, "The collector didn't generate any EMC");
            BlockEntityCollector loaded = (BlockEntityCollector) reload(collector);
            check(loaded.getStoredEmcBigInteger().equals(collector.getStoredEmcBigInteger()), "Stored EMC changed from " + collector.getStoredEmcBigInteger() + " to " + loaded.getStoredEmcBigInteger());
        });
    }

    @GameTest(template = EMPTY)
    public static void condenserKeepsLock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CONDENSER_MK3.get());
        BlockEntity condenser = Objects.requireNonNull(helper.getBlockEntity(pos));
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        Tag lock = ItemInfo.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), ItemInfo.fromItem(Items.DIAMOND)).getOrThrow();
        CompoundTag tag = condenser.saveWithFullMetadata(registries);
        tag.getCompound("Up").put("Lock", lock);
        BlockEntity edited = BlockEntity.loadStatic(condenser.getBlockPos(), condenser.getBlockState(), tag, registries);
        check(edited != null, "Couldn't load the condenser");
        edited.setLevel(helper.getLevel());
        CompoundTag saved = reload(edited).saveWithFullMetadata(registries);
        check(lock.equals(saved.getCompound("Up").getCompound("Lock")), "The condenser's lock wasn't kept: " + saved.getCompound("Up"));
        helper.succeed();
    }
}
