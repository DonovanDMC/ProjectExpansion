package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.entity.*;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;

@SuppressWarnings("ConstantConditions")
public class BlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> Registry = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, Main.MOD_ID);

    public static final RegistryObject<BlockEntityType<BlockEntityEMCLink>> EMC_LINK = Registry.register("emc_link", () -> BlockEntityType.Builder.of(BlockEntityEMCLink::new, Arrays.stream(Matter.VALUES).map(Matter::getEMCLink).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityPowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> BlockEntityType.Builder.of(BlockEntityPowerFlower::new, Arrays.stream(Matter.VALUES).map(Matter::getPowerFlower).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityCollector>> COLLECTOR = Registry.register("collector", () -> BlockEntityType.Builder.of(BlockEntityCollector::new, Arrays.stream(Matter.VALUES).map(Matter::getCollector).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityRelay>> RELAY = Registry.register("relay", () -> BlockEntityType.Builder.of(BlockEntityRelay::new, Arrays.stream(Matter.VALUES).map(Matter::getRelay).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<BlockEntityTransmutationInterface>> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> BlockEntityType.Builder.of(BlockEntityTransmutationInterface::new, Blocks.TRANSMUTATION_INTERFACE.get()).build(null));
}
