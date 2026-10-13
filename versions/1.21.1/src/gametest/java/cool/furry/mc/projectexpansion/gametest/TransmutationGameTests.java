package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityTransmutationInterface;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.EmcLock;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.emc;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.knowledge;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

// the emc link & transmutation interface, which both trade with their owner's transmutation
// in the emc batch, as only items with emc can go through them
@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransmutationGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static IItemHandler items(GameTestHelper helper) {
        return Objects.requireNonNull(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(POS), null), "No item handler at " + POS);
    }

    // placing it like a player would sets the owner & the per second limits
    private static BlockEntityEMCLink link(GameTestHelper helper, Matter matter, ServerPlayer owner) {
        helper.setBlock(POS, Objects.requireNonNull(matter.getEMCLink()));
        BlockEntityEMCLink link = blockEntity(helper, POS, BlockEntityEMCLink.class);
        link.handlePlace(owner, ItemStack.EMPTY);
        return link;
    }

    private static BlockEntityTransmutationInterface transmutationInterface(GameTestHelper helper, ServerPlayer owner) {
        helper.setBlock(POS, Blocks.TRANSMUTATION_INTERFACE.get());
        BlockEntityTransmutationInterface be = blockEntity(helper, POS, BlockEntityTransmutationInterface.class);
        be.handlePlace(owner, ItemStack.EMPTY);
        return be;
    }

    private static long sellValue(Item item) {
        return IEMCProxy.INSTANCE.getSellValue(new ItemStack(item));
    }

    // the nbt filter turns away anything projecte wouldn't keep, like a custom name
    private static ItemStack named(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
        ItemInfo info = ItemInfo.fromStack(stack);
        check(!IEMCProxy.INSTANCE.getPersistentInfo(info).equals(info), "ProjectE keeps custom names, so they can't be used to test the filter");
        return stack;
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void emcLinkSellsItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        link(helper, Matter.FINAL, player);
        ItemStack left = items(helper).insertItem(1, new ItemStack(Items.DIAMOND, 4), false);
        check(left.isEmpty(), "The link gave back " + left);
        check(emc(player).longValueExact() == sellValue(Items.DIAMOND) * 4, "The owner has " + emc(player) + " EMC instead of " + sellValue(Items.DIAMOND) * 4);
        check(knowledge(player).hasKnowledge(ItemInfo.fromItem(Items.DIAMOND)), "The owner didn't learn diamonds");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void emcLinkBuysItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockEntityEMCLink link = link(helper, Matter.FINAL, player);
        link.itemStack = new ItemStack(Items.DIAMOND);
        knowledge(player).setEmc(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(Items.DIAMOND) * 3));
        ItemStack out = items(helper).extractItem(0, 64, false);
        check(out.is(Items.DIAMOND) && out.getCount() == 3, "Got " + out + " instead of 3 diamonds");
        check(emc(player).signum() == 0, "The owner has " + emc(player) + " EMC left over");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void emcLinkLimitsImports(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        link(helper, Matter.BASIC, player);
        int limit = Matter.BASIC.getEMCLinkItemLimit();
        check(limit < 64, "A basic link can import " + limit + " items a second, so a stack doesn't test the limit");
        ItemStack left = items(helper).insertItem(1, new ItemStack(Items.COBBLESTONE, 64), false);
        check(left.getCount() == 64 - limit, "The link took " + (64 - left.getCount()) + " cobblestone instead of " + limit);
        ItemStack again = items(helper).insertItem(1, new ItemStack(Items.COBBLESTONE), false);
        check(again.getCount() == 1, "The link took more cobblestone after reaching its limit");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void emcLinkLimitsExports(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockEntityEMCLink link = link(helper, Matter.BASIC, player);
        link.itemStack = new ItemStack(Items.COBBLESTONE);
        int limit = Matter.BASIC.getEMCLinkItemLimit();
        knowledge(player).setEmc(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(Items.COBBLESTONE) * 64));
        ItemStack out = items(helper).extractItem(0, 64, false);
        check(out.getCount() == limit, "Got " + out.getCount() + " cobblestone instead of " + limit);
        check(items(helper).extractItem(0, 64, false).isEmpty(), "The link gave out more cobblestone after reaching its limit");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void emcLinkFilterKeepsRejectedItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockEntityEMCLink link = link(helper, Matter.FINAL, player);
        check(link.getFilterStatus(), "The link's nbt filter isn't on by default");
        ItemStack left = items(helper).insertItem(1, named(Items.COBBLESTONE, 16), false);
        check(emc(player).signum() == 0, "The filter let named cobblestone through");
        check(left.getCount() == 16, "The filter turned away 16 named cobblestone, but only gave back " + left.getCount());
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void emcLinkNeedsOnlineOwner(GameTestHelper helper) {
        helper.setBlock(POS, Objects.requireNonNull(Matter.FINAL.getEMCLink()));
        BlockEntityEMCLink link = blockEntity(helper, POS, BlockEntityEMCLink.class);
        link.handlePlace(null, ItemStack.EMPTY);
        link.owner = UUID.randomUUID();
        link.itemStack = new ItemStack(Items.COBBLESTONE);
        ItemStack left = items(helper).insertItem(1, new ItemStack(Items.DIAMOND, 4), false);
        check(left.getCount() == 4, "The link took items for an offline owner");
        check(items(helper).extractItem(0, 64, false).isEmpty(), "The link gave out items for an offline owner");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void emcLinkFillsBuckets(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockEntityEMCLink link = link(helper, Matter.FINAL, player);
        // water is worth nothing on top of its bucket, so it's free
        check(IEMCProxy.INSTANCE.getValue(Items.LAVA_BUCKET) > IEMCProxy.INSTANCE.getValue(Items.BUCKET), "Lava is worth nothing on top of its bucket");
        link.itemStack = new ItemStack(Items.LAVA_BUCKET);
        BigInteger start = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(Items.LAVA_BUCKET) * 2);
        knowledge(player).setEmc(start);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        link.handleActivation(player, InteractionHand.MAIN_HAND);
        check(player.getInventory().countItem(Items.LAVA_BUCKET) == 1, "The player didn't get a lava bucket");
        check(player.getInventory().countItem(Items.BUCKET) == 0, "The empty bucket wasn't used up");
        check(emc(player).compareTo(start) < 0, "Filling the bucket was free");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void interfaceSellsItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        transmutationInterface(helper, player);
        ItemStack left = items(helper).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        check(left.isEmpty(), "The interface gave back " + left);
        check(emc(player).longValueExact() == sellValue(Items.DIAMOND) * 4, "The owner has " + emc(player) + " EMC instead of " + sellValue(Items.DIAMOND) * 4);
        check(knowledge(player).hasKnowledge(ItemInfo.fromItem(Items.DIAMOND)), "The owner didn't learn diamonds");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void interfaceBuysKnownItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        knowledge(player).addKnowledge(ItemInfo.fromItem(Items.DIAMOND));
        knowledge(player).setEmc(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(Items.DIAMOND) * 2));
        transmutationInterface(helper, player);
        IItemHandler items = items(helper);
        int slot = -1;
        for (int i = 1; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(Items.DIAMOND)) slot = i;
        }
        check(slot != -1, "The interface doesn't list diamonds");
        check(items.getStackInSlot(slot).getCount() == 2, "The interface lists " + items.getStackInSlot(slot).getCount() + " diamonds instead of 2");
        ItemStack out = items.extractItem(slot, 64, false);
        check(out.is(Items.DIAMOND) && out.getCount() == 2, "Got " + out + " instead of 2 diamonds");
        check(emc(player).signum() == 0, "The owner has " + emc(player) + " EMC left over");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void interfaceRefusesLockedItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        transmutationInterface(helper, player);
        ItemStack locked = new ItemStack(Items.DIAMOND, 4);
        EmcLock.toggle(locked);
        ItemStack left = items(helper).insertItem(0, locked, false);
        check(left.getCount() == 4, "The interface took EMC locked diamonds");
        check(emc(player).signum() == 0, "The owner got EMC for locked diamonds");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void interfaceFilterKeepsRejectedItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockEntityTransmutationInterface be = transmutationInterface(helper, player);
        check(be.getFilterStatus(), "The interface's nbt filter isn't on by default");
        ItemStack left = items(helper).insertItem(0, named(Items.COBBLESTONE, 16), false);
        check(emc(player).signum() == 0, "The filter let named cobblestone through");
        check(left.getCount() == 16, "The filter turned away 16 named cobblestone, but only gave back " + left.getCount());
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void interfaceNeedsOnlineOwner(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.TRANSMUTATION_INTERFACE.get());
        blockEntity(helper, POS, BlockEntityTransmutationInterface.class).owner = UUID.randomUUID();
        ItemStack left = items(helper).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        check(left.getCount() == 4, "The interface took items for an offline owner");
        helper.succeed();
    }
}
