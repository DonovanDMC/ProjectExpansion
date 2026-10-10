package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RecipeTypes {
    public static final DeferredRegister<RecipeType<?>> Registry = DeferredRegister.create(Registries.RECIPE_TYPE, Main.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> Serializers = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Main.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RefiningRecipe>> REFINING = Registry.register("refining", () -> RecipeType.simple(Main.rl("refining")));
    public static final DeferredHolder<RecipeSerializer<?>, RefiningRecipe.Serializer> REFINING_SERIALIZER = Serializers.register("refining", RefiningRecipe.Serializer::new);
}
