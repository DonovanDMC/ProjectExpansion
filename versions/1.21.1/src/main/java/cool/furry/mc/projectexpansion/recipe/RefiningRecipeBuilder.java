package cool.furry.mc.projectexpansion.recipe;

import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

// for other mods' datagen, refining recipes are plain json so datapacks can add them too
public class RefiningRecipeBuilder implements RecipeBuilder {
    private final Ingredient ingredient;
    private final ItemStack result;
    private final long emc;
    private int count = 1;
    private int time = 100;

    private RefiningRecipeBuilder(Ingredient ingredient, ItemStack result, long emc) {
        this.ingredient = ingredient;
        this.result = result;
        this.emc = emc;
    }

    public static RefiningRecipeBuilder refining(Ingredient ingredient, ItemStack result, long emc) {
        return new RefiningRecipeBuilder(ingredient, result, emc);
    }

    public RefiningRecipeBuilder count(int count) {
        this.count = count;
        return this;
    }

    public RefiningRecipeBuilder time(int time) {
        this.time = time;
        return this;
    }

    // refining recipes aren't in the recipe book, so they have no unlock advancement
    @Override
    public RefiningRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        return this;
    }

    @Override
    public RefiningRecipeBuilder group(@Nullable String group) {
        return this;
    }

    @Override
    public Item getResult() {
        return result.getItem();
    }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(id, new RefiningRecipe(ingredient, count, result, emc, time), null);
    }
}
