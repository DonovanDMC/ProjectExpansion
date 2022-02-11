package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.tile.*;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TileEntityTypes {
    public static final DeferredRegister<TileEntityType<?>> Registry = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Main.MOD_ID);

    public static final RegistryObject<TileEntityType<TilePersonalLink>> PERSONAL_LINK = Registry.register("personal_link", () -> TileEntityType.Builder.create(TilePersonalLink::new, Blocks.PERSONAL_LINK.get()).build(null));
    public static final RegistryObject<TileEntityType<TilePowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> TileEntityType.Builder.create(TilePowerFlower::new, Arrays.stream(Matter.VALUES).map(Matter::getPowerFlower).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileCollector>> ENERGY_COLLECTOR = Registry.register("collector", () -> TileEntityType.Builder.create(TileCollector::new, Arrays.stream(Matter.VALUES).map(Matter::getCollector).toArray(Block[]::new)).build(null));
    public static final RegistryObject<TileEntityType<TileRelay>> ANTI_MATTER_RELAY = Registry.register("relay", () -> TileEntityType.Builder.create(TileRelay::new, Arrays.stream(Matter.VALUES).map(Matter::getRelay).toArray(Block[]::new)).build(null));
}
