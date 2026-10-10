package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class IanthiumGameTests {
    private static List<ItemStack> drops(GameTestHelper helper, Block block, ItemStack tool) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, block);
        BlockPos absolute = helper.absolutePos(pos);
        return Block.getDrops(helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute, null, null, tool);
    }

    @GameTest(template = EMPTY)
    public static void oresDropRawIanthium(GameTestHelper helper) {
        ItemStack silkTouch = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("projecte", "dm_pick")));
        silkTouch.enchant(Enchantments.SILK_TOUCH, 1);
        for (Block ore : List.of(Blocks.IANTHIUM_ORE.get(), Blocks.DEEPSLATE_IANTHIUM_ORE.get())) {
            List<ItemStack> mined = drops(helper, ore, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("projecte", "dm_pick"))));
            check(mined.size() == 1 && mined.get(0).is(Items.RAW_IANTHIUM.get()), ore + " dropped " + mined + " instead of raw ianthium");
            List<ItemStack> silk = drops(helper, ore, silkTouch);
            check(silk.size() == 1 && silk.get(0).is(ore.asItem()), ore + " dropped " + silk + " with silk touch");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void needMatterPickaxe(GameTestHelper helper) {
        // the refinery is gated the same way
        for (Block ore : List.of(Blocks.IANTHIUM_ORE.get(), Blocks.DEEPSLATE_IANTHIUM_ORE.get(), Blocks.EMC_REFINERY.get())) {
            BlockState state = ore.defaultBlockState();
            check(state.requiresCorrectToolForDrops(), ore + " drops without the right tool");
            for (Item pickaxe : List.of(net.minecraft.world.item.Items.IRON_PICKAXE, net.minecraft.world.item.Items.DIAMOND_PICKAXE, net.minecraft.world.item.Items.NETHERITE_PICKAXE)) {
                check(!new ItemStack(pickaxe).isCorrectToolForDrops(state), ore + " can be mined with " + pickaxe);
            }
            for (String pickaxe : List.of("dm_pick", "rm_pick")) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("projecte", pickaxe));
                check(new ItemStack(item).isCorrectToolForDrops(state), ore + " can't be mined with projecte:" + pickaxe);
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pipesNeedIanthium(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(Main.rl("emc_pipe")).orElse(null);
        check(recipe != null, "The emc pipe recipe is missing");
        boolean usesIanthium = recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(Items.IANTHIUM_INGOT.get())));
        check(usesIanthium, "The emc pipe recipe doesn't use ianthium");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ianthiumGenerates(GameTestHelper helper) {
        check(Platform.hasPlacedFeature(helper.getLevel(), Main.rl("ore_ianthium")), "The ianthium ore feature isn't registered");
        check(Platform.hasOreModifier(helper.getLevel(), Main.rl("ore_ianthium")), "Nothing adds ianthium ore to biomes");
        helper.succeed();
    }

    // in the emc batch so ProjectE has mapped values first
    @GameTest(template = EMPTY, batch = "emc")
    public static void ianthiumIngotsAreWorthIron(GameTestHelper helper) {
        long iron = Platform.emcValue(net.minecraft.world.item.Items.IRON_INGOT);
        check(Platform.emcValue(Items.IANTHIUM_INGOT.get()) == iron, "An ianthium ingot is worth " + Platform.emcValue(Items.IANTHIUM_INGOT.get()) + ", iron is " + iron);
        check(Platform.emcValue(Items.RAW_IANTHIUM.get()) == 0, "Raw ianthium is worth " + Platform.emcValue(Items.RAW_IANTHIUM.get()) + ", raw materials shouldn't have a value");
        check(Platform.emcValue(Items.IANTHIUM_BLOCK.get()) == iron * 9, "A block of ianthium isn't worth nine ingots");
        helper.succeed();
    }
}
