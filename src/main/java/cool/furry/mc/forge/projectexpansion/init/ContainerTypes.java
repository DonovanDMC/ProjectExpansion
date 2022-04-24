package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ContainerTypes {
    public static final DeferredRegister<ContainerType<?>> Registry = new DeferredRegister(ForgeRegistries.CONTAINERS, Main.MOD_ID);
}
