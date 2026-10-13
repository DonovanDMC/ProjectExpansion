package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityTransmutationInterface;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.EmcLock;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emc;
import static cool.furry.mc.projectexpansion.test.TestUtil.knowledge;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;

// the emc link & transmutation interface, which both trade with their owner's transmutation
@IntegrationTestClass("empty")
public class TransmutationTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static IItemHandler items(IntegrationTestHelper helper) {
        return Objects.requireNonNull(helper.getTileEntity(POS), "No block entity at " + POS).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve().orElseThrow(() -> new AssertionError("No item handler at " + POS));
    }

    // placing it like a player would sets the owner & the per second limits
    private static BlockEntityEMCLink link(IntegrationTestHelper helper, Matter matter, ServerPlayerEntity owner) {
        helper.setBlockState(POS, Objects.requireNonNull(matter.getEMCLink()).defaultBlockState());
        BlockEntityEMCLink link = blockEntity(helper, POS, BlockEntityEMCLink.class);
        link.handlePlace(owner, ItemStack.EMPTY);
        return link;
    }

    private static BlockEntityTransmutationInterface transmutationInterface(IntegrationTestHelper helper, ServerPlayerEntity owner) {
        helper.setBlockState(POS, Blocks.TRANSMUTATION_INTERFACE.get().defaultBlockState());
        BlockEntityTransmutationInterface be = blockEntity(helper, POS, BlockEntityTransmutationInterface.class);
        be.handlePlace(owner, ItemStack.EMPTY);
        return be;
    }

    private static long sellValue(Item item) {
        return ProjectEAPI.getEMCProxy().getSellValue(new ItemStack(item));
    }

    // the nbt filter turns away anything projecte wouldn't keep, like a custom name
    private static ItemStack named(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.setHoverName(new StringTextComponent("Named"));
        ItemInfo info = ItemInfo.fromStack(stack);
        check(!ProjectEAPI.getEMCProxy().getPersistentInfo(info).equals(info), "ProjectE keeps custom names, so they can't be used to test the filter");
        return stack;
    }

    @IntegrationTest("empty")
    public void emcLinkSellsItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            link(helper, Matter.FINAL, player);
            ItemStack left = items(helper).insertItem(1, new ItemStack(Items.DIAMOND, 4), false);
            check(left.isEmpty(), "The link gave back " + left);
            check(emc(player).longValueExact() == sellValue(Items.DIAMOND) * 4, "The owner has " + emc(player) + " EMC instead of " + sellValue(Items.DIAMOND) * 4);
            check(knowledge(player).hasKnowledge(ItemInfo.fromItem(Items.DIAMOND)), "The owner didn't learn diamonds");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void emcLinkFilterKeepsRejectedItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            BlockEntityEMCLink link = link(helper, Matter.FINAL, player);
            check(link.getFilterStatus(), "The link's nbt filter isn't on by default");
            ItemStack left = items(helper).insertItem(1, named(Items.COBBLESTONE, 16), false);
            check(emc(player).signum() == 0, "The filter let named cobblestone through");
            check(left.getCount() == 16, "The filter turned away 16 named cobblestone, but only gave back " + left.getCount());
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void interfaceSellsItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            transmutationInterface(helper, player);
            ItemStack left = items(helper).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
            check(left.isEmpty(), "The interface gave back " + left);
            check(emc(player).longValueExact() == sellValue(Items.DIAMOND) * 4, "The owner has " + emc(player) + " EMC instead of " + sellValue(Items.DIAMOND) * 4);
            check(knowledge(player).hasKnowledge(ItemInfo.fromItem(Items.DIAMOND)), "The owner didn't learn diamonds");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void interfaceFilterKeepsRejectedItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            BlockEntityTransmutationInterface be = transmutationInterface(helper, player);
            check(be.getFilterStatus(), "The interface's nbt filter isn't on by default");
            ItemStack left = items(helper).insertItem(0, named(Items.COBBLESTONE, 16), false);
            check(emc(player).signum() == 0, "The filter let named cobblestone through");
            check(left.getCount() == 16, "The filter turned away 16 named cobblestone, but only gave back " + left.getCount());
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void emcLinkBuysItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            BlockEntityEMCLink link = link(helper, Matter.FINAL, player);
            link.itemStack = new ItemStack(Items.DIAMOND);
            knowledge(player).setEmc(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(Items.DIAMOND) * 3));
            ItemStack out = items(helper).extractItem(0, 64, false);
            check(out.getItem() == Items.DIAMOND && out.getCount() == 3, "Got " + out + " instead of 3 diamonds");
            check(emc(player).signum() == 0, "The owner has " + emc(player) + " EMC left over");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void emcLinkLimitsImports(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            link(helper, Matter.BASIC, player);
            int limit = Matter.BASIC.getEMCLinkItemLimit();
            check(limit < 64, "A basic link can import " + limit + " items a second, so a stack doesn't test the limit");
            ItemStack left = items(helper).insertItem(1, new ItemStack(Items.COBBLESTONE, 64), false);
            check(left.getCount() == 64 - limit, "The link took " + (64 - left.getCount()) + " cobblestone instead of " + limit);
            ItemStack again = items(helper).insertItem(1, new ItemStack(Items.COBBLESTONE), false);
            check(again.getCount() == 1, "The link took more cobblestone after reaching its limit");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void emcLinkLimitsExports(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            BlockEntityEMCLink link = link(helper, Matter.BASIC, player);
            link.itemStack = new ItemStack(Items.COBBLESTONE);
            int limit = Matter.BASIC.getEMCLinkItemLimit();
            knowledge(player).setEmc(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(Items.COBBLESTONE) * 64));
            ItemStack out = items(helper).extractItem(0, 64, false);
            check(out.getCount() == limit, "Got " + out.getCount() + " cobblestone instead of " + limit);
            check(items(helper).extractItem(0, 64, false).isEmpty(), "The link gave out more cobblestone after reaching its limit");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void emcLinkNeedsOnlineOwner(IntegrationTestHelper helper) {
        setBlock(helper, POS, Objects.requireNonNull(Matter.FINAL.getEMCLink()));
        BlockEntityEMCLink link = blockEntity(helper, POS, BlockEntityEMCLink.class);
        link.handlePlace(null, ItemStack.EMPTY);
        link.owner = UUID.randomUUID();
        link.itemStack = new ItemStack(Items.COBBLESTONE);
        ItemStack left = items(helper).insertItem(1, new ItemStack(Items.DIAMOND, 4), false);
        check(left.getCount() == 4, "The link took items for an offline owner");
        check(items(helper).extractItem(0, 64, false).isEmpty(), "The link gave out items for an offline owner");
    }

    @IntegrationTest("empty")
    public void emcLinkFillsBuckets(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            BlockEntityEMCLink link = link(helper, Matter.FINAL, player);
            // water is worth nothing on top of its bucket, so it's free
            check(ProjectEAPI.getEMCProxy().getValue(Items.LAVA_BUCKET) > ProjectEAPI.getEMCProxy().getValue(Items.BUCKET), "Lava is worth nothing on top of its bucket");
            link.itemStack = new ItemStack(Items.LAVA_BUCKET);
            BigInteger start = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(Items.LAVA_BUCKET) * 2);
            knowledge(player).setEmc(start);
            player.setItemInHand(Hand.MAIN_HAND, new ItemStack(Items.BUCKET));
            link.handleActivation(player, Hand.MAIN_HAND);
            check(player.inventory.countItem(Items.LAVA_BUCKET) == 1, "The player didn't get a lava bucket");
            check(player.inventory.countItem(Items.BUCKET) == 0, "The empty bucket wasn't used up");
            check(emc(player).compareTo(start) < 0, "Filling the bucket was free");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void interfaceBuysKnownItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            knowledge(player).addKnowledge(ItemInfo.fromItem(Items.DIAMOND));
            knowledge(player).setEmc(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(Items.DIAMOND) * 2));
            transmutationInterface(helper, player);
            IItemHandler items = items(helper);
            int slot = -1;
            for (int i = 1; i < items.getSlots(); i++) {
                if (items.getStackInSlot(i).getItem() == Items.DIAMOND) slot = i;
            }
            check(slot != -1, "The interface doesn't list diamonds");
            check(items.getStackInSlot(slot).getCount() == 2, "The interface lists " + items.getStackInSlot(slot).getCount() + " diamonds instead of 2");
            ItemStack out = items.extractItem(slot, 64, false);
            check(out.getItem() == Items.DIAMOND && out.getCount() == 2, "Got " + out + " instead of 2 diamonds");
            check(emc(player).signum() == 0, "The owner has " + emc(player) + " EMC left over");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void interfaceRefusesLockedItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            transmutationInterface(helper, player);
            ItemStack locked = new ItemStack(Items.DIAMOND, 4);
            EmcLock.toggle(locked);
            ItemStack left = items(helper).insertItem(0, locked, false);
            check(left.getCount() == 4, "The interface took EMC locked diamonds");
            check(emc(player).signum() == 0, "The owner got EMC for locked diamonds");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void interfaceNeedsOnlineOwner(IntegrationTestHelper helper) {
        setBlock(helper, POS, Blocks.TRANSMUTATION_INTERFACE.get());
        blockEntity(helper, POS, BlockEntityTransmutationInterface.class).owner = UUID.randomUUID();
        ItemStack left = items(helper).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        check(left.getCount() == 4, "The interface took items for an offline owner");
    }
}
