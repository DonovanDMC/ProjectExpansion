package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.util.AdvancedAlchemicalChest;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;

// the advanced alchemical chest, which stores its items in its owner's alchemical bag
@IntegrationTestClass("empty")
public class ChestTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static IItemHandler items(IntegrationTestHelper helper) {
        return Objects.requireNonNull(helper.getTileEntity(POS), "No block entity at " + POS).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).resolve().orElseThrow(() -> new AssertionError("No item handler at " + POS));
    }

    private static IItemHandler bag(ServerPlayerEntity player, DyeColor color) {
        return player.getCapability(ProjectEAPI.ALCH_BAG_CAPABILITY).resolve().orElseThrow(() -> new AssertionError("No alchemical bags for " + player.getScoreboardName())).getBag(color);
    }

    private static int count(IItemHandler handler, Item item) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).getItem() == item) count += handler.getStackInSlot(i).getCount();
        }
        return count;
    }

    private static BlockEntityAdvancedAlchemicalChest chest(IntegrationTestHelper helper, DyeColor color, ServerPlayerEntity owner) {
        helper.setBlockState(POS, AdvancedAlchemicalChest.getBlock(color).defaultBlockState());
        BlockEntityAdvancedAlchemicalChest chest = blockEntity(helper, POS, BlockEntityAdvancedAlchemicalChest.class);
        chest.setOwner(owner);
        return chest;
    }

    @IntegrationTest("empty")
    public void advancedChestUsesOwnersBag(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            chest(helper, DyeColor.WHITE, player);
            ItemStack left = items(helper).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
            check(left.isEmpty(), "The chest gave back " + left);
            check(count(bag(player, DyeColor.WHITE), Items.DIAMOND) == 4, "The diamonds didn't end up in the owner's white bag");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void advancedChestTakesBagColor(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            chest(helper, DyeColor.WHITE, player).handleItemActivation(player, new ItemStack(PEItems.getBag(DyeColor.RED)));
            check(helper.getBlockState(POS).is(AdvancedAlchemicalChest.getBlock(DyeColor.RED)), "The chest didn't turn red");
            BlockEntityAdvancedAlchemicalChest chest = blockEntity(helper, POS, BlockEntityAdvancedAlchemicalChest.class);
            check(player.getUUID().equals(chest.owner), "The chest lost its owner");
            check(chest.getColor() == DyeColor.RED, "The chest's block entity is " + chest.getColor() + " instead of red");
            items(helper).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
            check(count(bag(player, DyeColor.RED), Items.DIAMOND) == 4, "The diamonds didn't end up in the owner's red bag");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void advancedChestNeedsOnlineOwner(IntegrationTestHelper helper) {
        setBlock(helper, POS, AdvancedAlchemicalChest.getBlock(DyeColor.WHITE));
        blockEntity(helper, POS, BlockEntityAdvancedAlchemicalChest.class).owner = UUID.randomUUID();
        ItemStack left = items(helper).insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        check(left.getCount() == 4, "The chest took items for an offline owner");
    }
}
