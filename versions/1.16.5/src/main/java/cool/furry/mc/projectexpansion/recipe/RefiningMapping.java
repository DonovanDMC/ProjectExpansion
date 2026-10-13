package cool.furry.mc.projectexpansion.recipe;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.nss.NSSFake;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.util.Tuple;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

// what each version's RefiningRecipeMapper does, projecte's mapper interface changed shape between versions
public class RefiningMapping {
    // the emc a recipe costs is added as a fake ingredient, split in two so costs past an int still fit
    private static final long HIGH_UNIT = 1L << 30;
    private static final Map<IMappingCollector<?, ?>, Set<String>> VALUED = new WeakHashMap<>();

    public static final String NAME = "ProjectExpansionRefiningMapper";
    public static final String DESCRIPTION = "Maps EMC Refinery recipes from Project Expansion.";

    public static boolean handle(IMappingCollector<NormalizedSimpleStack, Long> mapper, IRecipe<?> value, INSSFakeGroupManager fakeGroupManager) {
        if (!(value instanceof RefiningRecipe) || ((RefiningRecipe) value).result().isEmpty()) return false;
        RefiningRecipe recipe = (RefiningRecipe) value;
        Set<NormalizedSimpleStack> matches = new LinkedHashSet<>();
        for (ItemStack stack : recipe.ingredient().getItems()) {
            if (!stack.isEmpty()) matches.add(NSSItem.createItem(stack));
        }
        if (matches.isEmpty()) return false;

        Map<NormalizedSimpleStack, Integer> ingredients = new HashMap<>();
        if (matches.size() == 1) {
            ingredients.put(matches.iterator().next(), recipe.count());
        } else {
            // anything from a tag works, so the cheapest of them sets the value
            Tuple<NormalizedSimpleStack, Boolean> group = fakeGroupManager.getOrCreateFakeGroup(matches);
            if (group.getB()) {
                // projecte looks up null in these, which immutable maps don't allow
                for (NormalizedSimpleStack match : matches) {
                    Map<NormalizedSimpleStack, Integer> single = new HashMap<>();
                    single.put(match, 1);
                    mapper.addConversion(1, group.getA(), single);
                }
            }
            ingredients.put(group.getA(), recipe.count());
        }

        if (includeEmcCost()) {
            long high = recipe.emc() / HIGH_UNIT, low = recipe.emc() % HIGH_UNIT;
            if (high > Integer.MAX_VALUE) return false;
            if (high > 0) ingredients.put(emcUnit(mapper, "emc_high", HIGH_UNIT), (int) high);
            if (low > 0) ingredients.put(emcUnit(mapper, "emc", 1), (int) low);
        }

        mapper.addConversion(recipe.result().getCount(), NSSItem.createItem(recipe.result()), ingredients);
        return true;
    }

    // projecte first maps while the world's datapacks load, which is before server configs are
    private static boolean includeEmcCost() {
        return !Config.server.Spec.isLoaded() || Config.server.refiningMapperIncludesEmcCost.get();
    }

    private static NormalizedSimpleStack emcUnit(IMappingCollector<NormalizedSimpleStack, Long> mapper, String name, long value) {
        NormalizedSimpleStack fake = NSSFake.create(Main.MOD_ID + ":refining_" + name);
        // each mapping run gets a new collector, the value only needs setting once in it
        if (VALUED.computeIfAbsent(mapper, m -> new HashSet<>()).add(name)) mapper.setValueBefore(fake, value);
        return fake;
    }
}
