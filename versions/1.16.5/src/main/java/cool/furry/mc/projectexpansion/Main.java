package cool.furry.mc.projectexpansion;

import cool.furry.mc.projectexpansion.commands.Permissions;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.registries.*;
import cool.furry.mc.projectexpansion.util.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "projectexpansion";
    @SuppressWarnings("unused")
    public static final org.apache.logging.log4j.Logger Logger = LogManager.getLogger();

    public Main() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Attributes.Registry.register(modEventBus);
        BlockEntityTypes.Registry.register(modEventBus);
        Blocks.Registry.register(modEventBus);
        Enchantments.Registry.register(modEventBus);
        Items.Registry.register(modEventBus);
        SoundEvents.Registry.register(modEventBus);
        MenuTypes.Registry.register(modEventBus);
        modEventBus.addGenericListener(Item.class, EventPriority.LOWEST, CreativeTabs::assignItems);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        Config.register(ModLoadingContext.get());

        Fuel.registerAll();
        Matter.registerAll();
        Star.registerAll();
        AdvancedAlchemicalChest.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PacketHandler.register();
            Capabilities.register();
            Permissions.registerPermissionNodes();
        });
    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        AlchemicalCollectionCollector.process();
        for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            for (int i = 0; i < player.inventory.getContainerSize(); i++) {
                ItemStack stack = player.inventory.getItem(i);
                if (stack.getItem().equals(Items.INFINITE_FUEL.get()) && !OwnerData.has(stack)) {
                    new OwnerData(player.getUUID(), player.getName().getString()).set(stack);
                    continue;
                }
                boolean hasEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0;
                if (hasEnch && !stack.getOrCreateTag().contains(TagNames.ACTIVE)) {
                    stack.getOrCreateTag().putBoolean(TagNames.ACTIVE, true);
                }
            }
        }
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
