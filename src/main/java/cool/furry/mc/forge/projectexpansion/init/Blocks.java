package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.block.BlockArcaneTable;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockPersonalLink;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Blocks {
    public static final DeferredRegister<Block> Registry = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<BlockArcaneTable> ARCANE_TABLE = Registry.register("arcane_table", BlockArcaneTable::new);
    public static final RegistryObject<BlockPersonalLink> PERSONAL_LINK = Registry.register("personal_link", BlockPersonalLink::new);
}
