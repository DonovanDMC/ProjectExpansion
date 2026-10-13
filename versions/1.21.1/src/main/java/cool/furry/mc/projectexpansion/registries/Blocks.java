package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockArcaneTransmutationTable;
import cool.furry.mc.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.BlockEmcStorage;
import cool.furry.mc.projectexpansion.block.BlockTransmutationInterface;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import net.minecraft.Util;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Blocks {
    public static final DeferredRegister.Blocks Registry = DeferredRegister.createBlocks(Main.MOD_ID);

    public static final DeferredBlock<BlockTransmutationInterface> TRANSMUTATION_INTERFACE = register("transmutation_interface", BlockTransmutationInterface::new, BlockTransmutationInterface::getProperties);
    public static final DeferredBlock<BlockCompactSun> COMPACT_SUN = register("compact_sun", BlockCompactSun::new, BlockCompactSun::getProperties);
    public static final DeferredBlock<BlockCondenserMK3> CONDENSER_MK3 = register("condenser_mk3", BlockCondenserMK3::new, BlockCondenserMK3::getProperties);
    public static final DeferredBlock<BlockArcaneTransmutationTable> ARCANE_TRANSMUTATION_TABLE = register("arcane_transmutation_table", BlockArcaneTransmutationTable::new, BlockArcaneTransmutationTable::getProperties);

    public static final DeferredBlock<BlockEMCRefinery> EMC_REFINERY = register("emc_refinery", BlockEMCRefinery::new, BlockEMCRefinery::getProperties);
    public static final DeferredBlock<BlockEmcPipe> EMC_PIPE = register("emc_pipe", BlockEmcPipe::new, BlockEmcPipe::getProperties);
    public static final Map<EmcStorageTier, DeferredBlock<BlockEmcStorage>> EMC_STORAGE = Util.make(new EnumMap<>(EmcStorageTier.class), map -> {
        for (EmcStorageTier tier : EmcStorageTier.VALUES) map.put(tier, register(tier.getSerializedName() + "_emc_storage", properties -> new BlockEmcStorage(properties, tier), BlockEmcStorage::getProperties));
    });

    // ianthium, the material pipes are made from, with the same properties as iron
    public static final DeferredBlock<DropExperienceBlock> IANTHIUM_ORE = register("ianthium_ore", properties -> new DropExperienceBlock(ConstantInt.of(0), properties), () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_ORE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_IANTHIUM_ORE = register("deepslate_ianthium_ore", properties -> new DropExperienceBlock(ConstantInt.of(0), properties), () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE));
    public static final DeferredBlock<Block> RAW_IANTHIUM_BLOCK = register("raw_ianthium_block", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.RAW_IRON_BLOCK).mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> IANTHIUM_BLOCK = register("ianthium_block", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PURPLE));

    public static <T extends Block> DeferredBlock<T> register(String name, Function<BlockBehaviour.Properties, T> createBlock, Supplier<BlockBehaviour.Properties> properties) {
        return Registry.register(name, () -> createBlock.apply(properties.get()));
    }
}
