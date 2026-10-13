package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.AlchemicalCollectionCollector;
import cool.furry.mc.projectexpansion.util.OwnerData;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.emc;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.knowledge;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class ItemGameTests {
    private static ItemStack ownedBy(ItemStack stack, ServerPlayer player) {
        new OwnerData(player.getUUID(), player.getScoreboardName()).set(stack);
        return stack;
    }

    @GameTest(template = EMPTY)
    public static void infiniteFuelNeedsEmc(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        long cost = Config.server.infiniteFuelCost.get();
        ItemStack fuel = new ItemStack(Items.INFINITE_FUEL.get());
        check(ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) == 0, "Infinite fuel without an owner burns");
        ownedBy(fuel, player);
        knowledge(player).setEmc(BigInteger.valueOf(cost - 1));
        check(ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) == 0, "Infinite fuel burns without enough EMC");
        knowledge(player).setEmc(BigInteger.valueOf(cost));
        check(ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) == Config.server.infiniteFuelBurnTime.get(), "Infinite fuel burns for " + ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) + " ticks instead of " + Config.server.infiniteFuelBurnTime.get());
        removePlayer(player);
        helper.succeed();
    }

    // smelting takes 200 ticks
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void infiniteFuelSmeltsForEmc(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper);
        long cost = Config.server.infiniteFuelCost.get();
        knowledge(player).setEmc(BigInteger.valueOf(cost * 4));
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.FURNACE);
        FurnaceBlockEntity furnace = blockEntity(helper, pos, FurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
        furnace.setItem(1, ownedBy(new ItemStack(Items.INFINITE_FUEL.get()), player));
        helper.startSequence()
            .thenWaitUntil(() -> check(furnace.getItem(2).is(net.minecraft.world.item.Items.STONE), "The furnace didn't smelt the cobblestone"))
            .thenExecute(() -> {
                check(furnace.getItem(1).is(Items.INFINITE_FUEL.get()), "The infinite fuel was used up, the slot has " + furnace.getItem(1));
                check(emc(player).longValueExact() == cost * 3, "The owner has " + emc(player) + " EMC instead of " + cost * 3);
            })
            .thenExecute(() -> removePlayer(player))
            .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void infiniteSteakFeedsForEmc(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        long cost = Config.server.infiniteSteakCost.get();
        knowledge(player).setEmc(BigInteger.valueOf(cost));
        player.getFoodData().setFoodLevel(10);
        ItemStack steak = new ItemStack(Items.INFINITE_STEAK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, steak);
        check(steak.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "The player couldn't start eating the steak");
        ItemStack left = steak.finishUsingItem(helper.getLevel(), player);
        check(left.is(Items.INFINITE_STEAK.get()) && left.getCount() == 1, "The steak was used up, got " + left);
        check(player.getFoodData().getFoodLevel() > 10, "Eating the steak didn't feed the player");
        check(emc(player).signum() == 0, "The player has " + emc(player) + " EMC left instead of 0");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void infiniteSteakNeedsEmc(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        knowledge(player).setEmc(BigInteger.valueOf(Config.server.infiniteSteakCost.get() - 1));
        player.getFoodData().setFoodLevel(10);
        ItemStack steak = new ItemStack(Items.INFINITE_STEAK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, steak);
        check(!steak.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "The player started eating the steak without enough EMC");
        steak.finishUsingItem(helper.getLevel(), player);
        check(player.getFoodData().getFoodLevel() == 10, "The steak fed the player without enough EMC");
        removePlayer(player);
        helper.succeed();
    }

    // in the emc batch, as knowledge can only be learned for items with EMC
    @GameTest(template = EMPTY, batch = "emc")
    public static void knowledgeBookSharesKnowledge(GameTestHelper helper) {
        ServerPlayer owner = mockPlayer(helper), learner = mockPlayer(helper, UUID.randomUUID(), 0);
        ItemInfo diamond = ItemInfo.fromItem(net.minecraft.world.item.Items.DIAMOND);
        knowledge(owner).addKnowledge(diamond);
        check(knowledge(owner).hasKnowledge(diamond), "The owner couldn't learn diamonds");

        ItemStack book = new ItemStack(Items.KNOWLEDGE_SHARING_BOOK.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, book);
        owner.setPose(Pose.CROUCHING);
        book.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND);
        OwnerData data = OwnerData.get(book);
        check(data != null && data.uuid().equals(owner.getUUID()), "Crouch using the book didn't store the owner's knowledge");

        owner.setPose(Pose.STANDING);
        check(!book.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND).getResult().consumesAction(), "The owner could use their own book");

        learner.setItemInHand(InteractionHand.MAIN_HAND, book);
        book.getItem().use(helper.getLevel(), learner, InteractionHand.MAIN_HAND);
        check(knowledge(learner).hasKnowledge(diamond), "The learner didn't learn diamonds from the book");
        check(book.isEmpty(), "The book wasn't used up");
        removePlayer(owner);
        removePlayer(learner);
        helper.succeed();
    }

    // what alchemical collection picks up is handed over in batches
    @GameTest(template = EMPTY, batch = "emc")
    public static void alchemicalCollectionPaysPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        AlchemicalCollectionCollector.Collected collected = new AlchemicalCollectionCollector.Collected(player.getUUID(), BigInteger.valueOf(100), new ArrayList<>(List.of(ItemInfo.fromItem(net.minecraft.world.item.Items.DIAMOND))), System.currentTimeMillis());
        try {
            check(collected.process(), "The collected EMC wasn't handed over");
        } catch (RuntimeException e) {
            throw new GameTestAssertException("Handing over the collected EMC threw " + e);
        }
        check(emc(player).longValueExact() == 100, "The player has " + emc(player) + " EMC instead of 100");
        check(knowledge(player).hasKnowledge(ItemInfo.fromItem(net.minecraft.world.item.Items.DIAMOND)), "The player didn't learn diamonds");
        removePlayer(player);
        helper.succeed();
    }
}
