package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.enchantments.EnchantmentAlchemicalCollection;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
public class Enchantments {
    public static final DeferredRegister<Enchantment> Registry = new DeferredRegister<>(ForgeRegistries.ENCHANTMENTS, Main.MOD_ID);

    public static final RegistryObject<EnchantmentAlchemicalCollection> ALCHEMICAL_COLLECTION = Registry.register("alchemical_collection", EnchantmentAlchemicalCollection::new);
}

