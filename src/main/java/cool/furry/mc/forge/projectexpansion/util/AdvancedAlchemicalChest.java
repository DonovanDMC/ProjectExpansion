package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.neoforge.projectexpansion.registries.Blocks;
import cool.furry.mc.neoforge.projectexpansion.registries.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AdvancedAlchemicalChest {
	private static final Map<DyeColor, DeferredHolder<Block, BlockAdvancedAlchemicalChest>> blocks = new HashMap<>();
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static final Map<DyeColor, DeferredHolder<Item, BlockItem>> blockItems = new HashMap<>();
	public static void register() {
		for(DyeColor color : DyeColor.values()) {
			blocks.put(color, Blocks.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockAdvancedAlchemicalChest(color)));
			blockItems.put(color, Items.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockItem(getBlock(color), new Item.Properties())));
		}
	}

	public static void setAllCreativeTab(CreativeModeTab.Output output) {
		Arrays.stream(DyeColor.values()).forEach(color -> output.accept(blockItems.get(color).get()));
	}

	public static DeferredHolder<Block, BlockAdvancedAlchemicalChest> getRegistryBlock(DyeColor color) {
		return blocks.get(color);
	}

	public static BlockAdvancedAlchemicalChest getBlock(DyeColor color) {
		return getRegistryBlock(color).get();
	}

	public static BlockAdvancedAlchemicalChest[] getBlocks() {
		return blocks.values().stream().map(DeferredHolder::get).toArray(BlockAdvancedAlchemicalChest[]::new);
	}
}
