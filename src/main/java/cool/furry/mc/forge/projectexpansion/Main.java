package cool.furry.mc.forge.projectexpansion;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.config.ConfigMenu;
import cool.furry.mc.forge.projectexpansion.init.Blocks;
import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Star;
import moze_intel.projecte.utils.Constants;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Collectors;

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

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.Spec, "project-expansion.toml");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigMenu(screen)));

        replaceEMCFormatter();
        Fuel.registerAll();
        Matter.registerAll();
        Star.registerAll();
    }

    void replaceEMCFormatter() {
        try {
            Field field = Constants.class.getDeclaredField("EMC_FORMATTER");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, EMCFormat.INSTANCE);
        } catch (NoSuchFieldException | IllegalAccessException ignore) {}
    }
}
