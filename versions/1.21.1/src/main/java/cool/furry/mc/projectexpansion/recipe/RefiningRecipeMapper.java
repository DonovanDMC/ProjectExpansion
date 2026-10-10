package cool.furry.mc.projectexpansion.recipe;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSFake;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

// lets projecte work out values through the refinery, picked up by projecte's annotation scan
@RecipeTypeMapper
public class RefiningRecipeMapper implements IRecipeTypeMapper {
    // the emc a recipe costs is added as a fake ingredient, split in two so costs past an int still fit
    private static final long HIGH_UNIT = 1L << 30;
    private ModConfigSpec.BooleanValue includeEmcCost;
    private final Map<IMappingCollector<?, ?>, Set<String>> valued = new WeakHashMap<>();

    @Override
    public String getName() {
        return "ProjectExpansionRefiningMapper";
    }

    @Override
    public String getTranslationKey() {
        return "config.projectexpansion.mapper.refining";
    }

    @Override
    public String getDescription() {
        return "Maps EMC Refinery recipes from Project Expansion.";
    }

    @Override
    public void addConfigOptions(ModConfigSpec.Builder builder) {
        includeEmcCost = builder
                .comment("Whether the EMC a refining recipe costs is added to the value of what it makes.")
                .translation("config.projectexpansion.mapper.refining.include_emc_cost")
                .define("include_emc_cost", true);
    }

    @Override
    public boolean canHandle(RecipeType<?> type) {
        return type == RecipeTypes.REFINING.get();
    }

    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> holder, RegistryAccess registryAccess, INSSFakeGroupManager fakeGroupManager) {
        if (!(holder.value() instanceof RefiningRecipe recipe) || recipe.result().isEmpty()) return false;
        Set<NormalizedSimpleStack> matches = new LinkedHashSet<>();
        for (ItemStack stack : recipe.ingredient().getItems()) {
            if (!stack.isEmpty()) matches.add(NSSItem.createItem(stack));
        }
        if (matches.isEmpty()) return false;

        Object2IntMap<NormalizedSimpleStack> ingredients = new Object2IntOpenHashMap<>();
        if (matches.size() == 1) {
            ingredients.put(matches.iterator().next(), recipe.count());
        } else {
            // anything from a tag works, so the cheapest of them sets the value
            INSSFakeGroupManager.FakeGroupData group = fakeGroupManager.getOrCreateFakeGroup(matches);
            if (group.created()) {
                for (NormalizedSimpleStack match : matches) mapper.addConversion(1, group.dummy(), Object2IntMaps.singleton(match, 1));
            }
            ingredients.put(group.dummy(), recipe.count());
        }

        if (includeEmcCost == null || includeEmcCost.get()) {
            long high = recipe.emc() / HIGH_UNIT, low = recipe.emc() % HIGH_UNIT;
            if (high > Integer.MAX_VALUE) return false;
            if (high > 0) ingredients.put(emcUnit(mapper, "emc_high", HIGH_UNIT), (int) high);
            if (low > 0) ingredients.put(emcUnit(mapper, "emc", 1), (int) low);
        }

        mapper.addConversion(recipe.result().getCount(), NSSItem.createItem(recipe.result()), ingredients);
        return true;
    }

    private NormalizedSimpleStack emcUnit(IMappingCollector<NormalizedSimpleStack, Long> mapper, String name, long value) {
        NSSFake fake = NSSFake.create(Main.MOD_ID + ":refining_" + name);
        // each mapping run gets a new collector, the value only needs setting once in it
        if (valued.computeIfAbsent(mapper, m -> new HashSet<>()).add(name)) mapper.setValueBefore(fake, value);
        return fake;
    }
}
