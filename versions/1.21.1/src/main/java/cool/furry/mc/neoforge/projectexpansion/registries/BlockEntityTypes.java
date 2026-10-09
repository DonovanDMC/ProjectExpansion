package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.entity.*;
import cool.furry.mc.neoforge.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.neoforge.projectexpansion.util.Matter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;

@SuppressWarnings({"unused", "ConstantConditions"})
public class BlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> Registry = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Main.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityEMCLink>> EMC_LINK = Registry.register("emc_link", () -> BlockEntityType.Builder.of(BlockEntityEMCLink::new, Arrays.stream(Matter.VALUES).map(Matter::getEMCLink).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityPowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> BlockEntityType.Builder.of(BlockEntityPowerFlower::new, Arrays.stream(Matter.VALUES).map(Matter::getPowerFlower).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityCollector>> COLLECTOR = Registry.register("collector", () -> BlockEntityType.Builder.of(BlockEntityCollector::new, Arrays.stream(Matter.VALUES).map(Matter::getCollector).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityRelay>> RELAY = Registry.register("relay", () -> BlockEntityType.Builder.of(BlockEntityRelay::new, Arrays.stream(Matter.VALUES).map(Matter::getRelay).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityTransmutationInterface>> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> BlockEntityType.Builder.of(BlockEntityTransmutationInterface::new, Blocks.TRANSMUTATION_INTERFACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityAdvancedAlchemicalChest>> ADVANCED_ALCHEMICAL_CHEST = Registry.register("advanced_alchemical_chest", () -> BlockEntityType.Builder.of(BlockEntityAdvancedAlchemicalChest::new, Arrays.stream(AdvancedAlchemicalChest.getBlocks()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityCondenserMK3>> CONDENSER_MK3 = Registry.register("condenser_mk3", () -> BlockEntityType.Builder.of(BlockEntityCondenserMK3::new, Blocks.CONDENSER_MK3.get()).build(null));
}
