package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;
import static cool.furry.mc.projectexpansion.test.TestUtil.steps;
import static cool.furry.mc.projectexpansion.test.TestUtil.waitUntil;

@IntegrationTestClass("empty")
public class RefineryTests {
    private static final BlockPos REFINERY = new BlockPos(1, 1, 1);

    private static BlockEntityEMCRefinery refinery(IntegrationTestHelper helper, int rawIanthium) {
        setBlock(helper, REFINERY, Blocks.EMC_REFINERY.get());
        BlockEntityEMCRefinery refinery = blockEntity(helper, REFINERY, BlockEntityEMCRefinery.class);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get(), rawIanthium));
        return refinery;
    }

    private static void relay(IntegrationTestHelper helper, BlockPos pos) {
        setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getRelay()));
        blockEntity(helper, pos, BlockEntityRelay.class).insertEmc(10_000, IEmcStorage.EmcAction.EXECUTE);
    }

    private static int ingots(BlockEntityEMCRefinery refinery) {
        ItemStack output = refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.OUTPUT_SLOT);
        return output.getItem() == Items.IANTHIUM_INGOT.get() ? output.getCount() : 0;
    }

    private static ItemStack kleinStar(long emc) {
        ItemStack star = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("projecte", "klein_star_ein")));
        holder(star).insertEmc(star, emc, IEmcStorage.EmcAction.EXECUTE);
        return star;
    }

    private static IItemEmcHolder holder(ItemStack star) {
        return star.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).resolve().orElseThrow(() -> new AssertionError("Klein stars hold no emc"));
    }

    @IntegrationTest(value = "empty", timeoutTicks = 400)
    public void refinesWithAdjacentEmc(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 2);
        relay(helper, new BlockPos(2, 1, 1));
        waitUntil(helper, () -> {
            check(ingots(refinery) == 2, "Refined " + ingots(refinery) + " of 2 raw ianthium with a relay next to it");
            check(refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT).isEmpty(), "The raw ianthium wasn't used up");
        });
    }

    @IntegrationTest(value = "empty", timeoutTicks = 400)
    public void refinesWithPipedEmc(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        relay(helper, new BlockPos(4, 1, 1));
        for (int x = 2; x <= 3; x++) setBlock(helper, new BlockPos(x, 1, 1), Blocks.EMC_PIPE.get());
        BlockPos pipe = absolute(helper, new BlockPos(3, 1, 1));
        BlockEmcPipe.useWrench(helper.getWorld(), pipe, helper.getWorld().getBlockState(pipe), Direction.EAST, false);
        waitUntil(helper, () -> check(ingots(refinery) == 1, "Nothing was refined with emc from a pipe"));
    }

    @IntegrationTest(value = "empty", timeoutTicks = 400)
    public void refinesWithKleinStar(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.STAR_SLOT, kleinStar(1_000));
        waitUntil(helper, () -> check(ingots(refinery) == 1, "Nothing was refined with emc from a Klein Star"));
    }

    @IntegrationTest("empty")
    public void litOnlyWhileRefining(IntegrationTestHelper helper) {
        refinery(helper, 1);
        steps(helper,
            () -> {
                check(!helper.getBlockState(REFINERY).getValue(BlockEMCRefinery.LIT), "Lit without any emc");
                relay(helper, new BlockPos(2, 1, 1));
            },
            () -> check(helper.getBlockState(REFINERY).getValue(BlockEMCRefinery.LIT), "Not lit while refining")
        );
    }

    @IntegrationTest("empty")
    public void bufferHoldsWhileSupplied(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        refinery.insertEmc(BlockEntityEMCRefinery.CAPACITY, IEmcStorage.EmcAction.EXECUTE);
        relay(helper, new BlockPos(2, 1, 1));
        waitUntil(helper, () -> {
            check(ingots(refinery) == 1, "Nothing was refined");
            check(refinery.getStoredEmc() == BlockEntityEMCRefinery.CAPACITY, "The buffer was used while a relay was supplying emc");
        });
    }

    @IntegrationTest("empty")
    public void bufferUsedWithoutSupply(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        refinery.insertEmc(BlockEntityEMCRefinery.CAPACITY, IEmcStorage.EmcAction.EXECUTE);
        waitUntil(helper, () -> {
            check(ingots(refinery) == 1, "Nothing was refined from the buffer");
            check(refinery.getStoredEmc() == BlockEntityEMCRefinery.CAPACITY - 64, "The buffer paid " + (BlockEntityEMCRefinery.CAPACITY - refinery.getStoredEmc()) + " instead of the recipe's 64");
        });
    }

    @IntegrationTest("empty")
    public void starPaysBeforeBuffer(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        refinery.insertEmc(BlockEntityEMCRefinery.CAPACITY, IEmcStorage.EmcAction.EXECUTE);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.STAR_SLOT, kleinStar(1_000));
        waitUntil(helper, () -> {
            check(ingots(refinery) == 1, "Nothing was refined");
            check(refinery.getStoredEmc() == BlockEntityEMCRefinery.CAPACITY, "The buffer was used while the star had emc");
            ItemStack inSlot = refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.STAR_SLOT);
            long left = holder(inSlot).getStoredEmc(inSlot);
            check(left == 1_000 - 64, "The star paid " + (1_000 - left) + " instead of the recipe's 64");
        });
    }

    @IntegrationTest("empty")
    public void noEmcNoRefining(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        helper.runAfter(120, () -> {
            check(ingots(refinery) == 0, "Refined without any emc");
            check(refinery.getProgress() == 0, "Made progress without any emc");
        });
    }

    @IntegrationTest("empty")
    public void automationOnlyMovesTheRightItems(IntegrationTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 0);
        IItemHandler handler = refinery.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).resolve().orElseThrow(() -> new AssertionError("The refinery has no item handler"));
        check(handler.insertItem(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get()), false).isEmpty(), "Raw ianthium couldn't be inserted");
        check(!handler.insertItem(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(net.minecraft.item.Items.DIRT), false).isEmpty(), "Something that can't be refined was inserted");
        check(!handler.insertItem(BlockEntityEMCRefinery.STAR_SLOT, new ItemStack(net.minecraft.item.Items.DIRT), false).isEmpty(), "Something that isn't a Klein Star went into the star slot");
        check(handler.extractItem(BlockEntityEMCRefinery.INPUT_SLOT, 1, false).isEmpty(), "The input could be taken out");
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.OUTPUT_SLOT, new ItemStack(Items.IANTHIUM_INGOT.get()));
        check(!handler.extractItem(BlockEntityEMCRefinery.OUTPUT_SLOT, 1, false).isEmpty(), "The output couldn't be taken out");
    }

    @IntegrationTest("empty")
    public void ianthiumOnlyRefines(IntegrationTestHelper helper) {
        World level = helper.getWorld();
        Inventory raw = new Inventory(new ItemStack(Items.RAW_IANTHIUM.get()));
        check(!level.getRecipeManager().getRecipeFor(IRecipeType.SMELTING, raw, level).isPresent(), "Raw ianthium can be smelted in a furnace");
        check(!level.getRecipeManager().getRecipeFor(IRecipeType.BLASTING, raw, level).isPresent(), "Raw ianthium can be smelted in a blast furnace");
        check(level.getRecipeManager().getRecipeFor(RecipeTypes.REFINING.get(), raw, level).map(recipe -> recipe.result().getItem() == Items.IANTHIUM_INGOT.get()).orElse(false), "Raw ianthium doesn't refine into an ingot");
        check(level.getRecipeManager().getRecipeFor(RecipeTypes.REFINING.get(), new Inventory(new ItemStack(Items.IANTHIUM_ORE.get())), level).isPresent(), "Ianthium ore doesn't refine");
    }

    // recipes from a datapack in src/test/datapacks, the same way another mod or a datapack would add them
    @IntegrationTest("empty")
    public void otherModsCanAddRecipes(IntegrationTestHelper helper) {
        RecipeManager recipes = helper.getWorld().getRecipeManager();
        check(!recipes.byKey(new ResourceLocation("pex_test_addon", "missing_mod")).isPresent(), "A recipe loaded even though its conditions failed");
        IRecipe<?> named = recipes.byKey(new ResourceLocation("pex_test_addon", "named_result")).orElse(null);
        check(named instanceof RefiningRecipe, "Another mod's refining recipe didn't load");
        BlockEntityEMCRefinery refinery = refinery(helper, 0);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(net.minecraft.item.Items.COBBLESTONE));
        relay(helper, new BlockPos(2, 1, 1));
        waitUntil(helper, () -> {
            ItemStack output = refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.OUTPUT_SLOT);
            check(output.getItem() == net.minecraft.item.Items.STONE && output.getCount() == 3, "Refined into " + output + " instead of 3 stone");
            check("Refined Stone".equals(output.getHoverName().getString()), "The result lost its name");
        });
    }

    @IntegrationTest("empty")
    public void refiningMapsEmc(IntegrationTestHelper helper) {
        long iron = emcValue(net.minecraft.item.Items.IRON_INGOT);
        check(iron > 0, "Iron has no emc");
        // two iron and 100 emc refine into a structure void, which has no value any other way
        long value = emcValue(net.minecraft.item.Items.STRUCTURE_VOID);
        check(value == iron * 2 + 100, "Refined structure void is worth " + value + " instead of " + (iron * 2 + 100));
        check(emcValue(Items.IANTHIUM_INGOT.get()) == 256, "Refining changed the ianthium ingot's fixed value");
    }
}
