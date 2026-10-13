package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.Star;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class CreativeTabs {
    public static final ItemGroup MAIN = new ItemGroup(Main.MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Objects.requireNonNull(Items.FINAL_STAR.get()));
        }

        @Override
        public ITextComponent getDisplayName() {
            return Lang.ITEMGROUP.translate();
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            displayItems(item -> items.add(new ItemStack(item)));
        }
    };

    private static void displayItems(Consumer<IItemProvider> output) {
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
        output.accept(Items.ARCANE_TRANSMUTATION_TABLET.get());
        output.accept(Items.ARCANE_TRANSMUTATION_TABLE.get());
        output.accept(Items.IANTHIUM_ORE.get());
        output.accept(Items.RAW_IANTHIUM.get());
        output.accept(Items.RAW_IANTHIUM_BLOCK.get());
        output.accept(Items.IANTHIUM_NUGGET.get());
        output.accept(Items.IANTHIUM_INGOT.get());
        output.accept(Items.IANTHIUM_BLOCK.get());
        output.accept(Items.EMC_REFINERY.get());
        output.accept(Items.EMC_PIPE.get());
        Items.EMC_STORAGE.values().forEach(item -> output.accept(item.get()));
        output.accept(Items.WRENCH.get());
        output.accept(Items.BASIC_PIPE_UPGRADE.get());
        output.accept(Items.IMPROVED_PIPE_UPGRADE.get());
        output.accept(Items.ADVANCED_PIPE_UPGRADE.get());
        output.accept(Items.ULTIMATE_PIPE_UPGRADE.get());
        output.accept(Items.INFINITY_PIPE_UPGRADE.get());
    }

    // items need a tab to show up in search, this sets it without needing to touch every Item.Properties
    // the search tree is built before common setup, so this has to happen as soon as the items are registered
    public static void assignItems(RegistryEvent.Register<Item> event) {
        for (Item item : event.getRegistry().getValues()) {
            ResourceLocation id = item.getRegistryName();
            if (id != null && id.getNamespace().equals(Main.MOD_ID) && item.getItemCategory() == null) {
                ObfuscationReflectionHelper.setPrivateValue(Item.class, item, MAIN, "field_77701_a");
            }
        }
    }
}
