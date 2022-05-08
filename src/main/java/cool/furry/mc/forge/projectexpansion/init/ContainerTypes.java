package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.container.ContainerMatterReplicator;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
public class ContainerTypes {
    public static final DeferredRegister<ContainerType<?>> Registry = DeferredRegister.create(ForgeRegistries.CONTAINERS, Main.MOD_ID);

    public static final RegistryObject<ContainerType<ContainerMatterReplicator>> MATTER_REPLICATOR = Registry.register("matter_replicator", () -> IForgeContainerType.create(ContainerMatterReplicator::new));
}
