package cool.furry.mc.projectexpansion.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

import java.math.BigInteger;

// done in the emc refinery, count of the ingredient becomes the result, paying emc a bit at a time over time ticks
// assemble and getResultItem took the registries from 1.19.4, both shapes are here so it works on every version
public record RefiningRecipe(ResourceLocation id, Ingredient ingredient, int count, ItemStack result, long emc, int time) implements Recipe<Container> {
    public static RefiningRecipe fromJson(ResourceLocation id, JsonObject json) {
        Ingredient ingredient = Ingredient.fromJson(GsonHelper.isArrayNode(json, "ingredient") ? GsonHelper.getAsJsonArray(json, "ingredient") : GsonHelper.getAsJsonObject(json, "ingredient"));
        if (ingredient.isEmpty()) throw new JsonParseException("Refining recipes need an ingredient");
        int count = GsonHelper.getAsInt(json, "count", 1);
        ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
        long emc = GsonHelper.getAsLong(json, "emc");
        int time = GsonHelper.getAsInt(json, "time", 100);
        if (count <= 0 || emc <= 0 || time <= 0) throw new JsonParseException("Refining recipes need a positive count, emc and time");
        return new RefiningRecipe(id, ingredient, count, result, emc, time);
    }

    public static RefiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        return new RefiningRecipe(id, Ingredient.fromNetwork(buf), buf.readVarInt(), buf.readItem(), buf.readVarLong(), buf.readVarInt());
    }

    public static void toNetwork(FriendlyByteBuf buf, RefiningRecipe recipe) {
        recipe.ingredient.toNetwork(buf);
        buf.writeVarInt(recipe.count);
        buf.writeItem(recipe.result);
        buf.writeVarLong(recipe.emc);
        buf.writeVarInt(recipe.time);
    }

    @Override
    public boolean matches(Container input, Level level) {
        ItemStack stack = input.getItem(0);
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    public ItemStack assemble(Container input) {
        return result.copy();
    }

    public ItemStack assemble(Container input, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    public ItemStack getResultItem() {
        return result;
    }

    public ItemStack getResultItem(RegistryAccess registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.EMC_REFINERY.get());
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    // the emc due on the given tick, rounded up so the first tick always costs something and the last one finishes paying
    public long emcForTick(int tick) {
        return paidBy(tick + 1).subtract(paidBy(tick)).longValueExact();
    }

    private BigInteger paidBy(int ticks) {
        BigInteger[] result = BigInteger.valueOf(emc).multiply(BigInteger.valueOf(ticks)).divideAndRemainder(BigInteger.valueOf(time));
        return result[1].signum() == 0 ? result[0] : result[0].add(BigInteger.ONE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypes.REFINING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeTypes.REFINING.get();
    }
}
