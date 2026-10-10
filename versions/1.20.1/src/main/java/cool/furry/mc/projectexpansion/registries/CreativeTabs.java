package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.util.*;
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
                    .icon(() -> new ItemStack(Objects.requireNonNull(Items.FINAL_STAR.get())))
                    .title(Lang.ITEMGROUP.translate())
                    .displayItems((displayParameters, output) -> {
                        Star.setAllCreativeTab(output::accept);
                        Matter.setAllCreativeTab(output::accept);
                        Fuel.setAllCreativeTab(output::accept);
                        AdvancedAlchemicalChest.setAllCreativeTab(output::accept);

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
                        output.accept(Items.ARCANE_TRANSMUTATION_TABLET.get());
                        output.accept(Items.ARCANE_TRANSMUTATION_TABLE.get());
                        output.accept(Items.IANTHIUM_ORE.get());
                        output.accept(Items.DEEPSLATE_IANTHIUM_ORE.get());
                        output.accept(Items.RAW_IANTHIUM.get());
                        output.accept(Items.RAW_IANTHIUM_BLOCK.get());
                        output.accept(Items.IANTHIUM_NUGGET.get());
                        output.accept(Items.IANTHIUM_INGOT.get());
                        output.accept(Items.IANTHIUM_BLOCK.get());
                        output.accept(Items.EMC_REFINERY.get());
                        output.accept(Items.EMC_PIPE.get());
                        output.accept(Items.WRENCH.get());
                        output.accept(Items.BASIC_PIPE_UPGRADE.get());
                        output.accept(Items.IMPROVED_PIPE_UPGRADE.get());
                        output.accept(Items.ADVANCED_PIPE_UPGRADE.get());
                        output.accept(Items.ULTIMATE_PIPE_UPGRADE.get());
                        output.accept(Items.INFINITY_PIPE_UPGRADE.get());
                    })
                    .build()
    );
}
