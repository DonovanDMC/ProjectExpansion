package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.AlchemicalCollectionCollector;
import cool.furry.mc.projectexpansion.util.OwnerData;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.FurnaceTileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeHooks;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emc;
import static cool.furry.mc.projectexpansion.test.TestUtil.knowledge;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.steps;

@IntegrationTestClass("empty")
public class ItemTests {
    // what alchemical collection picks up is handed over in batches, which used to read the client config
    @IntegrationTest("empty")
    public void alchemicalCollectionPaysPlayer(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            AlchemicalCollectionCollector.Collected collected = new AlchemicalCollectionCollector.Collected(player.getUUID(), BigInteger.valueOf(100), new ArrayList<>(Collections.singletonList(ItemInfo.fromItem(net.minecraft.item.Items.DIAMOND))), System.currentTimeMillis());
            check(collected.process(), "The collected EMC wasn't handed over");
            check(emc(player).longValueExact() == 100, "The player has " + emc(player) + " EMC instead of 100");
            check(knowledge(player).hasKnowledge(ItemInfo.fromItem(net.minecraft.item.Items.DIAMOND)), "The player didn't learn diamonds");
        } finally {
            removePlayer(player);
        }
    }

    private static ItemStack ownedBy(ItemStack stack, ServerPlayerEntity player) {
        new OwnerData(player.getUUID(), player.getScoreboardName()).set(stack);
        return stack;
    }

    private static int burnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, IRecipeType.SMELTING);
    }

    @IntegrationTest("empty")
    public void infiniteFuelNeedsEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            long cost = Config.server.infiniteFuelCost.get();
            ItemStack fuel = new ItemStack(Items.INFINITE_FUEL.get());
            check(burnTime(fuel) == 0, "Infinite fuel without an owner burns");
            ownedBy(fuel, player);
            knowledge(player).setEmc(BigInteger.valueOf(cost - 1));
            check(burnTime(fuel) == 0, "Infinite fuel burns without enough EMC");
            knowledge(player).setEmc(BigInteger.valueOf(cost));
            check(burnTime(fuel) == Config.server.infiniteFuelBurnTime.get(), "Infinite fuel burns for " + burnTime(fuel) + " ticks instead of " + Config.server.infiniteFuelBurnTime.get());
        } finally {
            removePlayer(player);
        }
    }

    // smelting takes 200 ticks
    @IntegrationTest(value = "empty", timeoutTicks = 300)
    public void infiniteFuelSmeltsForEmc(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayerEntity player = mockPlayer(helper);
        long cost = Config.server.infiniteFuelCost.get();
        knowledge(player).setEmc(BigInteger.valueOf(cost * 4));
        setBlock(helper, pos, net.minecraft.block.Blocks.FURNACE);
        FurnaceTileEntity furnace = blockEntity(helper, pos, FurnaceTileEntity.class);
        furnace.setItem(0, new ItemStack(net.minecraft.item.Items.COBBLESTONE));
        furnace.setItem(1, ownedBy(new ItemStack(Items.INFINITE_FUEL.get()), player));
        steps(helper,
            () -> check(furnace.getItem(2).getItem() == net.minecraft.item.Items.STONE, "The furnace didn't smelt the cobblestone"),
            () -> {
                check(furnace.getItem(1).getItem() == Items.INFINITE_FUEL.get(), "The infinite fuel was used up, the slot has " + furnace.getItem(1));
                check(emc(player).longValueExact() == cost * 3, "The owner has " + emc(player) + " EMC instead of " + cost * 3);
            },
            () -> removePlayer(player)
        );
    }

    @IntegrationTest("empty")
    public void infiniteSteakFeedsForEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            long cost = Config.server.infiniteSteakCost.get();
            knowledge(player).setEmc(BigInteger.valueOf(cost));
            player.getFoodData().setFoodLevel(10);
            ItemStack steak = new ItemStack(Items.INFINITE_STEAK.get());
            player.setItemInHand(Hand.MAIN_HAND, steak);
            check(steak.getItem().use(helper.getWorld(), player, Hand.MAIN_HAND).getResult().consumesAction(), "The player couldn't start eating the steak");
            ItemStack left = steak.finishUsingItem(helper.getWorld(), player);
            check(left.getItem() == Items.INFINITE_STEAK.get() && left.getCount() == 1, "The steak was used up, got " + left);
            check(player.getFoodData().getFoodLevel() > 10, "Eating the steak didn't feed the player");
            check(emc(player).signum() == 0, "The player has " + emc(player) + " EMC left instead of 0");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void infiniteSteakNeedsEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            knowledge(player).setEmc(BigInteger.valueOf(Config.server.infiniteSteakCost.get() - 1));
            player.getFoodData().setFoodLevel(10);
            ItemStack steak = new ItemStack(Items.INFINITE_STEAK.get());
            player.setItemInHand(Hand.MAIN_HAND, steak);
            check(!steak.getItem().use(helper.getWorld(), player, Hand.MAIN_HAND).getResult().consumesAction(), "The player started eating the steak without enough EMC");
            steak.finishUsingItem(helper.getWorld(), player);
            check(player.getFoodData().getFoodLevel() == 10, "The steak fed the player without enough EMC");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void knowledgeBookSharesKnowledge(IntegrationTestHelper helper) {
        ServerPlayerEntity owner = mockPlayer(helper), learner = mockPlayer(helper);
        try {
            ItemInfo diamond = ItemInfo.fromItem(net.minecraft.item.Items.DIAMOND);
            knowledge(owner).addKnowledge(diamond);
            check(knowledge(owner).hasKnowledge(diamond), "The owner couldn't learn diamonds");

            ItemStack book = new ItemStack(Items.KNOWLEDGE_SHARING_BOOK.get());
            owner.setItemInHand(Hand.MAIN_HAND, book);
            owner.setPose(Pose.CROUCHING);
            book.getItem().use(helper.getWorld(), owner, Hand.MAIN_HAND);
            OwnerData data = OwnerData.get(book);
            check(data != null && data.uuid().equals(owner.getUUID()), "Crouch using the book didn't store the owner's knowledge");

            owner.setPose(Pose.STANDING);
            check(!book.getItem().use(helper.getWorld(), owner, Hand.MAIN_HAND).getResult().consumesAction(), "The owner could use their own book");

            learner.setItemInHand(Hand.MAIN_HAND, book);
            book.getItem().use(helper.getWorld(), learner, Hand.MAIN_HAND);
            check(knowledge(learner).hasKnowledge(diamond), "The learner didn't learn diamonds from the book");
            check(book.isEmpty(), "The book wasn't used up");
        } finally {
            removePlayer(owner);
            removePlayer(learner);
        }
    }
}
