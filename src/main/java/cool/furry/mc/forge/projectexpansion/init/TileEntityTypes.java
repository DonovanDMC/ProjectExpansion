package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.tile.*;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

public class TileEntityTypes {
    public static final DeferredRegister<TileEntityType<?>> Registry = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Main.MOD_ID);

    public static final RegistryObject<TileEntityType<TileEMCLink>> emc_link = Registry.register("emc_link", () -> TileEntityType.Builder.create(TileEMCLink::new, Blocks.EMC_LINK.get()).build(null));
    public static final RegistryObject<TileEntityType<TilePowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> TileEntityType.Builder.create(TilePowerFlower::new, Arrays.stream(Matter.VALUES).map(Matter::getPowerFlower).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileCollector>> ENERGY_COLLECTOR = Registry.register("collector", () -> TileEntityType.Builder.create(TileCollector::new, Arrays.stream(Matter.VALUES).map(Matter::getCollector).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileRelay>> ANTI_MATTER_RELAY = Registry.register("relay", () -> TileEntityType.Builder.create(TileRelay::new, Arrays.stream(Matter.VALUES).map(Matter::getRelay).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileEMCExport>> EMC_EXPORT = Registry.register("emc_export", () -> TileEntityType.Builder.create(TileEMCExport::new, Blocks.EMC_EXPORT.get()).build(null));
    public static final RegistryObject<TileEntityType<TileEMCImport>> EMC_IMPORT = Registry.register("emc_import", () -> TileEntityType.Builder.create(TileEMCImport::new, Blocks.EMC_IMPORT.get()).build(null));
}
