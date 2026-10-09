package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Attributes {
    public static final DeferredRegister<Attribute> Registry = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Main.MOD_ID);

    public static final RegistryObject<Attribute> SUN_EXPOSURE_PROTECTION = Registry.register("sun_exposure_protection", () -> new RangedAttribute(String.format("attribute.%s.sun_exposure_protection", Main.MOD_ID), 0, 0, 1).setSyncable(true));
}
