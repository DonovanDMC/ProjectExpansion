package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Objects;

@SuppressWarnings("unused")
public class CreativeTabs {
    public static final DeferredRegister<CreativeModeTab> Registry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = Registry.register(Main.MOD_ID, () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Objects.requireNonNull(Items.FINAL_STAR.get())))
                    .title(Lang.ITEMGROUP.translate())
                    .displayItems((displayParameters, output) -> {
                        Star.setAllCreativeTab(output);
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
                        output.accept(Items.COMPACT_SUN.get());
                        output.accept(Items.CONDENSER_MK3.get());
                    })
                    .build()
    );
}
