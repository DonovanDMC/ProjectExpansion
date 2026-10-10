package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class RefineryGameTests {
    private static final BlockPos REFINERY = new BlockPos(1, 1, 1);

    private static BlockEntityEMCRefinery refinery(GameTestHelper helper, int rawIanthium) {
        helper.setBlock(REFINERY, Blocks.EMC_REFINERY.get());
        BlockEntityEMCRefinery refinery = blockEntity(helper, REFINERY, BlockEntityEMCRefinery.class);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get(), rawIanthium));
        return refinery;
    }

    private static void relay(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Objects.requireNonNull(Matter.BASIC.getRelay()));
        blockEntity(helper, pos, BlockEntityRelay.class).insertEmc(10_000, IEmcStorage.EmcAction.EXECUTE);
    }

    private static int ingots(BlockEntityEMCRefinery refinery) {
        ItemStack output = refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.OUTPUT_SLOT);
        return output.is(Items.IANTHIUM_INGOT.get()) ? output.getCount() : 0;
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void refinesWithAdjacentEmc(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 2);
        relay(helper, new BlockPos(2, 1, 1));
        helper.succeedWhen(() -> {
            check(ingots(refinery) == 2, "Refined " + ingots(refinery) + " of 2 raw ianthium with a relay next to it");
            check(refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT).isEmpty(), "The raw ianthium wasn't used up");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void refinesWithPipedEmc(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        BlockPos source = new BlockPos(4, 1, 1);
        relay(helper, source);
        for (int x = 2; x <= 3; x++) helper.setBlock(new BlockPos(x, 1, 1), Blocks.EMC_PIPE.get());
        BlockPos pipe = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockEmcPipe.useWrench(helper.getLevel(), pipe, helper.getLevel().getBlockState(pipe), Direction.EAST, false);
        helper.succeedWhen(() -> check(ingots(refinery) == 1, "Nothing was refined with emc from a pipe"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void refinesWithKleinStar(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        ItemStack star = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("projecte", "klein_star_ein")));
        IItemEmcHolder holder = Objects.requireNonNull(star.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY), "Klein stars hold no emc");
        holder.insertEmc(star, 1_000, IEmcStorage.EmcAction.EXECUTE);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.STAR_SLOT, star);
        helper.succeedWhen(() -> check(ingots(refinery) == 1, "Nothing was refined with emc from a Klein Star"));
    }

    @GameTest(template = EMPTY)
    public static void litOnlyWhileRefining(GameTestHelper helper) {
        refinery(helper, 1);
        helper.runAfterDelay(5, () -> {
            check(!helper.getBlockState(REFINERY).getValue(BlockEMCRefinery.LIT), "Lit without any emc");
            relay(helper, new BlockPos(2, 1, 1));
            helper.succeedWhen(() -> check(helper.getBlockState(REFINERY).getValue(BlockEMCRefinery.LIT), "Not lit while refining"));
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bufferHoldsWhileSupplied(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        refinery.insertEmc(BlockEntityEMCRefinery.CAPACITY, IEmcStorage.EmcAction.EXECUTE);
        relay(helper, new BlockPos(2, 1, 1));
        helper.succeedWhen(() -> {
            check(ingots(refinery) == 1, "Nothing was refined");
            check(refinery.getStoredEmc() == BlockEntityEMCRefinery.CAPACITY, "The buffer was used while a relay was supplying emc");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bufferUsedWithoutSupply(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        refinery.insertEmc(BlockEntityEMCRefinery.CAPACITY, IEmcStorage.EmcAction.EXECUTE);
        helper.succeedWhen(() -> {
            check(ingots(refinery) == 1, "Nothing was refined from the buffer");
            check(refinery.getStoredEmc() == BlockEntityEMCRefinery.CAPACITY - 64, "The buffer paid " + (BlockEntityEMCRefinery.CAPACITY - refinery.getStoredEmc()) + " instead of the recipe's 64");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void starPaysBeforeBuffer(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        refinery.insertEmc(BlockEntityEMCRefinery.CAPACITY, IEmcStorage.EmcAction.EXECUTE);
        ItemStack star = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("projecte", "klein_star_ein")));
        IItemEmcHolder holder = Objects.requireNonNull(star.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY), "Klein stars hold no emc");
        holder.insertEmc(star, 1_000, IEmcStorage.EmcAction.EXECUTE);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.STAR_SLOT, star);
        helper.succeedWhen(() -> {
            check(ingots(refinery) == 1, "Nothing was refined");
            check(refinery.getStoredEmc() == BlockEntityEMCRefinery.CAPACITY, "The buffer was used while the star had emc");
            ItemStack inSlot = refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.STAR_SLOT);
            long left = Objects.requireNonNull(inSlot.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY)).getStoredEmc(inSlot);
            check(left == 1_000 - 64, "The star paid " + (1_000 - left) + " instead of the recipe's 64");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void noEmcNoRefining(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 1);
        helper.runAfterDelay(120, () -> {
            check(ingots(refinery) == 0, "Refined without any emc");
            check(refinery.getProgress() == 0, "Made progress without any emc");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void automationOnlyMovesTheRightItems(GameTestHelper helper) {
        BlockEntityEMCRefinery refinery = refinery(helper, 0);
        IItemHandler handler = Objects.requireNonNull(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(REFINERY), Direction.UP));
        check(handler.insertItem(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get()), false).isEmpty(), "Raw ianthium couldn't be inserted");
        check(!handler.insertItem(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(net.minecraft.world.item.Items.DIRT), false).isEmpty(), "Something that can't be refined was inserted");
        check(!handler.insertItem(BlockEntityEMCRefinery.STAR_SLOT, new ItemStack(net.minecraft.world.item.Items.DIRT), false).isEmpty(), "Something that isn't a Klein Star went into the star slot");
        check(handler.extractItem(BlockEntityEMCRefinery.INPUT_SLOT, 1, false).isEmpty(), "The input could be taken out");
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.OUTPUT_SLOT, new ItemStack(Items.IANTHIUM_INGOT.get()));
        check(!handler.extractItem(BlockEntityEMCRefinery.OUTPUT_SLOT, 1, false).isEmpty(), "The output couldn't be taken out");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ianthiumOnlyRefines(GameTestHelper helper) {
        var level = helper.getLevel();
        SingleRecipeInput raw = new SingleRecipeInput(new ItemStack(Items.RAW_IANTHIUM.get()));
        check(level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, raw, level).isEmpty(), "Raw ianthium can be smelted in a furnace");
        check(level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, raw, level).isEmpty(), "Raw ianthium can be smelted in a blast furnace");
        check(level.getRecipeManager().getRecipeFor(RecipeTypes.REFINING.get(), raw, level).map(recipe -> recipe.value().result().is(Items.IANTHIUM_INGOT.get())).orElse(false), "Raw ianthium doesn't refine into an ingot");
        check(level.getRecipeManager().getRecipeFor(RecipeTypes.REFINING.get(), new SingleRecipeInput(new ItemStack(Items.DEEPSLATE_IANTHIUM_ORE.get())), level).isPresent(), "Ianthium ore doesn't refine");
        helper.succeed();
    }

    // recipes from another namespace, in the gametest resources, the same way another mod or a datapack would add them
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void otherModsCanAddRecipes(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        check(recipes.byKey(ResourceLocation.fromNamespaceAndPath("pex_test_addon", "missing_mod")).isEmpty(), "A recipe loaded even though its conditions failed");
        RecipeHolder<?> named = recipes.byKey(ResourceLocation.fromNamespaceAndPath("pex_test_addon", "named_result")).orElse(null);
        check(named != null && named.value() instanceof RefiningRecipe, "Another mod's refining recipe didn't load");
        BlockEntityEMCRefinery refinery = refinery(helper, 0);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
        relay(helper, new BlockPos(2, 1, 1));
        helper.succeedWhen(() -> {
            ItemStack output = refinery.getItems().getStackInSlot(BlockEntityEMCRefinery.OUTPUT_SLOT);
            check(output.is(net.minecraft.world.item.Items.STONE) && output.getCount() == 3, "Refined into " + output + " instead of 3 stone");
            check("Refined Stone".equals(output.getHoverName().getString()), "The result lost its components");
        });
    }

    @GameTest(template = EMPTY, batch = "emc")
    public static void refiningMapsEmc(GameTestHelper helper) {
        long iron = Platform.emcValue(net.minecraft.world.item.Items.IRON_INGOT);
        check(iron > 0, "Iron has no emc");
        // two iron and 100 emc refine into a structure void, which has no value any other way
        long value = Platform.emcValue(net.minecraft.world.item.Items.STRUCTURE_VOID);
        check(value == iron * 2 + 100, "Refined structure void is worth " + value + " instead of " + (iron * 2 + 100));
        check(Platform.emcValue(Items.IANTHIUM_INGOT.get()) == 256, "Refining changed the ianthium ingot's fixed value");
        helper.succeed();
    }
}
