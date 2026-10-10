package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// 1.18 has no biome modifiers, so the ore is registered and added to biomes here instead of through data
public class Features {
    @Nullable
    private static Holder<PlacedFeature> ORE_IANTHIUM;

    // the builtin registries take these during common setup
    public static void register() {
        Holder<ConfiguredFeature<OreConfiguration, ?>> ore = FeatureUtils.register(Main.MOD_ID + ":ore_ianthium", Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(OreFeatures.STONE_ORE_REPLACEABLES, Blocks.IANTHIUM_ORE.get().defaultBlockState()),
                OreConfiguration.target(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, Blocks.DEEPSLATE_IANTHIUM_ORE.get().defaultBlockState())
        ), 6));
        ORE_IANTHIUM = PlacementUtils.register(Main.MOD_ID + ":ore_ianthium", ore, List.of(
                CountPlacement.of(10),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(48)),
                BiomeFilter.biome()
        ));
    }

    public static void addToBiomes(BiomeLoadingEvent event) {
        Biome.BiomeCategory category = event.getCategory();
        if (ORE_IANTHIUM == null || category == Biome.BiomeCategory.NETHER || category == Biome.BiomeCategory.THEEND || category == Biome.BiomeCategory.NONE) return;
        event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_IANTHIUM);
    }
}
