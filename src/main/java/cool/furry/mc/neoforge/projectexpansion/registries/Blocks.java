package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.neoforge.projectexpansion.block.BlockTransmutationInterface;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class Blocks {
    public static final DeferredRegister.Blocks Registry = DeferredRegister.createBlocks(Main.MOD_ID);

    public static final DeferredBlock<BlockTransmutationInterface> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", BlockTransmutationInterface::new);
    public static final DeferredBlock<BlockCompactSun> COMPACT_SUN = Registry.register("compact_sun", BlockCompactSun::new);
    public static final DeferredBlock<BlockCondenserMK3> CONDENSER_MK3 = Registry.register("condenser_mk3", BlockCondenserMK3::new);
}
