package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

@SuppressWarnings("unused")
public class CreativeTabs {
    public static final DeferredRegister<CreativeModeTab> Registry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = Registry.register(Main.MOD_ID, () ->
        CreativeModeTab.builder()
            .icon(() -> new ItemStack(Objects.requireNonNull(Matter.FADING.getMatter())))
            .title(Lang.CREATIVE_TAB.translate())
            .displayItems((displayParameters, output) -> {
                Matter.setAllCreativeTab(output);
                Fuel.setAllCreativeTab(output);
                AdvancedAlchemicalChest.setAllCreativeTab(output);

                output.accept(Items.FINAL_STAR_SHARD.get());
                output.accept(Items.FINAL_STAR.get());
                output.accept(Items.MATTER_UPGRADER.get());
                output.accept(Items.INFINITE_FUEL.get());
                output.accept(Items.INFINITE_STEAK.get());
                output.accept(Items.TRANSMUTATION_INTERFACE.get());
                output.accept(Items.KNOWLEDGE_SHARING_BOOK.get());
                output.accept(Items.BASIC_ALCHEMICAL_BOOK.get());
                output.accept(Items.ADVANCED_ALCHEMICAL_BOOK.get());
                output.accept(Items.MASTER_ALCHEMICAL_BOOK.get());
                output.accept(Items.ARCANE_ALCHEMICAL_BOOK.get());
            })
            .build()
    );
}
