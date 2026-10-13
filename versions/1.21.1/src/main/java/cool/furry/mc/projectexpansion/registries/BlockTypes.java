package cool.furry.mc.projectexpansion.registries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.*;
import cool.furry.mc.projectexpansion.util.IHasColor;
import cool.furry.mc.projectexpansion.util.IHasMatter;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;

public class BlockTypes {
    public static final DeferredRegister<MapCodec<? extends Block>> Registry = DeferredRegister.create(Registries.BLOCK_TYPE, Main.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockAdvancedAlchemicalChest>> ADVANCED_ALCHEMICAL_CHEST = registerColor("advanced_alchemical_chest", BlockAdvancedAlchemicalChest::new);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockCollector>> COLLECTOR = registerMatter("collector", BlockCollector::new);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockEMCLink>> EMC_LINK = registerMatter("emc_link", BlockEMCLink::new);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockPowerFlower>> POWER_FLOWER = registerMatter("power_flower", BlockPowerFlower::new);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockRelay>> RELAY = registerMatter("relay", BlockRelay::new);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockMatter>> MATTER_BLOCK = registerMatter("matter_block", BlockMatter::new);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockCondenserMK3>> CONDENSER_MK3 = Registry.register("condenser_mk3", () -> BlockBehaviour.simpleCodec(BlockCondenserMK3::new));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockArcaneTransmutationTable>> ARCANE_TRANSMUTATION_TABLE = Registry.register("arcane_transmutation_table", () -> BlockBehaviour.simpleCodec(BlockArcaneTransmutationTable::new));

    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockEMCRefinery>> EMC_REFINERY = Registry.register("emc_refinery", () -> BlockBehaviour.simpleCodec(BlockEMCRefinery::new));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockEmcPipe>> EMC_PIPE = Registry.register("emc_pipe", () -> BlockBehaviour.simpleCodec(BlockEmcPipe::new));
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockEmcStorage>> EMC_STORAGE = Registry.register("emc_storage", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(BlockBehaviour.propertiesCodec(), StringRepresentable.fromEnum(EmcStorageTier::values).fieldOf("tier").forGetter(block -> block.tier)).apply(instance, BlockEmcStorage::new)));

    public static <T extends Block & IHasMatter> DeferredHolder<MapCodec<? extends Block>, MapCodec<T>> registerMatter(String name, BiFunction<BlockBehaviour.Properties, Matter, T> block) {
        return Registry.register(name, () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlockBehaviour.propertiesCodec(),
                Matter.CODEC.fieldOf("matter").forGetter(IHasMatter::getMatter)
        ).apply(instance, block)));
    }

    public static <T extends Block & IHasColor> DeferredHolder<MapCodec<? extends Block>, MapCodec<T>> registerColor(String name, BiFunction<BlockBehaviour.Properties, DyeColor, T> block) {
        return Registry.register(name, () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlockBehaviour.propertiesCodec(),
                DyeColor.CODEC.fieldOf("color").forGetter(IHasColor::getColor)
        ).apply(instance, block)));
    }
}
