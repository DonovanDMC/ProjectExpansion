package cool.furry.mc.forge.projectexpansion;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.Blocks;
import cool.furry.mc.forge.projectexpansion.init.ContainerTypes;
import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Star;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;

@Mod("projectexpansion")
public class Main {
    public static final String MOD_ID = "projectexpansion";
    public static ItemGroup group;

    public static final org.apache.logging.log4j.Logger Logger = LogManager.getLogger();

    public Main() {
        group = new ItemGroup(MOD_ID) {

            @Override
            @Nonnull
            @OnlyIn(Dist.CLIENT)
            public ItemStack createIcon() {
                return new ItemStack(Matter.FADING.getMatter());
            }
        };
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Blocks.Registry.register(bus);
        Items.Registry.register(bus);
        TileEntityTypes.Registry.register(bus);
        ContainerTypes.Registry.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.Spec, "project-expansion.toml");

        Fuel.registerAll();
        Matter.registerAll();
        Star.registerAll();
    }
}
