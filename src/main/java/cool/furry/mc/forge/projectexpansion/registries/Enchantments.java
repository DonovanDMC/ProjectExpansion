package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.enchantments.EnchantmentAlchemicalCollection;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class Enchantments {
    public static final DeferredRegister<Enchantment> Registry = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Main.MOD_ID);

    public static final RegistryObject<EnchantmentAlchemicalCollection> ALCHEMICAL_COLLECTION = Registry.register("alchemical_collection", EnchantmentAlchemicalCollection::new);
}

