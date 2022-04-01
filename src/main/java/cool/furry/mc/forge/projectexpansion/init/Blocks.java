package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.*;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Blocks {
    public static final DeferredRegister<Block> Registry = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<BlockArcaneTable> ARCANE_TABLE = Registry.register("arcane_table", BlockArcaneTable::new);
    public static final RegistryObject<BlockEMCLink> EMC_LINK = Registry.register("emc_link", BlockEMCLink::new);
    public static final RegistryObject<BlockEMCExport> EMC_EXPORT = Registry.register("emc_export", BlockEMCExport::new);
    public static final RegistryObject<BlockEMCImport> EMC_IMPORT = Registry.register("emc_import", BlockEMCImport::new);
    public static final RegistryObject<BlockTransmutationInterface> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", BlockTransmutationInterface::new);
}
