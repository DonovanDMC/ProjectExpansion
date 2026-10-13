package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import org.jetbrains.annotations.Nullable;

// worldgen isn't data driven for mods before 1.19, so the ore is registered and added to biomes here
public class Features {
    @Nullable
    private static ConfiguredFeature<?, ?> ORE_IANTHIUM;

    // the builtin registries take these during common setup
    // the world starts at 0 here, so the ore is found from bedrock up to 48
    public static void register() {
        ORE_IANTHIUM = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, Main.rl("ore_ianthium"),
                Feature.ORE.configured(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE, Blocks.IANTHIUM_ORE.get().defaultBlockState(), 6)).range(48).squared().count(10));
    }

    public static void addToBiomes(BiomeLoadingEvent event) {
        Biome.Category category = event.getCategory();
        if (ORE_IANTHIUM == null || category == Biome.Category.NETHER || category == Biome.Category.THEEND || category == Biome.Category.NONE) return;
        event.getGeneration().addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, ORE_IANTHIUM);
    }
}
