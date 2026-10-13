package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Items;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;

@IntegrationTestClass("empty")
public class IanthiumTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static Item projecte(String name) {
        return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation("projecte", name)), "No projecte:" + name);
    }

    private static List<ItemStack> drops(IntegrationTestHelper helper, Block block, ItemStack tool) {
        setBlock(helper, POS, block);
        BlockPos pos = absolute(helper, POS);
        return Block.getDrops(helper.getWorld().getBlockState(pos), helper.getWorld(), pos, null, null, tool);
    }

    @IntegrationTest("empty")
    public void oreDropsRawIanthium(IntegrationTestHelper helper) {
        Block ore = Blocks.IANTHIUM_ORE.get();
        List<ItemStack> mined = drops(helper, ore, new ItemStack(projecte("dm_pick")));
        check(mined.size() == 1 && mined.get(0).getItem() == Items.RAW_IANTHIUM.get(), "The ore dropped " + mined + " instead of raw ianthium");
        ItemStack silkTouch = new ItemStack(projecte("dm_pick"));
        silkTouch.enchant(Enchantments.SILK_TOUCH, 1);
        List<ItemStack> silk = drops(helper, ore, silkTouch);
        check(silk.size() == 1 && silk.get(0).getItem() == ore.asItem(), "The ore dropped " + silk + " with silk touch");
    }

    // the refinery is gated the same way, the player's held tool decides it before 1.17
    @IntegrationTest("empty")
    public void needMatterPickaxe(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            for (Block block : Arrays.asList(Blocks.IANTHIUM_ORE.get(), Blocks.EMC_REFINERY.get())) {
                setBlock(helper, POS, block);
                BlockPos pos = absolute(helper, POS);
                BlockState state = helper.getWorld().getBlockState(pos);
                check(state.requiresCorrectToolForDrops(), block + " drops without the right tool");
                for (Item pickaxe : Arrays.asList(net.minecraft.item.Items.IRON_PICKAXE, net.minecraft.item.Items.DIAMOND_PICKAXE, net.minecraft.item.Items.NETHERITE_PICKAXE)) {
                    player.setItemInHand(Hand.MAIN_HAND, new ItemStack(pickaxe));
                    check(!state.canHarvestBlock(helper.getWorld(), pos, player), block + " can be mined with " + pickaxe);
                }
                for (String pickaxe : Arrays.asList("dm_pick", "rm_pick")) {
                    player.setItemInHand(Hand.MAIN_HAND, new ItemStack(projecte(pickaxe)));
                    check(state.canHarvestBlock(helper.getWorld(), pos, player), block + " can't be mined with projecte:" + pickaxe);
                }
            }
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void pipesNeedIanthium(IntegrationTestHelper helper) {
        IRecipe<?> recipe = helper.getWorld().getRecipeManager().byKey(Main.rl("emc_pipe")).orElse(null);
        check(recipe != null, "The emc pipe recipe is missing");
        boolean usesIanthium = recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(Items.IANTHIUM_INGOT.get())));
        check(usesIanthium, "The emc pipe recipe doesn't use ianthium");
    }

    // the ore is added to biomes as they load, see Features
    @IntegrationTest("empty")
    public void ianthiumGenerates(IntegrationTestHelper helper) {
        ConfiguredFeature<?, ?> ore = WorldGenRegistries.CONFIGURED_FEATURE.get(Main.rl("ore_ianthium"));
        check(ore != null, "The ianthium ore feature isn't registered");
        Biome plains = helper.getWorld().getServer().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOrThrow(Biomes.PLAINS);
        List<List<Supplier<ConfiguredFeature<?, ?>>>> features = plains.getGenerationSettings().features();
        int step = GenerationStage.Decoration.UNDERGROUND_ORES.ordinal();
        check(features.size() > step && features.get(step).stream().anyMatch(feature -> feature.get() == ore), "Nothing adds ianthium ore to biomes");
    }

    @IntegrationTest("empty")
    public void ianthiumIngotsAreWorthIron(IntegrationTestHelper helper) {
        long iron = emcValue(net.minecraft.item.Items.IRON_INGOT);
        check(emcValue(Items.IANTHIUM_INGOT.get()) == iron, "An ianthium ingot is worth " + emcValue(Items.IANTHIUM_INGOT.get()) + ", iron is " + iron);
        check(emcValue(Items.RAW_IANTHIUM.get()) == 0, "Raw ianthium is worth " + emcValue(Items.RAW_IANTHIUM.get()) + ", raw materials shouldn't have a value");
        check(emcValue(Items.IANTHIUM_BLOCK.get()) == iron * 9, "A block of ianthium isn't worth nine ingots");
    }
}
