package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.neoforge.projectexpansion.block.BlockTransmutationInterface;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Blocks {
    public static final DeferredRegister.Blocks Registry = DeferredRegister.createBlocks(Main.MOD_ID);

    public static final DeferredBlock<BlockTransmutationInterface> TRANSMUTATION_INTERFACE = register("transmutation_interface", BlockTransmutationInterface::new, BlockTransmutationInterface::getProperties);
    public static final DeferredBlock<BlockCompactSun> COMPACT_SUN = register("compact_sun", BlockCompactSun::new, BlockCompactSun::getProperties);
    public static final DeferredBlock<BlockCondenserMK3> CONDENSER_MK3 = register("condenser_mk3", BlockCondenserMK3::new, BlockCondenserMK3::getProperties);

    public static <T extends Block> DeferredBlock<T> register(String name, Function<BlockBehaviour.Properties, T> createBlock, Supplier<BlockBehaviour.Properties> properties) {
        return Registry.register(name, () -> createBlock.apply(properties.get()));
    }
}
