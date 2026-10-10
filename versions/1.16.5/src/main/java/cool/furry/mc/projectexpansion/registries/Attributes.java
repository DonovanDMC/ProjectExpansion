package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Attributes {
    public static final DeferredRegister<Attribute> Registry = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Main.MOD_ID);

    public static final RegistryObject<Attribute> SUN_EXPOSURE_PROTECTION = Registry.register("sun_exposure_protection", () -> new RangedAttribute(String.format("attribute.%s.sun_exposure_protection", Main.MOD_ID), 0, 0, 1).setSyncable(true));
}
