package cool.furry.mc.projectexpansion.events;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.integrations.curios.CuriosIntegration;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.rendering.ChestRenderer;
import cool.furry.mc.projectexpansion.util.IChestLike;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderingEvent {
    // renderers are bound during client setup before 1.17
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        registerChest(BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get());
        registerChest(BlockEntityTypes.CONDENSER_MK3.get());
    }

    // atlas sources were added in 1.19.3, before that sprites are added here
    @SubscribeEvent
    public static void textureStitch(TextureStitchEvent.Pre event) {
        if (event.getMap().location().equals(PlayerContainer.BLOCK_ATLAS)) {
            event.addSprite(CuriosIntegration.TABLET_SLOT_ICON);
        }
    }

    private static <BE extends TileEntity & IChestLike> void registerChest(TileEntityType<BE> type) {
        ClientRegistry.bindTileEntityRenderer(type, dispatcher -> new ChestRenderer<>(dispatcher, type));
    }
}
