package cool.furry.mc.projectexpansion.wiki;

import com.mojang.brigadier.Command;
import cool.furry.mc.projectexpansion.Main;
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
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.pipe.Distribution;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
            TASKS.add(new Task("join", () -> mc.player != null && mc.screen == null, () -> {}));
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
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
            return;
        }
        // flat, creative & always noon, so nothing changes between runs
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
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
        items(root, "Condenser-MK3", "block", 6, () -> List.of(new ItemStack(Blocks.CONDENSER_MK3.get())));
        items(root, "EMC-Refinery", "block", 6, () -> List.of(new ItemStack(Blocks.EMC_REFINERY.get())));
        items(root, "Ianthium", "items", 3, () -> List.of(
                new ItemStack(Blocks.IANTHIUM_ORE.get()), new ItemStack(Blocks.DEEPSLATE_IANTHIUM_ORE.get()), new ItemStack(Items.RAW_IANTHIUM.get()),
                new ItemStack(Blocks.RAW_IANTHIUM_BLOCK.get()), new ItemStack(Items.IANTHIUM_INGOT.get()), new ItemStack(Items.IANTHIUM_NUGGET.get()),
                new ItemStack(Blocks.IANTHIUM_BLOCK.get())));

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
        TASKS.add(new Task(page + "/" + name + " open", () -> opener[0] != null, () -> Objects.requireNonNull(Minecraft.getInstance().getSingleplayerServer()).execute(opener[0])));
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
