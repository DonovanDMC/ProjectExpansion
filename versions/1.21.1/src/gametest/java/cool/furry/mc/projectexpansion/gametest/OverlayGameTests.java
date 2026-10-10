package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.integrations.EmcServerData;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class OverlayGameTests {
    // projecte's condenser never syncs its emc to the client, so jade and wthit need it sent from the server
    @GameTest(template = EMPTY)
    public static void condenserEmcSentFromServer(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("projecte", "condenser_mk1")));
        BlockPos absolute = helper.absolutePos(pos);
        BlockEntity condenser = Objects.requireNonNull(helper.getLevel().getBlockEntity(absolute), "The condenser has no block entity");
        IEmcStorage storage = Objects.requireNonNull(helper.getLevel().getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, absolute, null), "The condenser stores no emc");
        // condensers only take emc from items, so it's loaded in the way projecte saves it
        CompoundTag saved = condenser.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putLong("emc", 12_345);
        condenser.loadWithComponents(saved, helper.getLevel().registryAccess());
        check(storage.getStoredEmc() == 12_345, "The condenser holds " + storage.getStoredEmc() + " emc instead of 12,345");

        CompoundTag data = new CompoundTag();
        EmcServerData.append(data, helper.getLevel(), absolute, condenser, null);
        check(EmcServerData.has(data), "No emc was written for the condenser");
        check(EmcServerData.stored(data).longValueExact() == storage.getStoredEmc(), "Sent " + EmcServerData.stored(data) + " emc instead of " + storage.getStoredEmc());
        check(EmcServerData.maximum(data).longValueExact() == storage.getMaximumEmc(), "Sent the wrong maximum emc");
        helper.succeed();
    }
}
