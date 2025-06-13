package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

@SuppressWarnings("unused")
public class Enchantments {
    public static final ResourceKey<Enchantment> ALCHEMICAL_COLLECTION = ResourceKey.create(Registries.ENCHANTMENT, Main.rl("alchemical_collection"));
}

