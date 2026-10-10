package cool.furry.mc.projectexpansion.registries;

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

    public static <T extends Block> RegistryObject<T> register(String name, Function<AbstractBlock.Properties, T> createBlock, Supplier<AbstractBlock.Properties> properties) {
        return Registry.register(name, () -> createBlock.apply(properties.get()));
    }
}
