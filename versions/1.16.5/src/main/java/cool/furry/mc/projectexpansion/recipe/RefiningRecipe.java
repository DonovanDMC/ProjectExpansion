package cool.furry.mc.projectexpansion.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.JSONUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;

import java.math.BigInteger;

// done in the emc refinery, count of the ingredient becomes the result, paying emc a bit at a time over time ticks
public class RefiningRecipe implements IRecipe<IInventory> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int count;
    private final ItemStack result;
    private final long emc;
    private final int time;

    public RefiningRecipe(ResourceLocation id, Ingredient ingredient, int count, ItemStack result, long emc, int time) {
        this.id = id;
        this.ingredient = ingredient;
        this.count = count;
        this.result = result;
        this.emc = emc;
        this.time = time;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public int count() {
        return count;
    }

    public ItemStack result() {
        return result;
    }

    public long emc() {
        return emc;
    }

    public int time() {
        return time;
    }

    public static RefiningRecipe fromJson(ResourceLocation id, JsonObject json) {
        Ingredient ingredient = Ingredient.fromJson(JSONUtils.isArrayNode(json, "ingredient") ? JSONUtils.getAsJsonArray(json, "ingredient") : JSONUtils.getAsJsonObject(json, "ingredient"));
        if (ingredient.isEmpty()) throw new JsonParseException("Refining recipes need an ingredient");
        int count = JSONUtils.getAsInt(json, "count", 1);
        ItemStack result = CraftingHelper.getItemStack(JSONUtils.getAsJsonObject(json, "result"), true);
        long emc = JSONUtils.getAsLong(json, "emc");
        int time = JSONUtils.getAsInt(json, "time", 100);
        if (count <= 0 || emc <= 0 || time <= 0) throw new JsonParseException("Refining recipes need a positive count, emc and time");
        return new RefiningRecipe(id, ingredient, count, result, emc, time);
    }

    public static RefiningRecipe fromNetwork(ResourceLocation id, PacketBuffer buf) {
        return new RefiningRecipe(id, Ingredient.fromNetwork(buf), buf.readVarInt(), buf.readItem(), buf.readVarLong(), buf.readVarInt());
    }

    public static void toNetwork(PacketBuffer buf, RefiningRecipe recipe) {
        recipe.ingredient.toNetwork(buf);
        buf.writeVarInt(recipe.count);
        buf.writeItem(recipe.result);
        buf.writeVarLong(recipe.emc);
        buf.writeVarInt(recipe.time);
    }

    @Override
    public boolean matches(IInventory input, World level) {
        ItemStack stack = input.getItem(0);
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    @Override
    public ItemStack assemble(IInventory input) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
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
    public IRecipeSerializer<?> getSerializer() {
        return RecipeTypes.REFINING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return RecipeTypes.REFINING.get();
    }
}
