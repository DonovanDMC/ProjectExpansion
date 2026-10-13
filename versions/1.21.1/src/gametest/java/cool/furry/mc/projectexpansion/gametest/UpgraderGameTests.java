package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.emc;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.knowledge;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

// the matter upgrader, which swaps a block for its next tier
// creative players skip the cost & knowledge checks, so they're used where those don't matter
@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class UpgraderGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static InteractionResult upgrade(GameTestHelper helper, ServerPlayer player) {
        ItemStack upgrader = new ItemStack(Items.MATTER_UPGRADER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, upgrader);
        BlockPos pos = helper.absolutePos(POS);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        // anything thrown here would crash a real server, so it fails the test instead of the whole run
        try {
            return upgrader.getItem().onItemUseFirst(upgrader, context);
        } catch (RuntimeException e) {
            throw new GameTestAssertException("Using the upgrader threw " + e);
        }
    }

    private static ServerPlayer creativePlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.setGameMode(GameType.CREATIVE);
        return player;
    }

    // the game test level defaults to creative
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void checkBlock(GameTestHelper helper, Block block) {
        check(helper.getBlockState(POS).is(block), "Found " + helper.getBlockState(POS).getBlock() + " instead of " + block);
    }

    @GameTest(template = EMPTY)
    public static void upgraderUpgradesCollector(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
        check(upgrade(helper, player) == InteractionResult.SUCCESS, "Upgrading the collector didn't succeed");
        checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getCollector()));
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgraderStopsAtFinal(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        helper.setBlock(POS, Objects.requireNonNull(Matter.FINAL.getCollector()));
        check(upgrade(helper, player) == InteractionResult.FAIL, "A final collector was upgraded");
        checkBlock(helper, Objects.requireNonNull(Matter.FINAL.getCollector()));
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgraderKeepsCollectorEmc(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
        AtomicReference<BigInteger> stored = new AtomicReference<>();
        helper.startSequence()
            .thenWaitUntil(() -> check(blockEntity(helper, POS, BlockEntityCollector.class).getStoredEmcBigInteger().signum() > 0, "The collector didn't generate any EMC"))
            .thenExecute(() -> {
                stored.set(blockEntity(helper, POS, BlockEntityCollector.class).getStoredEmcBigInteger());
                check(upgrade(helper, player) == InteractionResult.SUCCESS, "Upgrading the collector didn't succeed");
                BigInteger after = blockEntity(helper, POS, BlockEntityCollector.class).getStoredEmcBigInteger();
                check(after.compareTo(stored.get()) >= 0, "The collector had " + stored.get() + " EMC, but only " + after + " after upgrading");
            })
            .thenExecute(() -> removePlayer(player))
            .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void upgraderKeepsRelayEmc(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getRelay()));
        BigInteger stored = blockEntity(helper, POS, BlockEntityRelay.class).insertEmcBigInteger(BigInteger.valueOf(1000), IEmcStorage.EmcAction.EXECUTE);
        check(stored.signum() > 0, "The relay didn't take any EMC");
        check(upgrade(helper, player) == InteractionResult.SUCCESS, "Upgrading the relay didn't succeed");
        BigInteger after = blockEntity(helper, POS, BlockEntityRelay.class).getStoredEmcBigInteger();
        check(after.compareTo(stored) >= 0, "The relay had " + stored + " EMC, but only " + after + " after upgrading");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgraderKeepsPowerFlower(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        BlockEntityPowerFlower flower = blockEntity(helper, POS, BlockEntityPowerFlower.class);
        flower.setOwner(player);
        flower.emc = BigInteger.valueOf(1000);
        check(upgrade(helper, player) == InteractionResult.SUCCESS, "Upgrading the power flower didn't succeed");
        checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getPowerFlower()));
        BlockEntityPowerFlower upgraded = blockEntity(helper, POS, BlockEntityPowerFlower.class);
        check(player.getUUID().equals(upgraded.owner), "The power flower lost its owner");
        check(upgraded.emc.equals(BigInteger.valueOf(1000)), "The power flower had 1000 EMC, but " + upgraded.emc + " after upgrading");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgraderKeepsEmcLink(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        BlockEntityEMCLink link = blockEntity(helper, POS, BlockEntityEMCLink.class);
        link.setOwner(player);
        link.itemStack = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
        check(upgrade(helper, player) == InteractionResult.SUCCESS, "Upgrading the EMC link didn't succeed");
        checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getEMCLink()));
        BlockEntityEMCLink upgraded = blockEntity(helper, POS, BlockEntityEMCLink.class);
        check(player.getUUID().equals(upgraded.owner), "The EMC link lost its owner");
        check(upgraded.itemStack.is(net.minecraft.world.item.Items.DIAMOND), "The EMC link lost its item, it has " + upgraded.itemStack);
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgraderNeedsOwner(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        blockEntity(helper, POS, BlockEntityPowerFlower.class).owner = UUID.randomUUID();
        check(upgrade(helper, player) == InteractionResult.FAIL, "Someone else's power flower was upgraded");
        checkBlock(helper, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        removePlayer(player);
        helper.succeed();
    }

    // the rest pay for the upgrade, so they need emc values
    private static long upgradeCost() {
        long cost = IEMCProxy.INSTANCE.getValue(Objects.requireNonNull(Matter.BASIC.next().getCollectorItem())) - IEMCProxy.INSTANCE.getValue(Objects.requireNonNull(Matter.BASIC.getCollectorItem()));
        check(cost > 0, "Upgrading a collector costs " + cost + " EMC");
        return cost;
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void upgraderChargesDifference(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        long cost = upgradeCost();
        knowledge(player).addKnowledge(ItemInfo.fromItem(Objects.requireNonNull(Matter.BASIC.next().getCollectorItem())));
        knowledge(player).setEmc(BigInteger.valueOf(cost));
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
        check(upgrade(helper, player) == InteractionResult.SUCCESS, "Upgrading the collector didn't succeed");
        checkBlock(helper, Objects.requireNonNull(Matter.BASIC.next().getCollector()));
        check(emc(player).signum() == 0, "The player has " + emc(player) + " EMC left instead of 0");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void upgraderNeedsEnoughEmc(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        long cost = upgradeCost();
        knowledge(player).addKnowledge(ItemInfo.fromItem(Objects.requireNonNull(Matter.BASIC.next().getCollectorItem())));
        knowledge(player).setEmc(BigInteger.valueOf(cost - 1));
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
        check(upgrade(helper, player) == InteractionResult.FAIL, "The collector was upgraded without enough EMC");
        checkBlock(helper, Objects.requireNonNull(Matter.BASIC.getCollector()));
        check(emc(player).longValueExact() == cost - 1, "The player was charged for a failed upgrade");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void upgraderNeedsKnowledge(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        knowledge(player).setEmc(BigInteger.valueOf(upgradeCost() * 2));
        helper.setBlock(POS, Objects.requireNonNull(Matter.BASIC.getCollector()));
        check(upgrade(helper, player) == InteractionResult.FAIL, "The collector was upgraded without knowing the next tier");
        checkBlock(helper, Objects.requireNonNull(Matter.BASIC.getCollector()));
        removePlayer(player);
        helper.succeed();
    }
}
