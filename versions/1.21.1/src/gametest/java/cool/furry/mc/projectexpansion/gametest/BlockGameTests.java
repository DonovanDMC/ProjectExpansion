package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.gui.container.ContainerCondenserMK3Input;
import cool.furry.mc.projectexpansion.gui.container.ContainerCondenserMK3Output;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Objects;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlockGameTests {
    private static IItemHandler items(GameTestHelper helper, BlockPos pos, Direction side) {
        return Objects.requireNonNull(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side), "No item handler at " + pos);
    }

    private static int count(IItemHandler handler, Item item) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).is(item)) count += handler.getStackInSlot(i).getCount();
        }
        return count;
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void condenserMk3Condenses(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CONDENSER_MK3.get());
        blockEntity(helper, pos, BlockEntityCondenserMK3.class).getSidedHandler(Direction.UP).setLockInfoFromPacket(ItemInfo.fromItem(Items.IRON_INGOT));
        ItemStack left = items(helper, pos, Direction.UP).insertItem(0, new ItemStack(Items.DIAMOND), false);
        check(left.isEmpty(), "The condenser didn't take the diamond");
        helper.succeedWhen(() -> check(count(items(helper, pos, Direction.DOWN), Items.IRON_INGOT) > 0, "The condenser didn't make any iron, or it can't be taken from the bottom"));
    }

    // each side has its own lock, but they all output to the bottom
    @GameTest(template = EMPTY, batch = "emc")
    public static void condenserMk3SidesAreSeparate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CONDENSER_MK3.get());
        BlockEntityCondenserMK3 condenser = blockEntity(helper, pos, BlockEntityCondenserMK3.class);
        condenser.getSidedHandler(Direction.UP).setLockInfoFromPacket(ItemInfo.fromItem(Items.IRON_INGOT));
        condenser.getSidedHandler(Direction.NORTH).setLockInfoFromPacket(ItemInfo.fromItem(Items.GOLD_INGOT));
        items(helper, pos, Direction.NORTH).insertItem(0, new ItemStack(Items.DIAMOND), false);
        helper.succeedWhen(() -> {
            IItemHandler output = items(helper, pos, Direction.DOWN);
            check(count(output, Items.GOLD_INGOT) > 0, "The north side didn't make any gold");
            check(count(output, Items.IRON_INGOT) == 0, "The top side made iron from the north side's diamond");
        });
    }

    @GameTest(template = EMPTY)
    public static void compactSunBoostsCollector(GameTestHelper helper) {
        BlockPos plain = new BlockPos(1, 1, 1), boosted = new BlockPos(3, 1, 1);
        helper.setBlock(plain, Objects.requireNonNull(Matter.BASIC.getCollector()));
        helper.setBlock(boosted, Objects.requireNonNull(Matter.BASIC.getCollector()));
        helper.setBlock(boosted.above(), Blocks.COMPACT_SUN.get());
        BlockEntityCollector without = blockEntity(helper, plain, BlockEntityCollector.class), with = blockEntity(helper, boosted, BlockEntityCollector.class);
        check(!without.hasSunBonus(), "A collector without a compact sun has the sun bonus");
        check(with.hasSunBonus(), "A collector under a compact sun doesn't have the sun bonus");
        check(with.getGeneratedEMC().compareTo(without.getGeneratedEMC()) > 0, "A collector under a compact sun makes " + with.getGeneratedEMC() + " EMC, no more than one without (" + without.getGeneratedEMC() + ")");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void compactSunBoostsPowerFlower(GameTestHelper helper) {
        BlockPos plain = new BlockPos(1, 2, 1), boosted = new BlockPos(3, 2, 1);
        helper.setBlock(plain, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        helper.setBlock(boosted, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        helper.setBlock(boosted.below(), Blocks.COMPACT_SUN.get());
        BlockEntityPowerFlower without = blockEntity(helper, plain, BlockEntityPowerFlower.class), with = blockEntity(helper, boosted, BlockEntityPowerFlower.class);
        check(!without.hasSunBonus(), "A power flower without a compact sun has the sun bonus");
        check(with.hasSunBonus(), "A power flower on a compact sun doesn't have the sun bonus");
        check(with.getGeneratedEMC().compareTo(without.getGeneratedEMC()) > 0, "A power flower on a compact sun makes " + with.getGeneratedEMC() + " EMC, no more than one without (" + without.getGeneratedEMC() + ")");
        helper.succeed();
    }

    private static IItemHandler bag(ServerPlayer player, DyeColor color) {
        IAlchBagProvider provider = Objects.requireNonNull(player.getCapability(PECapabilities.ALCH_BAG_CAPABILITY), "No alchemical bags for " + player.getScoreboardName());
        return provider.getBag(color);
    }

    private static BlockEntityAdvancedAlchemicalChest chest(GameTestHelper helper, BlockPos pos, DyeColor color, ServerPlayer owner) {
        helper.setBlock(pos, AdvancedAlchemicalChest.getBlock(color));
        BlockEntityAdvancedAlchemicalChest chest = blockEntity(helper, pos, BlockEntityAdvancedAlchemicalChest.class);
        chest.setOwner(owner);
        return chest;
    }

    @GameTest(template = EMPTY)
    public static void advancedChestUsesOwnersBag(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper);
        chest(helper, pos, DyeColor.WHITE, player);
        ItemStack left = items(helper, pos, Direction.UP).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        check(left.isEmpty(), "The chest gave back " + left);
        check(count(bag(player, DyeColor.WHITE), Items.DIAMOND) == 4, "The diamonds didn't end up in the owner's white bag");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void advancedChestTakesBagColor(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper);
        chest(helper, pos, DyeColor.WHITE, player).handleItemActivation(player, new ItemStack(PEItems.getBag(DyeColor.RED)));
        check(helper.getBlockState(pos).is(AdvancedAlchemicalChest.getBlock(DyeColor.RED)), "The chest didn't turn red");
        BlockEntityAdvancedAlchemicalChest chest = blockEntity(helper, pos, BlockEntityAdvancedAlchemicalChest.class);
        check(player.getUUID().equals(chest.owner), "The chest lost its owner");
        check(chest.getColor() == DyeColor.RED, "The chest's block entity is " + chest.getColor() + " instead of red");
        items(helper, pos, Direction.UP).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        check(count(bag(player, DyeColor.RED), Items.DIAMOND) == 4, "The diamonds didn't end up in the owner's red bag");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void advancedChestNeedsOnlineOwner(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, AdvancedAlchemicalChest.getBlock(DyeColor.WHITE));
        blockEntity(helper, pos, BlockEntityAdvancedAlchemicalChest.class).owner = UUID.randomUUID();
        ItemStack left = items(helper, pos, Direction.UP).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        check(left.getCount() == 4, "The chest took items for an offline owner");
        helper.succeed();
    }

    // a lock with no emc never gets its cost paid, it used to fill the output for free instead
    @GameTest(template = EMPTY, batch = "emc")
    public static void condenserMk3IgnoresLockWithoutEmc(GameTestHelper helper) {
        check(!IEMCProxy.INSTANCE.hasValue(Items.BEDROCK), "Bedrock has EMC, so it can't be used to test this");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CONDENSER_MK3.get());
        blockEntity(helper, pos, BlockEntityCondenserMK3.class).getSidedHandler(Direction.UP).setLockInfoFromPacket(ItemInfo.fromItem(Items.BEDROCK));
        helper.runAfterDelay(5, () -> {
            int made = count(items(helper, pos, Direction.DOWN), Items.BEDROCK);
            check(made == 0, "The condenser made " + made + " bedrock without any EMC");
            helper.succeed();
        });
    }

    private static void useCondenser(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
        BlockPos abs = helper.absolutePos(pos);
        try {
            helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(abs), side, abs, false));
        } catch (RuntimeException | LinkageError e) {
            throw new GameTestAssertException("Opening the condenser from " + side + " threw " + e);
        }
    }

    // which menu opens used to come from the client's crosshair, which a dedicated server doesn't have
    @GameTest(template = EMPTY)
    public static void condenserMk3OpensOnServers(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CONDENSER_MK3.get());
        try {
            useCondenser(helper, player, pos, Direction.UP);
            check(player.containerMenu instanceof ContainerCondenserMK3Input, "Opening it from the top gave " + player.containerMenu);
            player.closeContainer();
            useCondenser(helper, player, pos, Direction.DOWN);
            check(player.containerMenu instanceof ContainerCondenserMK3Output, "Opening it from the bottom gave " + player.containerMenu);
            player.closeContainer();
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    // right clicks the chest with a bag, the same as a player looking at it would
    private static void clickWithBag(GameTestHelper helper, ServerPlayer player, BlockPos chest, DyeColor color, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.setPose(sneaking ? Pose.CROUCHING : Pose.STANDING);
        ItemStack bag = new ItemStack(PEItems.getBag(color));
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        BlockPos abs = helper.absolutePos(chest);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        helper.getLevel().getBlockState(abs).useItemOn(bag, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    // the tooltip says shift click, which is the only way to set the color
    @GameTest(template = EMPTY)
    public static void advancedChestTakesBagColorWhenSneaking(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper);
        chest(helper, pos, DyeColor.WHITE, player);
        clickWithBag(helper, player, pos, DyeColor.RED, true);
        check(helper.getBlockState(pos).is(AdvancedAlchemicalChest.getBlock(DyeColor.RED)), "Sneak clicking the chest with a red bag didn't turn it red");
        removePlayer(player);
        helper.succeed();
    }

    // without sneaking a click opens the chest instead
    @GameTest(template = EMPTY)
    public static void advancedChestKeepsColorWithoutSneaking(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper);
        chest(helper, pos, DyeColor.WHITE, player);
        clickWithBag(helper, player, pos, DyeColor.RED, false);
        check(helper.getBlockState(pos).is(AdvancedAlchemicalChest.getBlock(DyeColor.WHITE)), "Clicking the chest with a red bag without sneaking recolored it");
        removePlayer(player);
        helper.succeed();
    }
}
