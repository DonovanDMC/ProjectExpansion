package cool.furry.mc.projectexpansion.registries;

import com.google.gson.JsonObject;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.recipe.EmcStorageUpgradeRecipe;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.fml.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class RecipeTypes {
    public static final DeferredRegister<IRecipeSerializer<?>> Serializers = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Main.MOD_ID);

    // recipe types aren't a forge registry before 1.17, it's registered along with its serializer
    public static final Lazy<IRecipeType<RefiningRecipe>> REFINING = Lazy.of(() -> IRecipeType.register(Main.MOD_ID + ":refining"));
    public static final RegistryObject<Serializer> REFINING_SERIALIZER = Serializers.register("refining", () -> {
        REFINING.get();
        return new Serializer();
    });
    public static final RegistryObject<StorageUpgradeSerializer> EMC_STORAGE_UPGRADE_SERIALIZER = Serializers.register("emc_storage_upgrade", StorageUpgradeSerializer::new);

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<RefiningRecipe> {
        @Override
        public RefiningRecipe fromJson(ResourceLocation id, JsonObject json) {
            return RefiningRecipe.fromJson(id, json);
        }

        @Override
        public @Nullable RefiningRecipe fromNetwork(ResourceLocation id, PacketBuffer buf) {
            return RefiningRecipe.fromNetwork(id, buf);
        }

        @Override
        public void toNetwork(PacketBuffer buf, RefiningRecipe recipe) {
            RefiningRecipe.toNetwork(buf, recipe);
        }
    }

    // the same json and packet as a shaped recipe
    public static class StorageUpgradeSerializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<EmcStorageUpgradeRecipe> {
        @Override
        public EmcStorageUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new EmcStorageUpgradeRecipe(IRecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public @Nullable EmcStorageUpgradeRecipe fromNetwork(ResourceLocation id, PacketBuffer buf) {
            return new EmcStorageUpgradeRecipe(IRecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(PacketBuffer buf, EmcStorageUpgradeRecipe recipe) {
            IRecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
