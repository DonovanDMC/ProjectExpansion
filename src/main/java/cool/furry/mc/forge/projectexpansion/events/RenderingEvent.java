package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.config.ConfigMenu;
import cool.furry.mc.forge.projectexpansion.rendering.ChestRenderer;
import cool.furry.mc.forge.projectexpansion.tile.TileAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.AdvancedAlchemicalChest;
import net.minecraft.item.DyeColor;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderingEvent {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigMenu(screen));
        for(DyeColor color: DyeColor.values()) {
            RegistryObject<BlockAdvancedAlchemicalChest> block = AdvancedAlchemicalChest.getRegistryBlock(color);
            TileEntityType<TileAdvancedAlchemicalChest> blockEntityType = AdvancedAlchemicalChest.getBlockEntityType(color);
            ClientRegistry.bindTileEntityRenderer(blockEntityType, context -> new ChestRenderer(context, new ResourceLocation(Main.MOD_ID, String.format("textures/block/advanced_alchemical_chest/%s.png", color.getName())), (b) -> b == block.get()));
        }
    }
}
