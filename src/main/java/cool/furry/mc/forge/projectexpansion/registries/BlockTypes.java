package cool.furry.mc.neoforge.projectexpansion.registries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.*;
import cool.furry.mc.neoforge.projectexpansion.util.Matter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockTypes {
    public static final DeferredRegister<MapCodec<? extends Block>> Registry = DeferredRegister.create(Registries.BLOCK_TYPE, Main.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockAdvancedAlchemicalChest>> ADVANCED_ALCHEMICAL_CHEST = Registry.register("advanced_alchemical_chest", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
            DyeColor.CODEC.fieldOf("color").forGetter(BlockAdvancedAlchemicalChest::getColor)
    ).apply(instance, BlockAdvancedAlchemicalChest::new)));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockCollector>> COLLECTOR = Registry.register("collector", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
            Matter.CODEC.fieldOf("matter").forGetter(BlockCollector::getMatter)
    ).apply(instance, BlockCollector::new)));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockEMCLink>> EMC_LINK = Registry.register("emc_link", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
            Matter.CODEC.fieldOf("matter").forGetter(BlockEMCLink::getMatter)
    ).apply(instance, BlockEMCLink::new)));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockMatter>> MATTER_BLOCK = Registry.register("matter_block", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
            Matter.CODEC.fieldOf("matter").forGetter(BlockMatter::getMatter)
    ).apply(instance, BlockMatter::new)));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockPowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
            Matter.CODEC.fieldOf("matter").forGetter(BlockPowerFlower::getMatter)
    ).apply(instance, BlockPowerFlower::new)));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockRelay>> RELAY = Registry.register("relay", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
            Matter.CODEC.fieldOf("matter").forGetter(BlockRelay::getMatter)
    ).apply(instance, BlockRelay::new)));
}
