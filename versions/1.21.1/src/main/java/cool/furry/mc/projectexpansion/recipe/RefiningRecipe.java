package cool.furry.mc.projectexpansion.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.math.BigInteger;

// done in the emc refinery, count of the ingredient becomes the result, paying emc a bit at a time over time ticks
public record RefiningRecipe(Ingredient ingredient, int count, ItemStack result, long emc, int time) implements Recipe<SingleRecipeInput> {
    private static final Codec<Long> POSITIVE_LONG = Codec.LONG.validate(value -> value > 0 ? DataResult.success(value) : DataResult.error(() -> "Value must be positive: " + value));
    public static final MapCodec<RefiningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(RefiningRecipe::ingredient),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(RefiningRecipe::count),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(RefiningRecipe::result),
            POSITIVE_LONG.fieldOf("emc").forGetter(RefiningRecipe::emc),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 100).forGetter(RefiningRecipe::time)
    ).apply(instance, RefiningRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RefiningRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, RefiningRecipe::ingredient,
            ByteBufCodecs.VAR_INT, RefiningRecipe::count,
            ItemStack.STREAM_CODEC, RefiningRecipe::result,
            ByteBufCodecs.VAR_LONG, RefiningRecipe::emc,
            ByteBufCodecs.VAR_INT, RefiningRecipe::time,
            RefiningRecipe::new
    );

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= count;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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

    public static class Serializer implements RecipeSerializer<RefiningRecipe> {
        @Override
        public MapCodec<RefiningRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RefiningRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
