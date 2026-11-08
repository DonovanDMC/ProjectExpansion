package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.container.slots.PXCraftingSlot;
import cool.furry.mc.projectexpansion.gui.container.slots.PXResultSlot;
import cool.furry.mc.projectexpansion.util.EmcLock;
import cool.furry.mc.projectexpansion.util.Platform;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.emc;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.knowledge;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

// the arcane transmutation tablet's crafting grid & output slots, in the emc batch as most of it trades in emc
@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class ArcaneTabletGameTests {
    private static ContainerArcaneTransmutationTablet open(ServerPlayer player) {
        ContainerArcaneTransmutationTablet menu = new ContainerArcaneTransmutationTablet(0, player.getInventory(), knowledge(player));
        player.containerMenu = menu;
        return menu;
    }

    private static List<Slot> grid(ContainerArcaneTransmutationTablet menu) {
        return menu.slots.stream().filter(slot -> slot instanceof PXCraftingSlot).toList();
    }

    private static Slot result(ContainerArcaneTransmutationTablet menu) {
        return menu.slots.stream().filter(slot -> slot instanceof PXResultSlot).findFirst().orElseThrow();
    }

    private static long value(Item item) {
        long value = Platform.emcProxy().getValue(item);
        check(value > 0, item + " has no EMC, so it can't be used to test the tablet");
        return value;
    }

    // two planks on top of each other make sticks, as the 3x3 recipe a recipe viewer would send
    private static List<List<ItemStack>> stickRecipe() {
        List<List<ItemStack>> recipe = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            // the tablet sorts these, so they need to be mutable
            recipe.add(i == 0 || i == 3 ? new ArrayList<>(List.of(new ItemStack(Items.OAK_PLANKS))) : new ArrayList<>());
        }
        return recipe;
    }

    // fills the stick recipe with ghost items, which are only paid for when they're used
    private static ContainerArcaneTransmutationTablet withGhostPlanks(ServerPlayer player) {
        knowledge(player).addKnowledge(ItemInfo.fromItem(Items.OAK_PLANKS));
        knowledge(player).setEmc(BigInteger.valueOf(value(Items.OAK_PLANKS) * 2));
        ContainerArcaneTransmutationTablet menu = open(player);
        menu.onRecipeTransfer(stickRecipe(), false, Optional.empty());
        check(menu.getGhostCount(0) == 1 && menu.getGhostCount(3) == 1, "The recipe was filled with " + menu.getGhostCount(0) + " & " + menu.getGhostCount(3) + " ghost planks instead of 1 each");
        check(result(menu).getItem().is(Items.STICK), "The grid shows " + result(menu).getItem() + " instead of sticks");
        return menu;
    }

    // items either go back to the inventory or turn into emc when the grid is emptied, depending on covalence loss
    private static void checkReturned(ServerPlayer player, Item item, int count) {
        int items = player.getInventory().countItem(item);
        BigInteger expected = BigInteger.valueOf(value(item) * (count - items));
        check(emc(player).equals(expected), "Got " + emc(player) + " EMC and " + items + " " + item + " back, instead of " + count + " " + item + "'s worth");
    }

    // closing it with a recipe in the grid used to hand out the result as well as the ingredients (#189)
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletClosingDoesNotCraft(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = open(player);
        grid(menu).get(0).set(new ItemStack(Items.OAK_PLANKS));
        grid(menu).get(3).set(new ItemStack(Items.OAK_PLANKS));
        check(result(menu).getItem().is(Items.STICK), "The grid shows " + result(menu).getItem() + " instead of sticks");

        menu.removed(player);
        int sticks = player.getInventory().countItem(Items.STICK);
        check(sticks == 0, "Closing the tablet gave " + sticks + " sticks");
        checkReturned(player, Items.OAK_PLANKS, 2);
        removePlayer(player);
        helper.succeed();
    }

    // whole stacks used to be turned into a single item's worth of emc
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletClosingReturnsWholeStacks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = open(player);
        grid(menu).get(0).set(new ItemStack(Items.OAK_PLANKS, 16));
        grid(menu).get(4).set(new ItemStack(Items.OAK_PLANKS, 16));
        menu.removed(player);
        checkReturned(player, Items.OAK_PLANKS, 32);
        removePlayer(player);
        helper.succeed();
    }

    // locked items are never turned into emc
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletClosingKeepsLockedItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = open(player);
        ItemStack locked = new ItemStack(Items.DIAMOND, 2);
        EmcLock.toggle(locked);
        grid(menu).get(0).set(locked);
        menu.removed(player);
        check(emc(player).signum() == 0, "The locked diamonds were turned into " + emc(player) + " EMC");
        int diamonds = player.getInventory().countItem(Items.DIAMOND);
        check(diamonds == 2, "Got " + diamonds + " diamonds back instead of 2");
        removePlayer(player);
        helper.succeed();
    }

    // ghost items were never paid for, so closing has to throw them away rather than hand them out or refund them
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletClosingDiscardsGhosts(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = withGhostPlanks(player);
        BigInteger before = emc(player);
        menu.removed(player);
        int planks = player.getInventory().countItem(Items.OAK_PLANKS);
        check(planks == 0, "Closing the tablet gave " + planks + " unpaid planks");
        check(emc(player).equals(before), "Closing the tablet changed the EMC from " + before + " to " + emc(player));
        removePlayer(player);
        helper.succeed();
    }

    // a player who dies or leaves with the tablet open gets the grid dropped instead, but not the result
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletDyingDropsGrid(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        Vec3 pos = helper.absoluteVec(new Vec3(4, 2, 4));
        player.teleportTo(helper.getLevel(), pos.x, pos.y, pos.z, 0, 0);
        ContainerArcaneTransmutationTablet menu = open(player);
        grid(menu).get(0).set(new ItemStack(Items.OAK_PLANKS));
        grid(menu).get(3).set(new ItemStack(Items.OAK_PLANKS));
        player.setHealth(0);
        menu.removed(player);

        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4));
        int planks = drops.stream().filter(drop -> drop.getItem().is(Items.OAK_PLANKS)).mapToInt(drop -> drop.getItem().getCount()).sum();
        int sticks = drops.stream().filter(drop -> drop.getItem().is(Items.STICK)).mapToInt(drop -> drop.getItem().getCount()).sum();
        drops.forEach(ItemEntity::discard);
        check(planks == 2, "Dropped " + planks + " planks instead of 2");
        check(sticks == 0, "Dropped " + sticks + " sticks");
        removePlayer(player);
        helper.succeed();
    }

    // real items are used before any ghost items are made
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletRecipeTransferUsesInventoryFirst(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        knowledge(player).addKnowledge(ItemInfo.fromItem(Items.OAK_PLANKS));
        knowledge(player).setEmc(BigInteger.valueOf(value(Items.OAK_PLANKS) * 2));
        player.getInventory().add(new ItemStack(Items.OAK_PLANKS, 2));
        ContainerArcaneTransmutationTablet menu = open(player);
        menu.onRecipeTransfer(stickRecipe(), false, Optional.empty());
        Slot top = grid(menu).get(0), bottom = grid(menu).get(3);
        check(menu.getGhostCount(0) == 0 && menu.getGhostCount(3) == 0, "The recipe put " + top.getItem().getCount() + " & " + bottom.getItem().getCount() + " planks in the grid, " + menu.getGhostCount(0) + " & " + menu.getGhostCount(3) + " of them ghosts, with real ones in the inventory");
        int planks = player.getInventory().countItem(Items.OAK_PLANKS);
        check(planks == 0, planks + " planks were left in the inventory");
        check(result(menu).getItem().is(Items.STICK), "The grid shows " + result(menu).getItem() + " instead of sticks");
        removePlayer(player);
        helper.succeed();
    }

    // locked items & ones with extra data (like a name) are left alone unless the recipe asks for exactly that
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletRecipeTransferSkipsProtectedItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack locked = new ItemStack(Items.OAK_PLANKS);
        EmcLock.toggle(locked);
        ItemStack named = new ItemStack(Items.OAK_PLANKS);
        named.setHoverName(Platform.literal("Named"));
        player.getInventory().add(locked);
        player.getInventory().add(named);
        ContainerArcaneTransmutationTablet menu = open(player);
        menu.onRecipeTransfer(stickRecipe(), false, Optional.empty());
        check(grid(menu).stream().noneMatch(Slot::hasItem), "The recipe pulled a locked or named plank into the grid");
        int planks = player.getInventory().countItem(Items.OAK_PLANKS);
        check(planks == 2, planks + " planks were left in the inventory instead of 2");
        removePlayer(player);
        helper.succeed();
    }

    // crafting with ghost items pays for them then
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletGhostCraftPaysEmc(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = withGhostPlanks(player);
        menu.clicked(result(menu).index, 0, ClickType.PICKUP, player);
        check(menu.getCarried().is(Items.STICK) && menu.getCarried().getCount() == 4, "Crafting gave " + menu.getCarried() + " instead of 4 sticks");
        check(emc(player).signum() == 0, "Crafting with ghost planks left " + emc(player) + " EMC instead of using it all");
        // there's nothing left to pay for more, so the grid isn't refilled
        check(grid(menu).stream().noneMatch(Slot::hasItem), "The grid was refilled without the EMC to pay for it");
        removePlayer(player);
        helper.succeed();
    }

    // without the emc, ghost items can't be crafted with, taken out, or double clicked out
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletGhostsNeedEmc(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = withGhostPlanks(player);
        knowledge(player).setEmc(BigInteger.ZERO);
        menu.clicked(result(menu).index, 0, ClickType.PICKUP, player);
        check(menu.getCarried().isEmpty(), "Crafted " + menu.getCarried() + " without the EMC for the ghost planks");
        Slot ghost = grid(menu).get(0);
        menu.quickMoveStack(player, ghost.index);
        int planks = player.getInventory().countItem(Items.OAK_PLANKS);
        check(planks == 0, "Shift clicking took " + planks + " unpaid planks");
        check(!menu.canTakeItemForPickAll(new ItemStack(Items.OAK_PLANKS), ghost), "Double clicking can take unpaid planks");
        removePlayer(player);
        helper.succeed();
    }

    // leftovers like the bottle from a honey bottle are taken out of the grid, so they don't block the next craft
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletCraftReturnsLeftovers(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = open(player);
        grid(menu).get(0).set(new ItemStack(Items.HONEY_BOTTLE));
        check(result(menu).getItem().is(Items.SUGAR), "The grid shows " + result(menu).getItem() + " instead of sugar");
        menu.clicked(result(menu).index, 0, ClickType.PICKUP, player);
        check(menu.getCarried().is(Items.SUGAR), "Crafting gave " + menu.getCarried() + " instead of sugar");
        check(grid(menu).stream().noneMatch(Slot::hasItem), "The grid still has " + grid(menu).get(0).getItem() + " in it");
        checkReturned(player, Items.GLASS_BOTTLE, 1);
        removePlayer(player);
        helper.succeed();
    }

    // clearing used to turn items with extra data into emc as well, losing the data, closing gives them back
    @GameTest(template = EMPTY, batch = "emc")
    public static void arcaneTabletClearKeepsNamedItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ContainerArcaneTransmutationTablet menu = open(player);
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.setHoverName(Platform.literal("Named"));
        grid(menu).get(0).set(named.copy());
        menu.clearCrafting(false);
        check(emc(player).signum() == 0, "Clearing turned the named diamond into " + emc(player) + " EMC");
        check(player.getInventory().items.stream().anyMatch(stack -> Platform.isSameItemSameData(stack, named)), "The named diamond didn't come back");
        removePlayer(player);
        helper.succeed();
    }
}
