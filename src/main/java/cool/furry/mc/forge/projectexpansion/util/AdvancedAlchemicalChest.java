package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import cool.furry.mc.forge.projectexpansion.registries.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.tile.TileAdvancedAlchemicalChest;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class AdvancedAlchemicalChest {
	private static final Map<DyeColor, RegistryObject<BlockAdvancedAlchemicalChest>> blocks = new HashMap<>();
	private static final Map<DyeColor, RegistryObject<TileEntityType<TileAdvancedAlchemicalChest>>> blockEntityTypes = new HashMap<>();
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static final Map<DyeColor, RegistryObject<BlockItem>> blockItems = new HashMap<>();
	public static void register() {
		for (DyeColor color : DyeColor.values()) {
			blocks.put(color, Blocks.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockAdvancedAlchemicalChest(color)));
			blockItems.put(color, Items.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockItem(getBlock(color), new Item.Properties().tab(Main.tab))));
			//noinspection ConstantConditions
			blockEntityTypes.put(color, TileEntityTypes.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> TileEntityType.Builder.of(() -> new TileAdvancedAlchemicalChest(getBlockEntityType(color), color), getBlock(color)).build(null)));
		}
	}

	public static RegistryObject<BlockAdvancedAlchemicalChest> getRegistryBlock(DyeColor color) {
		return blocks.get(color);
	}

	public static BlockAdvancedAlchemicalChest getBlock(DyeColor color) {
		return getRegistryBlock(color).get();
	}

	public static RegistryObject<TileEntityType<TileAdvancedAlchemicalChest>> getRegistryBlockEntityType(DyeColor color) {
		return blockEntityTypes.get(color);
	}

	public static TileEntityType<TileAdvancedAlchemicalChest> getBlockEntityType(DyeColor color) {
		return getRegistryBlockEntityType(color).get();
	}

	public static BlockAdvancedAlchemicalChest[] getBlocks() {
		return blocks.values().stream().map(RegistryObject::get).toArray(BlockAdvancedAlchemicalChest[]::new);
	}
}
