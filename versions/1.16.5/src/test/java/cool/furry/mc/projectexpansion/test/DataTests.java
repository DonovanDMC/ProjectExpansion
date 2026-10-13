package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.block.Block;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.RegistryObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;

@IntegrationTestClass("empty")
public class DataTests {
    @IntegrationTest("empty")
    public void everyBlockDropsItself(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos world = absolute(helper, pos);
        ServerWorld level = helper.getWorld();
        List<String> problems = new ArrayList<>();
        for (RegistryObject<Block> entry : Blocks.Registry.getEntries()) {
            Block block = entry.get();
            // the ore drops its raw material instead, see IanthiumTests
            Item item = block == Blocks.IANTHIUM_ORE.get() ? cool.furry.mc.projectexpansion.registries.Items.RAW_IANTHIUM.get() : block.asItem();
            if (item == Items.AIR) {
                problems.add(entry.getId() + " has no item");
                continue;
            }
            setBlock(helper, pos, block);
            List<ItemStack> drops = Block.getDrops(level.getBlockState(world), level, world, level.getBlockEntity(world));
            if (drops.stream().noneMatch(stack -> stack.getItem() == item)) problems.add(entry.getId() + " dropped " + drops);
        }
        check(problems.isEmpty(), String.join(", ", problems));
    }

    @IntegrationTest("empty")
    public void recipesLoad(IntegrationTestHelper helper) {
        List<String> expected = new ArrayList<>(Arrays.asList(
            "collector/basic", "relay/basic", "collector/mk1_conversion", "relay/mk3_conversion",
            "basic_alchemical_book", "arcane_transmutation_table", "arcane_transmutation_tablet",
            "compact_sun_1", "condenser_mk3", "transmutation_interface", "star/final_star", "star/magnum/ein",
            "emc_storage/basic", "emc_storage/improved", "emc_storage/advanced", "emc_storage/ultimate", "emc_storage/infinity"
        ));
        for (Matter matter : Matter.VALUES) {
            String tier = matter.getSerializedName();
            expected.addAll(Arrays.asList("emc_link/" + tier, "power_flower/" + tier, "collector/compressed/" + tier));
            if (matter != Matter.BASIC) expected.addAll(Arrays.asList("collector/" + tier, "relay/" + tier));
            if (matter.hasItem) expected.add("matter/" + tier);
        }
        for (Fuel fuel : Fuel.values()) {
            if (fuel.getItem() != null) expected.addAll(Arrays.asList("fuel/item/" + fuel.name().toLowerCase(Locale.ROOT), "fuel/block/" + fuel.name().toLowerCase(Locale.ROOT)));
        }
        for (DyeColor color : DyeColor.values()) expected.add("advanced_alchemical_chest/" + color.getName());

        List<String> missing = expected.stream().filter(id -> !helper.getWorld().getRecipeManager().byKey(Main.rl(id)).isPresent()).collect(Collectors.toList());
        check(missing.isEmpty(), "Missing recipes: " + missing);
    }

    @IntegrationTest("empty")
    public void tagsLoad(IntegrationTestHelper helper) {
        ITag<Item> collectors = Objects.requireNonNull(ItemTags.getAllTags().getTag(Main.rl("collector")), "The collector tag didn't load");
        for (Matter matter : Matter.VALUES) {
            check(collectors.contains(Objects.requireNonNull(matter.getCollectorItem())), matter + " collector isn't in " + Main.rl("collector"));
        }
        for (DyeColor color : DyeColor.values()) {
            check(Tags.Items.CHESTS.contains(AdvancedAlchemicalChest.getBlock(color).asItem()), color + " advanced alchemical chest isn't in " + Tags.Items.CHESTS.getName());
        }
    }

    @IntegrationTest("empty")
    public void advancementsLoad(IntegrationTestHelper helper) {
        for (String id : Arrays.asList("compact_sun", "collector", "magenta_matter", "pink_fuel", "knowledge_sharing_book", "blinded_by_the_light", "ianthium", "emc_refinery", "ianthium_ingot", "emc_pipe", "wrench", "pipe_upgrade", "infinity_pipe_upgrade", "emc_storage")) {
            ResourceLocation location = Main.rl(id);
            check(helper.getWorld().getServer().getAdvancements().getAdvancement(location) != null, "Missing advancement " + location);
        }
    }
}
