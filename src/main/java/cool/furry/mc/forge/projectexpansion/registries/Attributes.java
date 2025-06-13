package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Attributes {
    public static final DeferredRegister<Attribute> Registry = DeferredRegister.create(Registries.ATTRIBUTE, Main.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> SUN_EXPOSURE_PROTECTION = Registry.register("sun_exposure_protection", () -> new PercentageAttribute(String.format("attribute.%s.sun_exposure_protection", Main.MOD_ID), 0, 0, 1).setSyncable(true));
}
