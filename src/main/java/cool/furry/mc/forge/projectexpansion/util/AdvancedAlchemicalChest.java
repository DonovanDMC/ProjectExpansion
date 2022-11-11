package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class AdvancedAlchemicalChest {
	private static final Map<DyeColor, RegistryObject<BlockAdvancedAlchemicalChest>> blocks = new HashMap<>();
	private static final Map<DyeColor, RegistryObject<BlockEntityType<BlockEntityAdvancedAlchemicalChest>>> blockEntityTypes = new HashMap<>();
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static final Map<DyeColor, RegistryObject<BlockItem>> blockItems = new HashMap<>();
	public static void register() {
		for(DyeColor color : DyeColor.values()) {
			blocks.put(color, Blocks.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockAdvancedAlchemicalChest(color)));
			//noinspection ConstantConditions
			blockEntityTypes.put(color, BlockEntityTypes.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> BlockEntityType.Builder.of((pos, state) -> new BlockEntityAdvancedAlchemicalChest(pos, state, getBlockEntityType(color), color), getBlock(color)).build(null)));
			blockItems.put(color, Items.Registry.register(String.format("%s_advanced_alchemical_chest", color.getName()), () -> new BlockItem(getBlock(color), new Item.Properties().tab(Main.tab))));
		}
	}

	public static RegistryObject<BlockAdvancedAlchemicalChest> getRegistryBlock(DyeColor color) {
		return blocks.get(color);
	}

	public static BlockAdvancedAlchemicalChest getBlock(DyeColor color) {
		return getRegistryBlock(color).get();
	}

	public static RegistryObject<BlockEntityType<BlockEntityAdvancedAlchemicalChest>> getRegistryBlockEntityType(DyeColor color) {
		return blockEntityTypes.get(color);
	}

	public static BlockEntityType<BlockEntityAdvancedAlchemicalChest> getBlockEntityType(DyeColor color) {
		return getRegistryBlockEntityType(color).get();
	}

	public static BlockAdvancedAlchemicalChest[] getBlocks() {
		return blocks.values().stream().map(RegistryObject::get).toArray(BlockAdvancedAlchemicalChest[]::new);
	}
}
