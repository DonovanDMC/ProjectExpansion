package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockArcaneTransmutationTable;
import cool.furry.mc.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.BlockTransmutationInterface;
import cool.furry.mc.projectexpansion.util.Platform;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Blocks {
    public static final DeferredRegister<Block> Registry = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<BlockTransmutationInterface> TRANSMUTATION_INTERFACE = register("transmutation_interface", BlockTransmutationInterface::new, BlockTransmutationInterface::getProperties);
    public static final RegistryObject<BlockCompactSun> COMPACT_SUN = register("compact_sun", BlockCompactSun::new, BlockCompactSun::getProperties);
    public static final RegistryObject<BlockArcaneTransmutationTable> ARCANE_TRANSMUTATION_TABLE = register("arcane_transmutation_table", BlockArcaneTransmutationTable::new, BlockArcaneTransmutationTable::getProperties);
    public static final RegistryObject<BlockCondenserMK3> CONDENSER_MK3 = register("condenser_mk3", BlockCondenserMK3::new, BlockCondenserMK3::getProperties);
    public static final RegistryObject<BlockEMCRefinery> EMC_REFINERY = register("emc_refinery", BlockEMCRefinery::new, BlockEMCRefinery::getProperties);
    public static final RegistryObject<BlockEmcPipe> EMC_PIPE = register("emc_pipe", BlockEmcPipe::new, BlockEmcPipe::getProperties);
    // the ores drop no experience, same as raw iron ores
    public static final RegistryObject<Block> IANTHIUM_ORE = register("ianthium_ore", Block::new, () -> BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.IRON_ORE));
    public static final RegistryObject<Block> DEEPSLATE_IANTHIUM_ORE = register("deepslate_ianthium_ore", Block::new, () -> BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE));
    public static final RegistryObject<Block> RAW_IANTHIUM_BLOCK = register("raw_ianthium_block", Block::new, () -> Platform.mapColor(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.RAW_IRON_BLOCK), DyeColor.PURPLE));
    public static final RegistryObject<Block> IANTHIUM_BLOCK = register("ianthium_block", Block::new, () -> Platform.mapColor(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.IRON_BLOCK), DyeColor.PURPLE));

    public static <T extends Block> RegistryObject<T> register(String name, Function<BlockBehaviour.Properties, T> createBlock, Supplier<BlockBehaviour.Properties> properties) {
        return Registry.register(name, () -> createBlock.apply(properties.get()));
    }
}
