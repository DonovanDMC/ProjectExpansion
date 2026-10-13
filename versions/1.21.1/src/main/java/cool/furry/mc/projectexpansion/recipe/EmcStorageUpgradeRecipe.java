package cool.furry.mc.projectexpansion.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.math.BigInteger;

// a shaped recipe that keeps the emc stored in the emc storage it's made from
public class EmcStorageUpgradeRecipe extends ShapedRecipe {
    // ShapedRecipe keeps these to itself, so they're kept here too for the serializer
    private final ShapedRecipePattern pattern;
    private final ItemStack result;

    public EmcStorageUpgradeRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result) {
        super(group, category, pattern, result);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack output = super.assemble(input, registries);
        BigInteger stored = BigInteger.ZERO;
        for (ItemStack stack : input.items()) {
            BigInteger emc = stack.get(DataComponentTypes.STORED_EMC.get());
            if (emc != null) stored = stored.add(emc);
        }
        if (stored.signum() > 0) output.set(DataComponentTypes.STORED_EMC.get(), stored);
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypes.EMC_STORAGE_UPGRADE_SERIALIZER.get();
    }

    // the same json as a shaped recipe
    public static class Serializer implements RecipeSerializer<EmcStorageUpgradeRecipe> {
        private static final MapCodec<EmcStorageUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, EmcStorageUpgradeRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, EmcStorageUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
            ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
            ItemStack.STREAM_CODEC, recipe -> recipe.result,
            EmcStorageUpgradeRecipe::new
        );

        @Override
        public MapCodec<EmcStorageUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EmcStorageUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
