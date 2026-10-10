package cool.furry.mc.neoforge.projectexpansion.gametest;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.registries.Blocks;
import cool.furry.mc.neoforge.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.neoforge.projectexpansion.util.Fuel;
import cool.furry.mc.neoforge.projectexpansion.util.Matter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static cool.furry.mc.neoforge.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.neoforge.projectexpansion.gametest.GameTestUtil.check;

// checks that the generated data for this version loads, see src/generation
@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class DataGameTests {
    @GameTest(template = EMPTY)
    public static void everyBlockDropsItself(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        List<String> problems = new ArrayList<>();
        for (DeferredHolder<Block, ? extends Block> entry : Blocks.Registry.getEntries()) {
            Block block = entry.get();
            Item item = block.asItem();
            if (item == Items.AIR) {
                problems.add(entry.getId() + " has no item");
                continue;
            }
            helper.setBlock(pos, block);
            BlockPos absolute = helper.absolutePos(pos);
            List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute, helper.getLevel().getBlockEntity(absolute));
            if (drops.stream().noneMatch(stack -> stack.is(item))) problems.add(entry.getId() + " dropped " + drops);
        }
        check(problems.isEmpty(), String.join(", ", problems));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void recipesLoad(GameTestHelper helper) {
        List<String> expected = new ArrayList<>(List.of(
            "collector/basic", "relay/basic", "collector/mk1_conversion", "relay/mk3_conversion",
            "basic_alchemical_book", "arcane_transmutation_table", "arcane_transmutation_tablet",
            "compact_sun_1", "condenser_mk3", "transmutation_interface", "star/final_star", "star/magnum/ein"
        ));
        for (Matter matter : Matter.VALUES) {
            String tier = matter.getSerializedName();
            expected.addAll(List.of("emc_link/" + tier, "power_flower/" + tier, "collector/compressed/" + tier));
            if (matter != Matter.BASIC) expected.addAll(List.of("collector/" + tier, "relay/" + tier));
            if (matter.hasItem) expected.add("matter/" + tier);
        }
        for (Fuel fuel : Fuel.values()) {
            if (fuel.getItem() != null) expected.addAll(List.of("fuel/item/" + fuel.name().toLowerCase(Locale.ROOT), "fuel/block/" + fuel.name().toLowerCase(Locale.ROOT)));
        }
        for (DyeColor color : DyeColor.values()) expected.add("advanced_alchemical_chest/" + color.getName());

        List<String> missing = expected.stream().filter(id -> helper.getLevel().getRecipeManager().byKey(Main.rl(id)).isEmpty()).toList();
        check(missing.isEmpty(), "Missing recipes: " + missing);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tagsLoad(GameTestHelper helper) {
        TagKey<Item> collectors = TagKey.create(Registries.ITEM, Main.rl("collector"));
        for (Matter matter : Matter.VALUES) {
            check(new ItemStack(Objects.requireNonNull(matter.getCollectorItem())).is(collectors), matter + " collector isn't in " + collectors.location());
        }
        for (DyeColor color : DyeColor.values()) {
            check(new ItemStack(AdvancedAlchemicalChest.getBlock(color)).is(Tags.Items.CHESTS), color + " advanced alchemical chest isn't in " + Tags.Items.CHESTS.location());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void advancementsLoad(GameTestHelper helper) {
        for (String id : List.of("compact_sun", "collector", "magenta_matter", "pink_fuel", "knowledge_sharing_book", "blinded_by_the_light")) {
            ResourceLocation location = Main.rl(id);
            check(Platform.hasAdvancement(helper.getLevel().getServer(), location), "Missing advancement " + location);
        }
        helper.succeed();
    }
}
