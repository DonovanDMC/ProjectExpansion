package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.container.ContainerEMCExport;
import cool.furry.mc.forge.projectexpansion.container.ContainerEMCImport;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ContainerTypes {
    public static final DeferredRegister<ContainerType<?>> Registry = DeferredRegister.create(ForgeRegistries.CONTAINERS, Main.MOD_ID);

    public static final RegistryObject<ContainerType<ContainerEMCExport>> EMC_EXPORT = Registry.register("emc_export", () -> IForgeContainerType.create(ContainerEMCExport::new));
    public static final RegistryObject<ContainerType<ContainerEMCImport>> EMC_IMPORT = Registry.register("emc_import", () -> IForgeContainerType.create(ContainerEMCImport::new));
}
