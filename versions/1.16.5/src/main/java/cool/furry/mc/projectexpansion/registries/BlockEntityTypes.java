package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.*;
import cool.furry.mc.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

@SuppressWarnings({"unused", "ConstantConditions"})
public class BlockEntityTypes {
    public static final DeferredRegister<TileEntityType<?>> Registry = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Main.MOD_ID);

    public static final RegistryObject<TileEntityType<BlockEntityEMCLink>> EMC_LINK = Registry.register("emc_link", () -> TileEntityType.Builder.of(BlockEntityEMCLink::new, Arrays.stream(Matter.VALUES).map(Matter::getEMCLink).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityPowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> TileEntityType.Builder.of(BlockEntityPowerFlower::new, Arrays.stream(Matter.VALUES).map(Matter::getPowerFlower).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityCollector>> COLLECTOR = Registry.register("collector", () -> TileEntityType.Builder.of(BlockEntityCollector::new, Arrays.stream(Matter.VALUES).map(Matter::getCollector).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityRelay>> RELAY = Registry.register("relay", () -> TileEntityType.Builder.of(BlockEntityRelay::new, Arrays.stream(Matter.VALUES).map(Matter::getRelay).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityTransmutationInterface>> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> TileEntityType.Builder.of(BlockEntityTransmutationInterface::new, Blocks.TRANSMUTATION_INTERFACE.get()).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityAdvancedAlchemicalChest>> ADVANCED_ALCHEMICAL_CHEST = Registry.register("advanced_alchemical_chest", () -> TileEntityType.Builder.of(BlockEntityAdvancedAlchemicalChest::new, Arrays.stream(AdvancedAlchemicalChest.getBlocks()).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityCondenserMK3>> CONDENSER_MK3 = Registry.register("condenser_mk3", () -> TileEntityType.Builder.of(BlockEntityCondenserMK3::new, Blocks.CONDENSER_MK3.get()).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityEMCRefinery>> EMC_REFINERY = Registry.register("emc_refinery", () -> TileEntityType.Builder.of(BlockEntityEMCRefinery::new, Blocks.EMC_REFINERY.get()).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityEmcStorage>> EMC_STORAGE = Registry.register("emc_storage", () -> TileEntityType.Builder.of(BlockEntityEmcStorage::new, Blocks.EMC_STORAGE.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<BlockEntityEmcPipe>> EMC_PIPE = Registry.register("emc_pipe", () -> TileEntityType.Builder.of(BlockEntityEmcPipe::new, Blocks.EMC_PIPE.get()).build(null));
}
