package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.PowerFlowerMode;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.block.Block;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emc;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.knowledge;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;

// the matter upgrader, which swaps a block for its next tier
// creative players skip the cost & knowledge checks, so they're used where those don't matter
@IntegrationTestClass("empty")
public class UpgraderTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static ActionResultType upgrade(IntegrationTestHelper helper, ServerPlayerEntity player) {
        ItemStack upgrader = new ItemStack(Items.MATTER_UPGRADER.get());
        player.setItemInHand(Hand.MAIN_HAND, upgrader);
        TileEntity target = Objects.requireNonNull(helper.getTileEntity(POS), "No block entity at " + POS);
        BlockPos pos = target.getBlockPos();
        ItemUseContext context = new ItemUseContext(player, Hand.MAIN_HAND, new BlockRayTraceResult(Vector3d.atCenterOf(pos), Direction.UP, pos, false));
        return upgrader.getItem().onItemUseFirst(upgrader, context);
    }

    private static ServerPlayerEntity creativePlayer(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        player.setGameMode(GameType.CREATIVE);
        return player;
    }

    // the server's default game mode isn't certain, so it's set either way
    private static ServerPlayerEntity survivalPlayer(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void checkBlock(IntegrationTestHelper helper, Block block) {
        check(helper.getBlockState(POS).is(block), "Found " + helper.getBlockState(POS).getBlock() + " instead of " + block);
    }

    // also checks the upgrader's message doesn't need the client config, which a dedicated server never loads
    @IntegrationTest("empty")
    public void upgraderKeepsCollectorEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        helper.setBlockState(POS, Objects.requireNonNull(Matter.BASIC.getCollector()).defaultBlockState());
        helper.runAfter(40, () -> {
            try {
                BigInteger stored = blockEntity(helper, POS, BlockEntityCollector.class).getStoredEmcBigInteger();
                check(stored.signum() > 0, "The collector didn't generate any EMC");
                check(upgrade(helper, player) == ActionResultType.SUCCESS, "Upgrading the collector didn't succeed");
                checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getCollector()));
                BigInteger after = blockEntity(helper, POS, BlockEntityCollector.class).getStoredEmcBigInteger();
                check(after.compareTo(stored) >= 0, "The collector had " + stored + " EMC, but only " + after + " after upgrading");
            } finally {
                removePlayer(player);
            }
        });
    }

    @IntegrationTest("empty")
    public void upgraderKeepsRelayEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        try {
            helper.setBlockState(POS, Objects.requireNonNull(Matter.BASIC.getRelay()).defaultBlockState());
            BigInteger stored = blockEntity(helper, POS, BlockEntityRelay.class).insertEmcBigInteger(BigInteger.valueOf(1000), IEmcStorage.EmcAction.EXECUTE);
            check(stored.signum() > 0, "The relay didn't take any EMC");
            check(upgrade(helper, player) == ActionResultType.SUCCESS, "Upgrading the relay didn't succeed");
            BigInteger after = blockEntity(helper, POS, BlockEntityRelay.class).getStoredEmcBigInteger();
            check(after.compareTo(stored) >= 0, "The relay had " + stored + " EMC, but only " + after + " after upgrading");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderUpgradesCollector(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        try {
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
            check(upgrade(helper, player) == ActionResultType.SUCCESS, "Upgrading the collector didn't succeed");
            checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getCollector()));
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderStopsAtFinal(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        try {
            setBlock(helper, POS, Objects.requireNonNull(Matter.FINAL.getCollector()));
            check(upgrade(helper, player) == ActionResultType.FAIL, "A final collector was upgraded");
            checkBlock(helper, Objects.requireNonNull(Matter.FINAL.getCollector()));
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderKeepsPowerFlower(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        try {
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
            BlockEntityPowerFlower flower = blockEntity(helper, POS, BlockEntityPowerFlower.class);
            flower.setOwner(player);
            flower.emc = BigInteger.valueOf(1000);
            check(upgrade(helper, player) == ActionResultType.SUCCESS, "Upgrading the power flower didn't succeed");
            checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getPowerFlower()));
            BlockEntityPowerFlower upgraded = blockEntity(helper, POS, BlockEntityPowerFlower.class);
            check(player.getUUID().equals(upgraded.owner), "The power flower lost its owner");
            check(upgraded.emc.equals(BigInteger.valueOf(1000)), "The power flower had 1000 EMC, but " + upgraded.emc + " after upgrading");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderKeepsEmcLink(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        try {
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
            BlockEntityEMCLink link = blockEntity(helper, POS, BlockEntityEMCLink.class);
            link.setOwner(player);
            link.itemStack = new ItemStack(net.minecraft.item.Items.DIAMOND);
            check(upgrade(helper, player) == ActionResultType.SUCCESS, "Upgrading the EMC link didn't succeed");
            checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getEMCLink()));
            BlockEntityEMCLink upgraded = blockEntity(helper, POS, BlockEntityEMCLink.class);
            check(player.getUUID().equals(upgraded.owner), "The EMC link lost its owner");
            check(upgraded.itemStack.getItem() == net.minecraft.item.Items.DIAMOND, "The EMC link lost its item, it has " + upgraded.itemStack);
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderNeedsOwner(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        try {
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
            blockEntity(helper, POS, BlockEntityPowerFlower.class).owner = UUID.randomUUID();
            check(upgrade(helper, player) == ActionResultType.FAIL, "Someone else's power flower was upgraded");
            checkBlock(helper, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        } finally {
            removePlayer(player);
        }
    }

    // the rest pay for the upgrade
    private static long upgradeCost() {
        long cost = emcValue(Objects.requireNonNull(Matter.BASIC.next().getCollectorItem())) - emcValue(Objects.requireNonNull(Matter.BASIC.getCollectorItem()));
        check(cost > 0, "Upgrading a collector costs " + cost + " EMC");
        return cost;
    }

    @IntegrationTest("empty")
    public void upgraderChargesDifference(IntegrationTestHelper helper) {
        ServerPlayerEntity player = survivalPlayer(helper);
        try {
            long cost = upgradeCost();
            knowledge(player).addKnowledge(ItemInfo.fromItem(Objects.requireNonNull(Matter.BASIC.next().getCollectorItem())));
            knowledge(player).setEmc(BigInteger.valueOf(cost));
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
            check(upgrade(helper, player) == ActionResultType.SUCCESS, "Upgrading the collector didn't succeed");
            checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getCollector()));
            check(emc(player).signum() == 0, "The player has " + emc(player) + " EMC left instead of 0");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderNeedsEnoughEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = survivalPlayer(helper);
        try {
            long cost = upgradeCost();
            knowledge(player).addKnowledge(ItemInfo.fromItem(Objects.requireNonNull(Matter.BASIC.next().getCollectorItem())));
            knowledge(player).setEmc(BigInteger.valueOf(cost - 1));
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
            check(upgrade(helper, player) == ActionResultType.FAIL, "The collector was upgraded without enough EMC");
            checkBlock(helper, Objects.requireNonNull(Matter.BASIC.getCollector()));
            check(emc(player).longValueExact() == cost - 1, "The player was charged for a failed upgrade");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderNeedsKnowledge(IntegrationTestHelper helper) {
        ServerPlayerEntity player = survivalPlayer(helper);
        try {
            knowledge(player).setEmc(BigInteger.valueOf(upgradeCost() * 2));
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
            check(upgrade(helper, player) == ActionResultType.FAIL, "The collector was upgraded without knowing the next tier");
            checkBlock(helper, Objects.requireNonNull(Matter.BASIC.getCollector()));
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void upgraderKeepsPowerFlowerMode(IntegrationTestHelper helper) {
        ServerPlayerEntity player = creativePlayer(helper);
        try {
            setBlock(helper, POS, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
            BlockEntityPowerFlower flower = blockEntity(helper, POS, BlockEntityPowerFlower.class);
            flower.setOwner(player);
            flower.setMode(PowerFlowerMode.STORAGE);
            check(upgrade(helper, player) == ActionResultType.SUCCESS, "Upgrading the power flower didn't succeed");
            PowerFlowerMode mode = blockEntity(helper, POS, BlockEntityPowerFlower.class).getMode();
            check(mode == PowerFlowerMode.STORAGE, "The power flower was in storage mode, but " + mode + " after upgrading");
        } finally {
            removePlayer(player);
        }
    }
}
