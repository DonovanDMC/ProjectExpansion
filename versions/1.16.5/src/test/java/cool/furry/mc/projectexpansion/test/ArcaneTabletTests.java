package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.container.slots.PXCraftingSlot;
import cool.furry.mc.projectexpansion.gui.container.slots.PXResultSlot;
import cool.furry.mc.projectexpansion.util.EmcLock;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emc;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.knowledge;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;

// the arcane transmutation tablet's crafting grid
@IntegrationTestClass("empty")
public class ArcaneTabletTests {
    private static ContainerArcaneTransmutationTablet open(ServerPlayerEntity player) {
        ContainerArcaneTransmutationTablet menu = new ContainerArcaneTransmutationTablet(0, player.inventory, knowledge(player));
        player.containerMenu = menu;
        return menu;
    }

    private static List<Slot> grid(ContainerArcaneTransmutationTablet menu) {
        return menu.slots.stream().filter(slot -> slot instanceof PXCraftingSlot).collect(Collectors.toList());
    }

    private static Slot result(ContainerArcaneTransmutationTablet menu) {
        return menu.slots.stream().filter(slot -> slot instanceof PXResultSlot).findFirst().orElseThrow(() -> new AssertionError("The tablet has no result slot"));
    }

    private static boolean noneInGrid(ContainerArcaneTransmutationTablet menu) {
        return grid(menu).stream().noneMatch(Slot::hasItem);
    }

    private static long value(Item item) {
        long value = emcValue(item);
        check(value > 0, item + " has no EMC, so it can't be used to test the tablet");
        return value;
    }

    // two planks on top of each other make sticks, as the 3x3 recipe a recipe viewer would send
    private static List<List<ItemStack>> stickRecipe() {
        List<List<ItemStack>> recipe = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            // the tablet sorts these, so they need to be mutable
            recipe.add(i == 0 || i == 3 ? new ArrayList<>(Collections.singletonList(new ItemStack(Items.OAK_PLANKS))) : new ArrayList<>());
        }
        return recipe;
    }

    // fills the stick recipe with ghost items, which are only paid for when they're used
    private static ContainerArcaneTransmutationTablet withGhostPlanks(ServerPlayerEntity player) {
        knowledge(player).addKnowledge(ItemInfo.fromItem(Items.OAK_PLANKS));
        knowledge(player).setEmc(BigInteger.valueOf(value(Items.OAK_PLANKS) * 2));
        ContainerArcaneTransmutationTablet menu = open(player);
        menu.onRecipeTransfer(stickRecipe(), false, Optional.empty());
        check(menu.getGhostCount(0) == 1 && menu.getGhostCount(3) == 1, "The recipe was filled with " + menu.getGhostCount(0) + " & " + menu.getGhostCount(3) + " ghost planks instead of 1 each");
        check(result(menu).getItem().getItem() == Items.STICK, "The grid shows " + result(menu).getItem() + " instead of sticks");
        return menu;
    }

    // items either go back to the inventory or turn into emc when the grid is emptied, depending on covalence loss
    private static void checkReturned(ServerPlayerEntity player, Item item, int count) {
        int items = player.inventory.countItem(item);
        BigInteger expected = BigInteger.valueOf(value(item) * (count - items));
        check(emc(player).equals(expected), "Got " + emc(player) + " EMC and " + items + " " + item + " back, instead of " + count + " " + item + "'s worth");
    }

    // closing it with a recipe in the grid used to hand out the result as well as the ingredients (#189)
    @IntegrationTest("empty")
    public void arcaneTabletClosingDoesNotCraft(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ContainerArcaneTransmutationTablet menu = open(player);
            grid(menu).get(0).set(new ItemStack(Items.OAK_PLANKS));
            grid(menu).get(3).set(new ItemStack(Items.OAK_PLANKS));
            check(result(menu).getItem().getItem() == Items.STICK, "The grid shows " + result(menu).getItem() + " instead of sticks");

            menu.removed(player);
            int sticks = player.inventory.countItem(Items.STICK);
            check(sticks == 0, "Closing the tablet gave " + sticks + " sticks");
            checkReturned(player, Items.OAK_PLANKS, 2);
        } finally {
            removePlayer(player);
        }
    }

    // whole stacks used to be turned into a single item's worth of emc
    @IntegrationTest("empty")
    public void arcaneTabletClosingReturnsWholeStacks(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ContainerArcaneTransmutationTablet menu = open(player);
            grid(menu).get(0).set(new ItemStack(Items.OAK_PLANKS, 16));
            grid(menu).get(4).set(new ItemStack(Items.OAK_PLANKS, 16));
            menu.removed(player);
            checkReturned(player, Items.OAK_PLANKS, 32);
        } finally {
            removePlayer(player);
        }
    }

    // locked items are never turned into emc
    @IntegrationTest("empty")
    public void arcaneTabletClosingKeepsLockedItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ContainerArcaneTransmutationTablet menu = open(player);
            ItemStack locked = new ItemStack(Items.DIAMOND, 2);
            EmcLock.toggle(locked);
            grid(menu).get(0).set(locked);
            menu.removed(player);
            check(emc(player).signum() == 0, "The locked diamonds were turned into " + emc(player) + " EMC");
            int diamonds = player.inventory.countItem(Items.DIAMOND);
            check(diamonds == 2, "Got " + diamonds + " diamonds back instead of 2");
        } finally {
            removePlayer(player);
        }
    }

    // ghost items were never paid for, so closing has to throw them away rather than hand them out or refund them
    @IntegrationTest("empty")
    public void arcaneTabletClosingDiscardsGhosts(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ContainerArcaneTransmutationTablet menu = withGhostPlanks(player);
            BigInteger before = emc(player);
            menu.removed(player);
            int planks = player.inventory.countItem(Items.OAK_PLANKS);
            check(planks == 0, "Closing the tablet gave " + planks + " unpaid planks");
            check(emc(player).equals(before), "Closing the tablet changed the EMC from " + before + " to " + emc(player));
        } finally {
            removePlayer(player);
        }
    }

    // a player who dies or leaves with the tablet open gets the grid dropped instead, but not the result
    @IntegrationTest("empty")
    public void arcaneTabletDyingDropsGrid(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            BlockPos pos = absolute(helper, new BlockPos(4, 2, 4));
            player.teleportTo(helper.getWorld(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            ContainerArcaneTransmutationTablet menu = open(player);
            grid(menu).get(0).set(new ItemStack(Items.OAK_PLANKS));
            grid(menu).get(3).set(new ItemStack(Items.OAK_PLANKS));
            player.setHealth(0);
            menu.removed(player);

            List<ItemEntity> drops = helper.getWorld().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4));
            int planks = drops.stream().filter(drop -> drop.getItem().getItem() == Items.OAK_PLANKS).mapToInt(drop -> drop.getItem().getCount()).sum();
            int sticks = drops.stream().filter(drop -> drop.getItem().getItem() == Items.STICK).mapToInt(drop -> drop.getItem().getCount()).sum();
            drops.forEach(ItemEntity::remove);
            check(planks == 2, "Dropped " + planks + " planks instead of 2");
            check(sticks == 0, "Dropped " + sticks + " sticks");
        } finally {
            removePlayer(player);
        }
    }

    // real items are used before any ghost items are made
    @IntegrationTest("empty")
    public void arcaneTabletRecipeTransferUsesInventoryFirst(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            knowledge(player).addKnowledge(ItemInfo.fromItem(Items.OAK_PLANKS));
            knowledge(player).setEmc(BigInteger.valueOf(value(Items.OAK_PLANKS) * 2));
            player.inventory.add(new ItemStack(Items.OAK_PLANKS, 2));
            ContainerArcaneTransmutationTablet menu = open(player);
            menu.onRecipeTransfer(stickRecipe(), false, Optional.empty());
            Slot top = grid(menu).get(0), bottom = grid(menu).get(3);
            check(menu.getGhostCount(0) == 0 && menu.getGhostCount(3) == 0, "The recipe put " + top.getItem().getCount() + " & " + bottom.getItem().getCount() + " planks in the grid, " + menu.getGhostCount(0) + " & " + menu.getGhostCount(3) + " of them ghosts, with real ones in the inventory");
            int planks = player.inventory.countItem(Items.OAK_PLANKS);
            check(planks == 0, planks + " planks were left in the inventory");
            check(result(menu).getItem().getItem() == Items.STICK, "The grid shows " + result(menu).getItem() + " instead of sticks");
        } finally {
            removePlayer(player);
        }
    }

    // locked items & ones with extra data (like a name) are left alone unless the recipe asks for exactly that
    @IntegrationTest("empty")
    public void arcaneTabletRecipeTransferSkipsProtectedItems(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ItemStack locked = new ItemStack(Items.OAK_PLANKS);
            EmcLock.toggle(locked);
            ItemStack named = new ItemStack(Items.OAK_PLANKS);
            named.setHoverName(new StringTextComponent("Named"));
            player.inventory.add(locked);
            player.inventory.add(named);
            ContainerArcaneTransmutationTablet menu = open(player);
            menu.onRecipeTransfer(stickRecipe(), false, Optional.empty());
            check(noneInGrid(menu), "The recipe pulled a locked or named plank into the grid");
            int planks = player.inventory.countItem(Items.OAK_PLANKS);
            check(planks == 2, planks + " planks were left in the inventory instead of 2");
        } finally {
            removePlayer(player);
        }
    }

    // crafting with ghost items pays for them then
    @IntegrationTest("empty")
    public void arcaneTabletGhostCraftPaysEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ContainerArcaneTransmutationTablet menu = withGhostPlanks(player);
            menu.clicked(result(menu).index, 0, ClickType.PICKUP, player);
            ItemStack carried = player.inventory.getCarried();
            check(carried.getItem() == Items.STICK && carried.getCount() == 4, "Crafting gave " + carried + " instead of 4 sticks");
            check(emc(player).signum() == 0, "Crafting with ghost planks left " + emc(player) + " EMC instead of using it all");
            // there's nothing left to pay for more, so the grid isn't refilled
            check(noneInGrid(menu), "The grid was refilled without the EMC to pay for it");
        } finally {
            removePlayer(player);
        }
    }

    // without the emc, ghost items can't be crafted with, taken out, or double clicked out
    @IntegrationTest("empty")
    public void arcaneTabletGhostsNeedEmc(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ContainerArcaneTransmutationTablet menu = withGhostPlanks(player);
            knowledge(player).setEmc(BigInteger.ZERO);
            menu.clicked(result(menu).index, 0, ClickType.PICKUP, player);
            check(player.inventory.getCarried().isEmpty(), "Crafted " + player.inventory.getCarried() + " without the EMC for the ghost planks");
            Slot ghost = grid(menu).get(0);
            menu.quickMoveStack(player, ghost.index);
            int planks = player.inventory.countItem(Items.OAK_PLANKS);
            check(planks == 0, "Shift clicking took " + planks + " unpaid planks");
            check(!menu.canTakeItemForPickAll(new ItemStack(Items.OAK_PLANKS), ghost), "Double clicking can take unpaid planks");
        } finally {
            removePlayer(player);
        }
    }

    // leftovers like the bottle from a honey bottle are taken out of the grid, so they don't block the next craft
    @IntegrationTest("empty")
    public void arcaneTabletCraftReturnsLeftovers(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            ContainerArcaneTransmutationTablet menu = open(player);
            grid(menu).get(0).set(new ItemStack(Items.HONEY_BOTTLE));
            check(result(menu).getItem().getItem() == Items.SUGAR, "The grid shows " + result(menu).getItem() + " instead of sugar");
            menu.clicked(result(menu).index, 0, ClickType.PICKUP, player);
            check(player.inventory.getCarried().getItem() == Items.SUGAR, "Crafting gave " + player.inventory.getCarried() + " instead of sugar");
            check(noneInGrid(menu), "The grid still has " + grid(menu).get(0).getItem() + " in it");
            checkReturned(player, Items.GLASS_BOTTLE, 1);
        } finally {
            removePlayer(player);
        }
    }
}
