package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.util.IItemProvider;
import net.minecraftforge.fml.RegistryObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AdvancedAlchemicalChest {
	private static final Map<DyeColor, RegistryObject<BlockAdvancedAlchemicalChest>> blocks = new HashMap<>();
	private static final Map<DyeColor, RegistryObject<BlockItem>> blockItems = new HashMap<>();
	public static void register() {
		for(DyeColor color : DyeColor.values()) {
			blocks.put(color, Blocks.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockAdvancedAlchemicalChest(BlockAdvancedAlchemicalChest.getProperties(color), color)));
			blockItems.put(color, Items.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockItem(getBlock(color), new Item.Properties())));
		}
	}

	public static void setAllCreativeTab(Consumer<IItemProvider> output) {
		Arrays.stream(DyeColor.values()).forEach(color -> output.accept(blockItems.get(color).get()));
	}

	public static RegistryObject<BlockAdvancedAlchemicalChest> getRegistryBlock(DyeColor color) {
		return blocks.get(color);
	}

	public static BlockAdvancedAlchemicalChest getBlock(DyeColor color) {
		return getRegistryBlock(color).get();
	}

	public static BlockAdvancedAlchemicalChest[] getBlocks() {
		return blocks.values().stream().map(RegistryObject::get).toArray(BlockAdvancedAlchemicalChest[]::new);
	}
}
