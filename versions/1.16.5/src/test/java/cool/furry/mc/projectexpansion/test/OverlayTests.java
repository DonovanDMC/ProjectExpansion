package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.BlockEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcStorage;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.integrations.Common;
import cool.furry.mc.projectexpansion.integrations.EmcServerData;
import cool.furry.mc.projectexpansion.integrations.IDataProvider;
import cool.furry.mc.projectexpansion.integrations.top.TOPIntegration;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import cool.furry.mc.projectexpansion.util.storage.StorageSide;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;

@IntegrationTestClass("empty")
public class OverlayTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    // what the overlays would show for the block at POS, looked at from the given side
    private static List<ITextComponent> lines(IntegrationTestHelper helper, PlayerEntity player, Direction side) {
        BlockPos pos = absolute(helper, POS);
        World level = helper.getWorld();
        List<ITextComponent> lines = new ArrayList<>();
        Common.registerCommonTooltips(lines::add, new IDataProvider() {
            @Override
            public BlockPos getBlockPos() {
                return pos;
            }

            @Override
            public Block getBlock() {
                return getBlockState().getBlock();
            }

            @Override
            public BlockState getBlockState() {
                return level.getBlockState(pos);
            }

            @Override
            public TileEntity getBlockEntity() {
                return level.getBlockEntity(pos);
            }

            @Override
            public World getLevel() {
                return level;
            }

            @Override
            public PlayerEntity getPlayer() {
                return player;
            }

            @Override
            public Direction getSide() {
                return side;
            }
        });
        return lines;
    }

    private static boolean hasLine(List<ITextComponent> lines, String key) {
        return lines.stream().anyMatch(line -> line instanceof TranslationTextComponent && ((TranslationTextComponent) line).getKey().equals(key));
    }

    @IntegrationTest("empty")
    public void overlayModsAreLoaded(IntegrationTestHelper helper) {
        for (String mod : Arrays.asList("jade", TOPIntegration.MOD_ID)) check(ModList.get().isLoaded(mod), mod + " isn't loaded, so its integration wasn't tried");
    }

    // projecte's condenser never syncs its emc to the client, so jade needs it sent from the server
    @IntegrationTest("empty")
    public void condenserEmcSentFromServer(IntegrationTestHelper helper) {
        setBlock(helper, POS, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("projecte", "condenser_mk1")));
        TileEntity condenser = Objects.requireNonNull(helper.getTileEntity(POS), "The condenser has no block entity");
        IEmcStorage storage = condenser.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY).resolve().orElseThrow(() -> new AssertionError("The condenser stores no emc"));
        // condensers only take emc from items, so it's loaded in the way projecte saves it
        CompoundNBT saved = condenser.save(new CompoundNBT());
        saved.putLong("EMC", 12_345);
        condenser.load(condenser.getBlockState(), saved);
        check(storage.getStoredEmc() == 12_345, "The condenser holds " + storage.getStoredEmc() + " emc instead of 12,345");

        CompoundNBT data = new CompoundNBT();
        EmcServerData.append(data, helper.getWorld(), absolute(helper, POS), condenser, null);
        check(EmcServerData.has(data), "No emc was written for the condenser");
        check(EmcServerData.stored(data).longValueExact() == storage.getStoredEmc(), "Sent " + EmcServerData.stored(data) + " emc instead of " + storage.getStoredEmc());
        check(EmcServerData.maximum(data).longValueExact() == storage.getMaximumEmc(), "Sent the wrong maximum emc");
    }

    // an off side has no emc storage of its own, the overlay still shows what the block holds
    @IntegrationTest("empty")
    public void storageShowsEmcOnOffSides(IntegrationTestHelper helper) {
        setBlock(helper, POS, Blocks.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        blockEntity(helper, POS, BlockEntityEmcStorage.class).insertEmcBigInteger(BigInteger.valueOf(777), IEmcStorage.EmcAction.EXECUTE);
        helper.setBlockState(POS, helper.getBlockState(POS).setValue(BlockEmcStorage.SIDES.get(Direction.UP), StorageSide.OFF));
        for (Direction side : Arrays.asList(Direction.UP, Direction.NORTH)) {
            CompoundNBT data = new CompoundNBT();
            EmcServerData.append(data, helper.getWorld(), absolute(helper, POS), blockEntity(helper, POS, BlockEntityEmcStorage.class), side);
            check(EmcServerData.has(data), "No emc was written when looking at the " + side + " side");
            check(EmcServerData.stored(data).equals(BigInteger.valueOf(777)), "Sent " + EmcServerData.stored(data) + " emc for the " + side + " side instead of 777");
        }
    }

    // the one probe builds its lines on the server, where there's no client to ask who's looking
    @IntegrationTest("empty")
    public void linesBuildOnServer(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
            blockEntity(helper, POS, BlockEntityPowerFlower.class).setOwner(player);
            check(hasLine(lines(helper, player, Direction.UP), "text.projectexpansion.owner"), "No owner line for an owned power flower");

            setBlock(helper, POS, Blocks.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
            List<ITextComponent> storage = lines(helper, player, Direction.UP);
            check(hasLine(storage, "text.projectexpansion.emc_storage"), "No stored emc line for an emc storage");
            check(hasLine(storage, "text.projectexpansion.storage_side"), "No side line for an emc storage");
        } finally {
            removePlayer(player);
        }
    }
}
