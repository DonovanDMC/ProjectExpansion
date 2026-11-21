package cool.furry.mc.projectexpansion.wiki;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.Command;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Input;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Output;
import cool.furry.mc.projectexpansion.gui.GUIEMCRefinery;
import cool.furry.mc.projectexpansion.gui.GUIEmcPipe;
import cool.furry.mc.projectexpansion.gui.GUIPowerFlower;
import cool.furry.mc.projectexpansion.item.ItemArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Enchantments;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.EmcLock;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.Star;
import cool.furry.mc.projectexpansion.util.pipe.Distribution;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import java.util.Optional;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

// saves the wiki's recipe, item, tooltip & menu images, so they all look the same and can be redone when something changes
// the wikiImages run does it all and quits, /pexwiki does it in any dev world, both save to images/<Page>/<name>.png
@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public final class WikiImages {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProjectExpansion Wiki Images");
    // set by the wikiImages run, to where the images go
    private static final String PROPERTY = "projectexpansion.wikiImages";
    private static final String WORLD = "wiki-images";
    private static final Deque<Task> TASKS = new ArrayDeque<>();
    private static boolean started;
    private static int saved, failed;

    private WikiImages() {}

    // a step that waits until it's ready, runs, then the next one starts
    private record Task(String name, BooleanSupplier ready, Runnable run) {}

    /*********
     * Start *
     *********/

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pexwiki").executes(ctx -> {
            queueAll(FMLPaths.GAMEDIR.get().resolve("wiki-images"), false);
            return Command.SINGLE_SUCCESS;
        }));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        String dir = System.getProperty(PROPERTY);
        if (dir != null && !started && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            started = true;
            openWorld(mc);
            // the world needs a moment for chunks, recipes & emc values to arrive
            TASKS.add(new Task("join", () -> {
                // flying straight away, there's nothing to stand on
                if (mc.player != null && !mc.player.getAbilities().flying) {
                    mc.player.getAbilities().flying = true;
                    mc.player.onUpdateAbilities();
                }
                return mc.player != null && mc.screen == null;
            }, () -> {}));
            // up off the bottom of the world, so blocks can be placed around the player
            TASKS.add(new Task("lift", () -> true, () -> {
                MinecraftServer server = Objects.requireNonNull(mc.getSingleplayerServer());
                server.execute(() -> Objects.requireNonNull(server.getPlayerList().getPlayer(Objects.requireNonNull(mc.player).getUUID())).connection.teleport(0.5, 100, 0.5, 0, 0));
            }));
            waitTicks(100);
            TASKS.add(new Task("queue", () -> true, () -> queueAll(Path.of(dir), true)));
        }
        Task task = TASKS.peek();
        if (task == null || !task.ready().getAsBoolean()) return;
        TASKS.poll();
        try {
            task.run().run();
        } catch (Exception e) {
            failed++;
            LOGGER.error("{} failed", task.name(), e);
        }
    }

    private static void openWorld(Minecraft mc) {
        // a fresh one every run, so nothing from an earlier run shows up
        if (mc.getLevelSource().levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().createAccess(WORLD)) {
                access.deleteLevel();
            } catch (Exception e) {
                throw new IllegalStateException("Couldn't delete the old " + WORLD + " world", e);
            }
        }
        // an empty sky in creative, scenes are always at noon
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0, false, false),
                access -> {
                    HolderLookup.RegistryLookup<Biome> biomes = access.lookupOrThrow(Registries.BIOME);
                    FlatLevelGeneratorSettings flat = FlatLevelGeneratorSettings.getDefault(biomes, access.lookupOrThrow(Registries.STRUCTURE_SET), access.lookupOrThrow(Registries.PLACED_FEATURE))
                            // no layers or villages, nothing at all
                            .withBiomeAndLayers(List.of(), Optional.of(HolderSet.direct()), biomes.getOrThrow(Biomes.PLAINS));
                    return access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()
                            .replaceOverworldGenerator(access, new FlatLevelSource(flat));
                }, mc.screen);
    }

    private static void waitTicks(int ticks) {
        int[] left = { ticks };
        TASKS.add(new Task("wait", () -> left[0]-- <= 0, () -> {}));
    }

    /**********
     * Images *
     **********/

    private static void queueAll(Path root, boolean quit) {
        saved = failed = 0;
        // recipes
        recipe(root, "Advanced-Alchemical-Chest", "recipe", "advanced_alchemical_chest/white");
        recipe(root, "Advanced-Alchemical-Chest", "recolor-with-dye", "advanced_alchemical_chest/recolor/light_blue");
        recipe(root, "Alchemical-Book", "basic-recipe", "basic_alchemical_book");
        recipe(root, "Alchemical-Book", "advanced-recipe", "advanced_alchemical_book");
        recipe(root, "Alchemical-Book", "master-recipe", "master_alchemical_book");
        recipe(root, "Alchemical-Book", "arcane-recipe", "arcane_alchemical_book");
        recipe(root, "Arcane-Transmutation-Tablet", "tablet-recipe", "arcane_transmutation_tablet");
        recipe(root, "Arcane-Transmutation-Tablet", "table-recipe", "arcane_transmutation_table");
        recipe(root, "Collectors", "from-projecte-mk1", "collector/mk1_conversion");
        recipe(root, "Collectors", "from-projecte-mk2", "collector/mk2_conversion");
        recipe(root, "Collectors", "from-projecte-mk3", "collector/mk3_conversion");
        recipe(root, "Collectors", "basic-recipe", "collector/basic");
        recipe(root, "Collectors", "upgrade-recipe", "collector/dark");
        recipe(root, "Collectors", "final-recipe", "collector/final");
        recipe(root, "Collectors", "compressed", "collector/compressed/basic");
        recipe(root, "Compact-Sun", "recipe", "compact_sun_1", "compact_sun_2");
        recipe(root, "Condenser-MK3", "recipe", "condenser_mk3");
        recipe(root, "EMC-Link", "basic-recipe", "emc_link/basic");
        recipe(root, "EMC-Link", "upgrade-recipe", "emc_link/dark");
        recipe(root, "EMC-Link", "final-recipe", "emc_link/final");
        recipe(root, "EMC-Pipes", "pipe-recipe", "emc_pipe");
        recipe(root, "EMC-Pipes", "upgrade-recipe", "pipe_upgrade/basic");
        recipe(root, "EMC-Pipes", "wrench-recipe", "wrench");
        recipe(root, "EMC-Refinery", "recipe", "emc_refinery");
        recipe(root, "EMC-Storage", "basic-recipe", "emc_storage/basic");
        recipe(root, "EMC-Storage", "upgrade-recipe", "emc_storage/improved");
        recipe(root, "Ianthium", "refining-recipes", "ianthium/ianthium_ingot_from_refining_ore", "ianthium/ianthium_ingot_from_refining_raw_ianthium");
        recipe(root, "Ianthium", "block-recipe", "ianthium/ianthium_block");
        recipe(root, "Ianthium", "ingots-from-block", "ianthium/ianthium_ingot_from_block");
        recipe(root, "Ianthium", "nuggets-recipe", "ianthium/ianthium_nugget");
        recipe(root, "Ianthium", "ingot-from-nuggets", "ianthium/ianthium_ingot_from_nuggets");
        recipe(root, "Knowledge-Sharing-Book", "recipe", "knowledge_sharing_book");
        recipe(root, "Matter-and-Fuel", "magenta-matter-recipes", "matter/magenta", "matter/magenta_alt");
        recipe(root, "Matter-and-Fuel", "pink-matter-recipes", "matter/pink", "matter/pink_alt");
        recipe(root, "Matter-and-Fuel", "matter-block-recipe", "matter/block/magenta");
        recipe(root, "Matter-and-Fuel", "magenta-fuel-recipe", "fuel/item/magenta");
        recipe(root, "Matter-and-Fuel", "pink-fuel-recipe", "fuel/item/pink");
        recipe(root, "Matter-and-Fuel", "fuel-block-recipe", "fuel/block/magenta");
        recipe(root, "Power-Flowers", "recipe", "power_flower/basic");
        recipe(root, "Relays", "from-projecte-mk1", "relay/mk1_conversion");
        recipe(root, "Relays", "from-projecte-mk2", "relay/mk2_conversion");
        recipe(root, "Relays", "from-projecte-mk3", "relay/mk3_conversion");
        recipe(root, "Relays", "basic-recipe", "relay/basic");
        recipe(root, "Relays", "upgrade-recipe", "relay/dark");
        recipe(root, "Relays", "final-recipe", "relay/final");
        recipe(root, "Stars", "magnum-star-recipe", "star/magnum/ein");
        recipe(root, "Stars", "colossal-star-recipe", "star/colossal/ein");
        recipe(root, "Stars", "final-star-recipe", "star/final_star");
        recipe(root, "Stars", "final-star-shard-recipe", "star/final_star_shard");
        recipe(root, "Transmutation-Interface", "recipe", "transmutation_interface");

        // tooltips
        tooltip(root, "Alchemical-Book", "basic-tooltip", () -> new ItemStack(Items.BASIC_ALCHEMICAL_BOOK.get()));
        tooltip(root, "Alchemical-Book", "advanced-tooltip", () -> new ItemStack(Items.ADVANCED_ALCHEMICAL_BOOK.get()));
        tooltip(root, "Alchemical-Book", "master-tooltip", () -> new ItemStack(Items.MASTER_ALCHEMICAL_BOOK.get()));
        tooltip(root, "Alchemical-Book", "arcane-tooltip", () -> new ItemStack(Items.ARCANE_ALCHEMICAL_BOOK.get()));
        tooltip(root, "Alchemical-Collection", "tooltip-enabled", () -> collectionPickaxe(true));
        tooltip(root, "Alchemical-Collection", "tooltip-disabled", () -> collectionPickaxe(false));
        tooltip(root, "Compact-Sun", "tooltip", () -> new ItemStack(Items.COMPACT_SUN.get()));
        tooltip(root, "EMC-Lock", "locked-tooltip", () -> {
            ItemStack stack = new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG);
            EmcLock.toggle(stack);
            return stack;
        });
        tooltip(root, "Stars", "final-star-tooltip", () -> new ItemStack(Items.FINAL_STAR.get()));
        tooltip(root, "Stars", "final-star-shard-tooltip", () -> new ItemStack(Items.FINAL_STAR_SHARD.get()));

        // items, drawn bigger than in an inventory
        items(root, "Ianthium", "items", 3, () -> List.of(
                new ItemStack(Blocks.IANTHIUM_ORE.get()), new ItemStack(Blocks.DEEPSLATE_IANTHIUM_ORE.get()), new ItemStack(Items.RAW_IANTHIUM.get()),
                new ItemStack(Blocks.RAW_IANTHIUM_BLOCK.get()), new ItemStack(Items.IANTHIUM_INGOT.get()), new ItemStack(Items.IANTHIUM_NUGGET.get()),
                new ItemStack(Blocks.IANTHIUM_BLOCK.get())));

        // every star, a row for each size
        grid(root, "Stars", "all-stars", () -> List.of(
                Arrays.stream(Star.VALUES).map(star -> new ItemStack(Objects.requireNonNull(star.asMagnumItem()))).toList(),
                Arrays.stream(Star.VALUES).map(star -> new ItemStack(Objects.requireNonNull(star.asColossalItem()))).toList(),
                Arrays.stream(Star.VALUES).map(star -> new ItemStack(Objects.requireNonNull(star.asGargantuanItem()))).toList()));

        // menus, each set up in its own spot
        menu(root, "EMC-Pipes", "settings-menu", 0, GUIEmcPipe.class, 5, (player, pos) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pipePos = pos.east();
            clear(level, pos, pipePos);
            // the pipe first, so it connects to the collector when that's placed
            level.setBlockAndUpdate(pipePos, Blocks.EMC_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(pos, Objects.requireNonNull(Matter.BASIC.getCollector()).defaultBlockState());
            BlockEmcPipe.useWrench(level, pipePos, level.getBlockState(pipePos), Direction.WEST, false);
            BlockEntityEmcPipe pipe = (BlockEntityEmcPipe) Objects.requireNonNull(level.getBlockEntity(pipePos));
            pipe.setUpgradeStack(Direction.WEST, new ItemStack(Items.IMPROVED_PIPE_UPGRADE.get()));
            pipe.setStoredSettings(Direction.WEST, pipe.getStoredSettings(Direction.WEST).withDistribution(Distribution.ROUND_ROBIN));
            return () -> use(player, pipePos, Direction.WEST);
        });
        menu(root, "Power-Flowers", "menu", 1, GUIPowerFlower.class, 5, (player, pos) -> {
            ServerLevel level = player.serverLevel();
            clear(level, pos);
            level.setBlockAndUpdate(pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()).defaultBlockState());
            ((BlockEntityPowerFlower) Objects.requireNonNull(level.getBlockEntity(pos))).setOwner(player);
            return () -> use(player, pos, Direction.UP);
        });
        // halfway through refining raw ianthium
        menu(root, "EMC-Refinery", "menu", 2, GUIEMCRefinery.class, 50, (player, pos) -> {
            ServerLevel level = player.serverLevel();
            clear(level, pos);
            level.setBlockAndUpdate(pos, Blocks.EMC_REFINERY.get().defaultBlockState());
            BlockEntityEMCRefinery refinery = (BlockEntityEMCRefinery) Objects.requireNonNull(level.getBlockEntity(pos));
            refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get(), 16));
            ItemStack star = new ItemStack(PEItems.KLEIN_STAR_EIN.get());
            IItemEmcHolder holder = Objects.requireNonNull(star.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY));
            holder.insertEmc(star, holder.getMaximumEmc(star), IEmcStorage.EmcAction.EXECUTE);
            refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.STAR_SLOT, star);
            return () -> use(player, pos, Direction.NORTH);
        });
        // the top side locked to iron, turning diamonds into it
        menu(root, "Condenser-MK3", "side-menu", 3, GUICondenserMK3Input.class, 30, (player, pos) -> {
            BlockEntityCondenserMK3 condenser = condenser(player, pos);
            return () -> condenser.openMenu(player, Direction.UP);
        });
        menu(root, "Condenser-MK3", "output-menu", 3, GUICondenserMK3Output.class, 5, (player, pos) -> {
            BlockEntityCondenserMK3 condenser = (BlockEntityCondenserMK3) Objects.requireNonNull(player.serverLevel().getBlockEntity(pos));
            return () -> condenser.openMenu(player, Direction.DOWN);
        });
        menu(root, "Arcane-Transmutation-Tablet", "menu", 4, GUIArcaneTransmutationTablet.class, 5, (player, pos) -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ARCANE_TRANSMUTATION_TABLET.get()));
            return () -> ((ItemArcaneTransmutationTablet) Items.ARCANE_TRANSMUTATION_TABLET.get()).openContainer(player);
        });

        // blocks placed in the world
        scenesStart();
        scene(root, "Arcane-Transmutation-Tablet", "table-placed", (level, pos) -> place(level, pos, Blocks.ARCANE_TRANSMUTATION_TABLE.get().defaultBlockState()));
        scene(root, "Condenser-MK3", "block", (level, pos) -> place(level, pos, Blocks.CONDENSER_MK3.get().defaultBlockState()));
        // lit up while refining, with a collector beside it giving it emc
        scene(root, "EMC-Refinery", "block", (level, pos) -> {
            place(level, pos, Blocks.EMC_REFINERY.get().defaultBlockState().setValue(BlockEMCRefinery.LIT, true));
            BlockEntityEMCRefinery refinery = (BlockEntityEMCRefinery) Objects.requireNonNull(level.getBlockEntity(pos));
            refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get(), 64));
            place(level, pos.south(), Objects.requireNonNull(Matter.FINAL.getCollector()).defaultBlockState());
        });
        scene(root, "EMC-Storage", "all-tiers", (level, pos) -> rows(level, pos, Arrays.stream(EmcStorageTier.VALUES).map(tier -> (Block) Blocks.EMC_STORAGE.get(tier).get()).toList(), 5));
        scene(root, "Collectors", "all-tiers", (level, pos) -> rows(level, pos, Arrays.stream(Matter.VALUES).map(Matter::getCollector).filter(Objects::nonNull).map(Block.class::cast).toList(), 4));
        scene(root, "Relays", "all-tiers", (level, pos) -> rows(level, pos, Arrays.stream(Matter.VALUES).map(Matter::getRelay).filter(Objects::nonNull).map(Block.class::cast).toList(), 4));
        scene(root, "Power-Flowers", "all-tiers", (level, pos) -> rows(level, pos, Arrays.stream(Matter.VALUES).map(Matter::getPowerFlower).filter(Objects::nonNull).map(Block.class::cast).toList(), 4));
        scene(root, "EMC-Link", "all-tiers", (level, pos) -> rows(level, pos, Arrays.stream(Matter.VALUES).map(Matter::getEMCLink).filter(Objects::nonNull).map(Block.class::cast).toList(), 4));
        // matter blocks are only in the registry, there's nothing on Matter to get them from
        scene(root, "Matter-and-Fuel", "matter-blocks", (level, pos) -> rows(level, pos, Arrays.stream(Matter.VALUES)
                .map(matter -> BuiltInRegistries.BLOCK.getOptional(Main.rl(matter.name().toLowerCase(Locale.ROOT) + "_matter_block")).orElse(null)).filter(Objects::nonNull).toList(), 4));
        scene(root, "Matter-and-Fuel", "fuel-blocks", (level, pos) -> rows(level, pos, Arrays.stream(Fuel.VALUES).map(Fuel::getBlock).filter(Objects::nonNull).toList(), 4));
        // a collector sending emc down a line of pipes to a condenser
        scene(root, "EMC-Pipes", "pipe-network", (level, pos) -> {
            for (int i = 1; i <= 4; i++) place(level, pos.south(i), Blocks.EMC_PIPE.get().defaultBlockState());
            place(level, pos, Objects.requireNonNull(Matter.BASIC.getCollector()).defaultBlockState());
            place(level, pos.south(5), Blocks.CONDENSER_MK3.get().defaultBlockState());
            BlockEmcPipe.useWrench(level, pos.south(), level.getBlockState(pos.south()), Direction.NORTH, false);
        });
        scenesEnd();

        TASKS.add(new Task("done", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            LOGGER.info("Saved {} wiki images to {}, {} failed", saved, root.toAbsolutePath(), failed);
            if (mc.player != null) mc.player.displayClientMessage(Component.literal(String.format("Saved %d wiki images to %s, %d failed", saved, root.toAbsolutePath(), failed)), false);
            if (quit) mc.stop();
        }));
    }

    private static Path file(Path root, String page, String name) {
        return root.resolve(page).resolve(name + ".png");
    }

    private static void done(Path file) {
        saved++;
        LOGGER.info("Saved {}", file);
    }

    private static ItemStack collectionPickaxe(boolean active) {
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        stack.enchant(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess().holderOrThrow(Enchantments.ALCHEMICAL_COLLECTION), 1);
        stack.set(PEDataComponentTypes.ACTIVE.get(), active);
        return stack;
    }

    /***********
     * Recipes *
     ***********/

    private static final int PAD = 4, GAP = 6, SLOT = 18, OUTPUT = 26;
    private static final int CRAFTING_WIDTH = PAD + SLOT * 3 + GAP + WikiRenderer.ARROW_WIDTH + GAP + OUTPUT + PAD, CRAFTING_HEIGHT = PAD + SLOT * 3 + PAD;
    private static final int REFINING_WIDTH = PAD + SLOT + GAP + WikiRenderer.ARROW_WIDTH + GAP + OUTPUT + PAD, REFINING_HEIGHT = PAD + OUTPUT + 2 + 9 + 3;

    // more than one recipe are stacked, like the different ways to make magenta matter
    private static void recipe(Path root, String page, String name, String... ids) {
        TASKS.add(new Task(page + "/" + name, () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            List<Recipe<?>> recipes = new ArrayList<>();
            for (String id : ids) {
                RecipeHolder<?> holder = Objects.requireNonNull(mc.level).getRecipeManager().byKey(id.contains(":") ? ResourceLocation.parse(id) : Main.rl(id))
                        .orElseThrow(() -> new IllegalStateException("No recipe " + id));
                recipes.add(holder.value());
            }
            Path file = file(root, page, name);
            try {
                WikiRenderer.save(file, 256, 256, false, graphics -> {
                    int y = 2;
                    for (Recipe<?> recipe : recipes) {
                        if (recipe instanceof RefiningRecipe refining) {
                            refining(graphics, 2, y, refining);
                            y += REFINING_HEIGHT + 4;
                        } else if (recipe instanceof CraftingRecipe crafting) {
                            crafting(graphics, 2, y, crafting);
                            y += CRAFTING_HEIGHT + 4;
                        } else {
                            throw new IllegalStateException("Can't draw " + recipe.getType());
                        }
                    }
                });
                done(file);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }));
    }

    // what a tag ingredient shows, the first item in it
    private static ItemStack shown(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }

    private static void crafting(GuiGraphics graphics, int x, int y, CraftingRecipe recipe) {
        WikiRenderer.panel(graphics, x, y, CRAFTING_WIDTH, CRAFTING_HEIGHT);
        int gridX = x + PAD, gridY = y + PAD;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) WikiRenderer.slot(graphics, gridX + column * SLOT, gridY + row * SLOT, SLOT);
        }
        // shaped recipes keep their shape, shapeless ones fill the grid in order
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            WikiRenderer.item(graphics, shown(ingredients.get(i)), gridX + (i % width) * SLOT + 1, gridY + (i / width) * SLOT + 1);
        }
        int arrowX = gridX + SLOT * 3 + GAP;
        WikiRenderer.arrow(graphics, arrowX, y + (CRAFTING_HEIGHT - WikiRenderer.ARROW_HEIGHT) / 2);
        int outputX = arrowX + WikiRenderer.ARROW_WIDTH + GAP, outputY = y + (CRAFTING_HEIGHT - OUTPUT) / 2;
        WikiRenderer.slot(graphics, outputX, outputY, OUTPUT);
        WikiRenderer.item(graphics, recipe.getResultItem(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess()), outputX + 5, outputY + 5);
    }

    private static void refining(GuiGraphics graphics, int x, int y, RefiningRecipe recipe) {
        WikiRenderer.panel(graphics, x, y, REFINING_WIDTH, REFINING_HEIGHT);
        int inputY = y + PAD + (OUTPUT - SLOT) / 2;
        WikiRenderer.slot(graphics, x + PAD, inputY, SLOT);
        WikiRenderer.item(graphics, shown(recipe.ingredient()).copyWithCount(recipe.count()), x + PAD + 1, inputY + 1);
        int arrowX = x + PAD + SLOT + GAP;
        WikiRenderer.arrow(graphics, arrowX, y + PAD + (OUTPUT - WikiRenderer.ARROW_HEIGHT) / 2);
        int outputX = arrowX + WikiRenderer.ARROW_WIDTH + GAP;
        WikiRenderer.slot(graphics, outputX, y + PAD, OUTPUT);
        WikiRenderer.item(graphics, recipe.result(), outputX + 5, y + PAD + 5);
        // the same text the refining category shows
        String text = String.format(Locale.ROOT, "%d EMC, %.1fs", recipe.emc(), recipe.time() / 20F);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, text, x + (REFINING_WIDTH - font.width(text)) / 2, y + PAD + OUTPUT + 2, 0xFF404040, false);
    }

    /*********************
     * Tooltips & Items *
     *********************/

    private static void tooltip(Path root, String page, String name, Supplier<ItemStack> stack) {
        TASKS.add(new Task(page + "/" + name, () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            ItemStack item = stack.get();
            // never the advanced (F3 + H) tooltip, so ids don't show
            List<Component> lines = item.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL);
            Path file = file(root, page, name);
            try {
                // always in the same spot, rather than kept inside the window like normal, so the window's size doesn't matter
                List<FormattedCharSequence> text = lines.stream().map(Component::getVisualOrderText).toList();
                WikiRenderer.save(file, 600, 400, false, graphics -> graphics.renderTooltip(mc.font, text, (screenWidth, screenHeight, x, y, width, height) -> new Vector2i(x, y), 4, 4));
                done(file);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }));
    }

    private static void items(Path root, String page, String name, int scale, Supplier<List<ItemStack>> stacks) {
        TASKS.add(new Task(page + "/" + name, () -> true, () -> {
            List<ItemStack> list = stacks.get();
            int size = 16 * scale, gap = 4 * scale;
            Path file = file(root, page, name);
            try {
                WikiRenderer.save(file, list.size() * (size + gap) + 4, size + 4, false, graphics -> {
                    for (int i = 0; i < list.size(); i++) WikiRenderer.bigItem(graphics, list.get(i), 2 + i * (size + gap), 2, scale);
                });
                done(file);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }));
    }

    // items in slots on a panel, like an inventory
    private static void grid(Path root, String page, String name, Supplier<List<List<ItemStack>>> rows) {
        TASKS.add(new Task(page + "/" + name, () -> true, () -> {
            List<List<ItemStack>> list = rows.get();
            int columns = list.stream().mapToInt(List::size).max().orElse(0);
            int width = PAD + columns * SLOT + PAD, height = PAD + list.size() * SLOT + PAD;
            Path file = file(root, page, name);
            try {
                WikiRenderer.save(file, width + 4, height + 4, false, graphics -> {
                    WikiRenderer.panel(graphics, 2, 2, width, height);
                    for (int row = 0; row < list.size(); row++) {
                        for (int column = 0; column < columns; column++) {
                            int x = 2 + PAD + column * SLOT, y = 2 + PAD + row * SLOT;
                            WikiRenderer.slot(graphics, x, y, SLOT);
                            if (column < list.get(row).size()) WikiRenderer.item(graphics, list.get(row).get(column), x + 1, y + 1);
                        }
                    }
                });
                done(file);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }));
    }

    /*********
     * Menus *
     *********/

    // sets up a block on the server, opens its menu once the client has the block, then saves the menu once it's had time to fill in
    // setup returns what opens the menu
    private static void menu(Path root, String page, String name, int spot, Class<? extends Screen> screen, int settleTicks, BiFunction<ServerPlayer, BlockPos, Runnable> setup) {
        Runnable[] opener = new Runnable[1];
        TASKS.add(new Task(page + "/" + name + " setup", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            MinecraftServer server = Objects.requireNonNull(mc.getSingleplayerServer(), "Menus can only be saved in singleplayer");
            server.execute(() -> {
                ServerPlayer player = Objects.requireNonNull(server.getPlayerList().getPlayer(Objects.requireNonNull(mc.player).getUUID()));
                // an empty inventory, so nothing from an earlier menu shows up in this one
                player.getInventory().clearContent();
                // each menu gets its own spot around the player, close enough that the menu doesn't close itself
                opener[0] = setup.apply(player, player.blockPosition().offset(SPOTS[spot]));
            });
        }));
        // menus look up their block entity on the client when they open, so it has to have arrived first
        waitTicks(10);
        int[] setupLeft = { 200 };
        TASKS.add(new Task(page + "/" + name + " open", () -> opener[0] != null || setupLeft[0]-- <= 0, () -> {
            if (opener[0] == null) throw new IllegalStateException("Setting up " + page + "/" + name + " failed, see the server's error");
            Objects.requireNonNull(Minecraft.getInstance().getSingleplayerServer()).execute(opener[0]);
        }));
        int[] left = { 200 };
        TASKS.add(new Task(page + "/" + name + " opened", () -> screen.isInstance(Minecraft.getInstance().screen) || left[0]-- <= 0, () -> {}));
        waitTicks(settleTicks);
        TASKS.add(new Task(page + "/" + name, () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!screen.isInstance(mc.screen)) throw new IllegalStateException(screen.getSimpleName() + " never opened");
            Screen open = mc.screen;
            int width = 400, height = 300;
            Path file = file(root, page, name);
            // laid out for the canvas while it's drawn, then put back for the window
            open.resize(mc, width, height);
            try {
                // just the menu itself, not the item lists other mods put beside it
                int[] area = open instanceof AbstractContainerScreen<?> container ? new int[] { container.getGuiLeft(), container.getGuiTop(), container.getXSize(), container.getYSize() } : null;
                WikiRenderer.save(file, width, height, true, area, graphics -> open.render(graphics, -1000, -1000, 0));
                done(file);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            } finally {
                open.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        }));
    }

    private static final Vec3i[] SPOTS = { new Vec3i(2, 0, 0), new Vec3i(0, 0, 2), new Vec3i(-2, 0, 0), new Vec3i(0, 0, -2), new Vec3i(2, 0, 2) };

    // whatever an earlier run left there, without its items dropping
    private static void clear(ServerLevel level, BlockPos... positions) {
        for (BlockPos pos : positions) {
            level.removeBlockEntity(pos);
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }

    /**********
     * Scenes *
     **********/

    // the block under each scene's first block, away from the menus & up in the sky
    private static BlockPos sceneOrigin = BlockPos.ZERO;
    // how far around the origin is cleared, and how high
    private static final int SCENE_RADIUS = 64, SCENE_HEIGHT = 16;
    // looking down at 45 degrees from the corner towards negative x & z, so north & west faces show
    private static final float YAW = -45, PITCH = 45;
    private static final Vec3 LOOK = Vec3.directionFromRotation(PITCH, YAW);
    // how much of the view the scene takes up
    private static final double FILL = 0.5;
    private static final int FOV = 70;

    private static void scenesStart() {
        TASKS.add(new Task("scenes start", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            sceneOrigin = Objects.requireNonNull(mc.player).blockPosition().offset(100, 40, 100);
            // no hud, hand or block outline, and nothing that changes from run to run
            mc.options.hideGui = true;
            inScene = true;
            mc.options.cloudStatus().set(CloudStatus.OFF);
            mc.options.bobView().set(false);
            mc.options.fovEffectScale().set(0D);
            mc.options.fov().set(FOV);
            MinecraftServer server = Objects.requireNonNull(mc.getSingleplayerServer(), "Scenes can only be saved in singleplayer");
            server.execute(() -> server.overworld().setDayTime(6000));
        }));
    }

    private static void scenesEnd() {
        TASKS.add(new Task("scenes end", () -> true, () -> {
            Minecraft.getInstance().options.hideGui = false;
            inScene = false;
        }));
    }

    private static boolean inScene;

    // the sky's a color no block uses while saving scenes, so it can be cut out after
    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!inScene) return;
        event.setRed(1);
        event.setGreen(0);
        event.setBlue(1);
    }

    // builds the scene in the empty sky, frames whatever was built from the corner, then saves it without the sky
    private static void scene(Path root, String page, String name, BiConsumer<ServerLevel, BlockPos> build) {
        boolean[] built = { false };
        TASKS.add(new Task(page + "/" + name + " build", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            MinecraftServer server = Objects.requireNonNull(mc.getSingleplayerServer(), "Scenes can only be saved in singleplayer");
            server.execute(() -> {
                ServerPlayer player = Objects.requireNonNull(server.getPlayerList().getPlayer(Objects.requireNonNull(mc.player).getUUID()));
                ServerLevel level = player.serverLevel();
                clearScene(level);
                build.accept(level, sceneOrigin.above());
                AABB bounds = sceneBounds(level);
                Vec3 eye = camera(bounds);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.setDeltaMovement(Vec3.ZERO);
                player.connection.teleport(eye.x, eye.y - player.getEyeHeight(), eye.z, YAW, PITCH);
                built[0] = true;
            });
        }));
        int[] buildLeft = { 200 };
        TASKS.add(new Task(page + "/" + name + " built", () -> built[0] || buildLeft[0]-- <= 0, () -> {}));
        // chunks, block entities & lighting need a moment to get to the client and be drawn
        waitTicks(40);
        int[] left = { 600 };
        TASKS.add(new Task(page + "/" + name + " drawn", () -> Minecraft.getInstance().levelRenderer.hasRenderedAllSections() || left[0]-- <= 0, () -> {}));
        waitTicks(10);
        TASKS.add(new Task(page + "/" + name, () -> true, () -> {
            if (!built[0]) throw new IllegalStateException("Building " + page + "/" + name + " failed, see the server's error");
            Path file = file(root, page, name);
            try (NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                WikiRenderer.saveKeyed(file, image);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            done(file);
        }));
    }

    // the last scene, without dropping anything that was in it
    private static void clearScene(ServerLevel level) {
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -SCENE_RADIUS; x <= SCENE_RADIUS; x++) {
            for (int z = -SCENE_RADIUS; z <= SCENE_RADIUS; z++) {
                for (int y = 0; y <= SCENE_HEIGHT; y++) {
                    pos.setWithOffset(sceneOrigin, x, y, z);
                    if (level.getBlockState(pos).isAir()) continue;
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                }
            }
        }
        level.getEntitiesOfClass(ItemEntity.class, new AABB(sceneOrigin).inflate(SCENE_RADIUS, SCENE_HEIGHT, SCENE_RADIUS)).forEach(Entity::discard);
    }

    // everything built, using each block's shape so thin blocks are framed tightly
    private static AABB sceneBounds(ServerLevel level) {
        AABB bounds = null;
        for (BlockPos pos : BlockPos.betweenClosed(sceneOrigin.offset(-SCENE_RADIUS, 1, -SCENE_RADIUS), sceneOrigin.offset(SCENE_RADIUS, SCENE_HEIGHT, SCENE_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            VoxelShape shape = state.getShape(level, pos);
            AABB box = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
            bounds = bounds == null ? box : bounds.minmax(box);
        }
        if (bounds == null) throw new IllegalStateException("The scene is empty");
        return bounds;
    }

    // far enough back along the view that the scene's bounding sphere takes up FILL of the view's height
    private static Vec3 camera(AABB bounds) {
        double radius = Math.sqrt(bounds.getXsize() * bounds.getXsize() + bounds.getYsize() * bounds.getYsize() + bounds.getZsize() * bounds.getZsize()) / 2;
        double distance = radius / Math.sin(Math.toRadians(FOV / 2D * FILL));
        return bounds.getCenter().subtract(LOOK.scale(distance));
    }

    // shaped to fit its neighbors, like it was placed by a player
    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(state, level, pos));
    }

    // rows that read left to right from the camera, each row further back
    private static void rows(ServerLevel level, BlockPos pos, List<? extends Block> blocks, int columns) {
        for (int i = 0; i < blocks.size(); i++) place(level, pos.offset(i / columns, 0, i % columns), blocks.get(i).defaultBlockState());
    }

    private static void use(ServerPlayer player, BlockPos pos, Direction side) {
        player.serverLevel().getBlockState(pos).useWithoutItem(player.serverLevel(), player, new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false));
    }

    private static BlockEntityCondenserMK3 condenser(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        clear(level, pos);
        level.setBlockAndUpdate(pos, Blocks.CONDENSER_MK3.get().defaultBlockState());
        BlockEntityCondenserMK3 condenser = (BlockEntityCondenserMK3) Objects.requireNonNull(level.getBlockEntity(pos));
        condenser.getSidedHandler(Direction.UP).setLockInfoFromPacket(ItemInfo.fromItem(net.minecraft.world.item.Items.IRON_INGOT));
        IItemHandler input = condenser.getAutomationSidedItemHandler(Direction.UP);
        // more than the output holds, so some stay in the input
        for (int i = 0; i < 8; i++) ItemHandlerHelper.insertItem(input, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64), false);
        return condenser;
    }
}
