package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.tile.*;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

@SuppressWarnings({"unused", "ConstantConditions"})
public class TileEntityTypes {
    public static final DeferredRegister<TileEntityType<?>> Registry = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Main.MOD_ID);

    public static final RegistryObject<TileEntityType<TileEMCLink>> EMC_LINK = Registry.register("emc_link", () -> TileEntityType.Builder.of(TileEMCLink::new, Arrays.stream(Matter.VALUES).map(Matter::getEMCLink).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TilePowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> TileEntityType.Builder.of(TilePowerFlower::new, Arrays.stream(Matter.VALUES).map(Matter::getPowerFlower).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileCollector>> COLLECTOR = Registry.register("collector", () -> TileEntityType.Builder.of(TileCollector::new, Arrays.stream(Matter.VALUES).map(Matter::getCollector).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileRelay>> RELAY = Registry.register("relay", () -> TileEntityType.Builder.of(TileRelay::new, Arrays.stream(Matter.VALUES).map(Matter::getRelay).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileTransmutationInterface>> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> TileEntityType.Builder.of(TileTransmutationInterface::new, Blocks.TRANSMUTATION_INTERFACE.get()).build(null));
}