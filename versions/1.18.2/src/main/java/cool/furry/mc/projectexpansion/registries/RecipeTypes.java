package cool.furry.mc.projectexpansion.registries;

import com.google.gson.JsonObject;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class RecipeTypes {
    public static final DeferredRegister<RecipeType<?>> Registry = DeferredRegister.create(net.minecraft.core.Registry.RECIPE_TYPE_REGISTRY, Main.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> Serializers = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Main.MOD_ID);

    public static final RegistryObject<RecipeType<RefiningRecipe>> REFINING = Registry.register("refining", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return Main.MOD_ID + ":refining";
        }
    });
    public static final RegistryObject<Serializer> REFINING_SERIALIZER = Serializers.register("refining", Serializer::new);

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<RefiningRecipe> {
        @Override
        public RefiningRecipe fromJson(ResourceLocation id, JsonObject json) {
            return RefiningRecipe.fromJson(id, json);
        }

        @Override
        public @Nullable RefiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return RefiningRecipe.fromNetwork(id, buf);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RefiningRecipe recipe) {
            RefiningRecipe.toNetwork(buf, recipe);
        }
    }
}
