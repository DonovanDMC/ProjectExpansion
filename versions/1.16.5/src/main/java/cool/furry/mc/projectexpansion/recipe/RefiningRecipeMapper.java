package cool.furry.mc.projectexpansion.recipe;

import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;

// lets projecte work out values through the refinery, picked up by projecte's annotation scan
@RecipeTypeMapper
public class RefiningRecipeMapper implements IRecipeTypeMapper {
    @Override
    public String getName() {
        return RefiningMapping.NAME;
    }

    @Override
    public String getDescription() {
        return RefiningMapping.DESCRIPTION;
    }

    @Override
    public boolean canHandle(IRecipeType<?> type) {
        return type == RecipeTypes.REFINING.get();
    }

    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, IRecipe<?> recipe, INSSFakeGroupManager fakeGroupManager) {
        return RefiningMapping.handle(mapper, recipe, fakeGroupManager);
    }
}
