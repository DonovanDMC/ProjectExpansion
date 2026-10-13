package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.BlockEmcStorage;
import cool.furry.mc.projectexpansion.block.BlockIanthiumOre;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.util.Util;
import net.minecraftforge.common.ToolType;
import java.util.EnumMap;
import java.util.Map;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockArcaneTransmutationTable;
import cool.furry.mc.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.projectexpansion.block.BlockTransmutationInterface;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

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
    public static final Map<EmcStorageTier, RegistryObject<BlockEmcStorage>> EMC_STORAGE = Util.make(new EnumMap<>(EmcStorageTier.class), map -> {
        for (EmcStorageTier tier : EmcStorageTier.VALUES) map.put(tier, register(tier.getSerializedName() + "_emc_storage", properties -> new BlockEmcStorage(properties, tier), BlockEmcStorage::getProperties));
    });
    // the ore drops no experience, deepslate and raw ore blocks are from later versions so there's only the one ore
    public static final RegistryObject<BlockIanthiumOre> IANTHIUM_ORE = register("ianthium_ore", BlockIanthiumOre::new, () -> AbstractBlock.Properties.copy(net.minecraft.block.Blocks.IRON_ORE));
    public static final RegistryObject<Block> RAW_IANTHIUM_BLOCK = register("raw_ianthium_block", Block::new, () -> AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(5, 6).harvestTool(ToolType.PICKAXE).harvestLevel(1));
    public static final RegistryObject<Block> IANTHIUM_BLOCK = register("ianthium_block", Block::new, () -> AbstractBlock.Properties.of(Material.METAL, MaterialColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(5, 6).sound(SoundType.METAL).harvestTool(ToolType.PICKAXE).harvestLevel(1));

    public static <T extends Block> RegistryObject<T> register(String name, Function<AbstractBlock.Properties, T> createBlock, Supplier<AbstractBlock.Properties> properties) {
        return Registry.register(name, () -> createBlock.apply(properties.get()));
    }
}
