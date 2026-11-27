package cool.furry.mc.projectexpansion.wiki;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.gui.EMCDisplay;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.wiki.images.Advancements;
import cool.furry.mc.projectexpansion.wiki.images.Displays;
import cool.furry.mc.projectexpansion.wiki.images.Items;
import cool.furry.mc.projectexpansion.wiki.images.Menus;
import cool.furry.mc.projectexpansion.wiki.images.Recipes;
import cool.furry.mc.projectexpansion.wiki.images.Scenes;
import cool.furry.mc.projectexpansion.wiki.images.Tooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ToastAddEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.LoadingErrorScreen;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

// saves the wiki's images, so they all look the same and can be redone when something changes
// images are static methods marked with WikiImage in the DEFINITIONS classes, under wiki.images
// the wikiImages run does them all and quits, /pexwiki does them in any dev world, both save to images/<Page>/<name>.png
// either can be limited to some of them, with -PwikiOnly=... or /pexwiki ..., which is part of a path or a glob like EMC-Link/*
@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public final class WikiImages {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProjectExpansion Wiki Images");
    // the classes images are declared in
    private static final List<Class<?>> DEFINITIONS = List.of(Recipes.class, Tooltips.class, Advancements.class, Items.class, Displays.class, cool.furry.mc.projectexpansion.wiki.images.Commands.class, Menus.class, Scenes.class);
    // set by the wikiImages run, to where the images go, and optionally which ones
    private static final String PROPERTY = "projectexpansion.wikiImages", ONLY_PROPERTY = "projectexpansion.wikiImages.only";
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
        Path root = FMLPaths.GAMEDIR.get().resolve("wiki-images");
        event.getDispatcher().register(Commands.literal("pexwiki")
                .executes(ctx -> {
                    queueAll(root, null, false);
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("only", StringArgumentType.greedyString()).executes(ctx -> {
                    queueAll(root, StringArgumentType.getString(ctx, "only"), false);
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        String dir = System.getProperty(PROPERTY);
        // nobody's there to click past it, so the run would wait forever
        if (dir != null && !started && mc.screen instanceof LoadingErrorScreen) {
            started = true;
            LOGGER.error("Mods loaded with errors or warnings, so no wiki images were saved, see the log above");
            mc.stop();
            return;
        }
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
                MinecraftServer server = server();
                server.execute(() -> player(server).connection.teleport(0.5, 100, 0.5, 0, 0));
            }));
            waitTicks(100);
            TASKS.add(new Task("queue", () -> true, () -> queueAll(Path.of(dir), System.getProperty(ONLY_PROPERTY), true)));
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
        rules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, null);
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

    private static MinecraftServer server() {
        return Objects.requireNonNull(Minecraft.getInstance().getSingleplayerServer(), "Menus & scenes can only be saved in singleplayer");
    }

    private static ServerPlayer player(MinecraftServer server) {
        return Objects.requireNonNull(server.getPlayerList().getPlayer(Objects.requireNonNull(Minecraft.getInstance().player).getUUID()));
    }

    /**********
     * Images *
     **********/

    // what an image is, from its annotation, and what its method has to look like
    private enum Kind {
        RECIPE(Recipe.class, new Signature(void.class)),
        TOOLTIP(Tooltip.class, new Signature(ItemStack.class), new Signature(ItemStack.class, ServerPlayer.class)),
        ADVANCEMENT(Advancement.class, new Signature(void.class)),
        ITEM_ROW(ItemRow.class, new Signature(List.class)),
        ITEM_GRID(ItemGrid.class, new Signature(List.class)),
        EMC_DISPLAY(EmcDisplay.class, new Signature(BigInteger.class)),
        COMMAND(Command.class, new Signature(String.class, ServerPlayer.class)),
        MENU(Menu.class, new Signature(Runnable.class, ServerPlayer.class, BlockPos.class)),
        SCENE(Scene.class, new Signature(void.class, ServerLevel.class, BlockPos.class), new Signature(Runnable.class, ServerPlayer.class, BlockPos.class));

        private final Class<? extends Annotation> annotation;
        // any one of these
        private final Signature[] signatures;

        Kind(Class<? extends Annotation> annotation, Signature... signatures) {
            this.annotation = annotation;
            this.signatures = signatures;
        }

        private boolean matches(Method method) {
            return Arrays.stream(signatures).anyMatch(signature -> method.getReturnType() == signature.returns() && Arrays.equals(method.getParameterTypes(), signature.parameters()));
        }
    }

    private record Signature(Class<?> returns, Class<?>... parameters) {
        @Override
        public String toString() {
            return returns.getSimpleName() + "(" + String.join(", ", Arrays.stream(parameters).map(Class::getSimpleName).toList()) + ")";
        }
    }

    private record Image(String path, boolean cutout, Kind kind, Method method) {
        private <T extends Annotation> T type(Class<T> annotation) {
            return Objects.requireNonNull(method.getAnnotation(annotation));
        }

        // the method's own exception, rather than reflection's wrapper around it
        private Object call(Object... args) {
            try {
                return method.invoke(null, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException cause) throw cause;
                throw new IllegalStateException(e.getCause());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private String name() {
            return path + " (" + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + ")";
        }
    }

    // every image, in the order they're saved: by kind, so scenes come last, then by path
    // anything set up wrong is added to errors instead
    private static List<Image> findImages(List<String> errors) {
        List<Image> images = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        for (Class<?> definitions : DEFINITIONS) {
            for (Method method : definitions.getDeclaredMethods()) {
                WikiImage image = method.getAnnotation(WikiImage.class);
                String where = definitions.getSimpleName() + "#" + method.getName();
                List<Kind> kinds = Arrays.stream(Kind.values()).filter(kind -> method.isAnnotationPresent(kind.annotation)).toList();
                if (image == null) {
                    if (!kinds.isEmpty()) errors.add(where + " has @" + kinds.getFirst().annotation.getSimpleName() + " but no @WikiImage");
                    continue;
                }
                if (kinds.size() != 1) {
                    errors.add(where + " needs exactly one of " + String.join(", ", Arrays.stream(Kind.values()).map(kind -> "@" + kind.annotation.getSimpleName()).toList()));
                    continue;
                }
                Kind kind = kinds.getFirst();
                if (!Modifier.isStatic(method.getModifiers()) || !kind.matches(method)) {
                    errors.add(where + " has to be static " + String.join(" or ", Arrays.stream(kind.signatures).map(Signature::toString).toList()) + " for @" + kind.annotation.getSimpleName());
                    continue;
                }
                if (kind == Kind.SCENE && image.cutout() && method.getAnnotation(Scene.class).hud()) {
                    errors.add(where + " can't have both cutout & hud, the hud can't be cut out");
                    continue;
                }
                if (!image.value().matches("[^/]+/[^/]+\\.png")) {
                    errors.add(where + "'s path \"" + image.value() + "\" has to be Page/name.png");
                    continue;
                }
                if (!paths.add(image.value())) {
                    errors.add(where + " saves to " + image.value() + ", which another image already does");
                    continue;
                }
                method.setAccessible(true);
                images.add(new Image(image.value(), image.cutout(), kind, method));
            }
        }
        images.sort(Comparator.comparing(Image::kind).thenComparing(Image::path));
        return images;
    }

    // part of a path, or a glob when it has a *
    private static boolean matches(String path, @Nullable String only) {
        if (only == null || only.isBlank()) return true;
        if (!only.contains("*")) return path.contains(only);
        return FileSystems.getDefault().getPathMatcher("glob:" + only).matches(Path.of(path));
    }

    private static boolean running;

    private static void queueAll(Path root, @Nullable String only, boolean quit) {
        saved = failed = 0;
        running = true;
        // nothing that moves on its own, so every image comes out the same every time
        TASKS.add(new Task("freeze", () -> true, WikiFreeze::start));
        List<String> errors = new ArrayList<>();
        List<Image> images = findImages(errors).stream().filter(image -> matches(image.path(), only)).toList();
        for (String error : errors) {
            failed++;
            LOGGER.error("Wiki image set up wrong: {}", error);
        }
        if (images.isEmpty()) LOGGER.warn("No wiki images match {}", only);
        boolean scenes = false;
        for (Image image : images) {
            Path file = root.resolve(image.path());
            boolean fullEmc = image.method().getAnnotation(WikiImage.class).fullEmc();
            boolean[] formatted = new boolean[1];
            if (fullEmc) TASKS.add(new Task(image.name() + " full emc", () -> true, () -> {
                formatted[0] = Config.client.formatEMC.get();
                Config.client.formatEMC.set(false);
            }));
            switch (image.kind()) {
                case RECIPE -> recipe(image, file);
                case TOOLTIP -> tooltip(image, file);
                case ADVANCEMENT -> advancement(image, file);
                case ITEM_ROW -> itemRow(image, file);
                case ITEM_GRID -> itemGrid(image, file);
                case EMC_DISPLAY -> emcDisplay(image, file);
                case COMMAND -> command(image, file);
                case MENU -> menu(image, file);
                case SCENE -> {
                    if (!scenes) scenesStart();
                    scenes = true;
                    scene(image, file);
                }
            }
            if (fullEmc) TASKS.add(new Task(image.name() + " formatted emc", () -> true, () -> Config.client.formatEMC.set(formatted[0])));
        }
        if (scenes) scenesEnd();

        TASKS.add(new Task("done", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            running = false;
            WikiFreeze.stop();
            WikiKeys.release();
            String message = String.format("Saved %d wiki images to %s, %d failed", saved, root.toAbsolutePath(), failed);
            LOGGER.info(message);
            if (mc.player != null) mc.player.displayClientMessage(Component.literal(message), false);
            if (quit) mc.stop();
        }));
    }

    private static void saved(Path file) {
        saved++;
        LOGGER.info("Saved {}", file);
    }

    /***********************************
     * Recipes, Tooltips, Rows & Grids *
     ***********************************/

    private static void recipe(Image image, Path file) {
        TASKS.add(new Task(image.name(), () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            List<net.minecraft.world.item.crafting.Recipe<?>> recipes = new ArrayList<>();
            for (String id : image.type(Recipe.class).value()) {
                ResourceLocation location = id.contains(":") ? ResourceLocation.parse(id) : Main.rl(id);
                recipes.add(Objects.requireNonNull(mc.level).getRecipeManager().byKey(location).orElseThrow(() -> new IllegalStateException("No recipe " + location)).value());
            }
            save(file, 256, 256, false, null, graphics -> RecipeDrawer.draw(graphics, recipes));
        }));
    }

    // one taking the player is made on the server, so it can set up things like knowledge, and drawn once that's reached the client
    private static void tooltip(Image image, Path file) {
        ItemStack[] made = new ItemStack[1];
        if (image.method().getParameterCount() == 1) {
            TASKS.add(new Task(image.name() + " setup", () -> true, () -> {
                MinecraftServer server = server();
                server.execute(() -> made[0] = (ItemStack) image.call(player(server)));
            }));
            waitTicks(5);
        }
        TASKS.add(new Task(image.name(), () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (image.method().getParameterCount() == 1 && made[0] == null) throw new IllegalStateException("Setting up " + image.name() + " failed, see the server's error");
            ItemStack stack = made[0] != null ? made[0] : (ItemStack) image.call();
            // never the advanced (F3 + H) tooltip, so ids don't show
            List<FormattedCharSequence> lines = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL).stream().map(Component::getVisualOrderText).toList();
            // always in the same spot, rather than kept inside the window like normal, so the window's size doesn't matter
            save(file, 600, 400, false, null, graphics -> graphics.renderTooltip(mc.font, lines, (screenWidth, screenHeight, x, y, width, height) -> new Vector2i(x, y), 4, 4));
        }));
    }

    // from the server, since the client only knows the ones the player can already see
    private static void advancement(Image image, Path file) {
        TASKS.add(new Task(image.name(), () -> true, () -> {
            Advancement type = image.type(Advancement.class);
            List<DisplayInfo> displays = new ArrayList<>();
            for (String id : type.value()) {
                ResourceLocation location = id.contains(":") ? ResourceLocation.parse(id) : Main.rl(id);
                AdvancementHolder advancement = server().getAdvancements().get(location);
                if (advancement == null) throw new IllegalStateException("No advancement " + location);
                if (advancement.value().display().isEmpty()) throw new IllegalStateException(location + " has no display, so it never shows a toast");
                displays.add(advancement.value().display().get());
            }
            // the game's toast is always 160 wide and wraps the title, then shows the header & title one at a time, so it's drawn here
            // the same, but as wide as the text needs, with the header & title both on one line
            Font font = Minecraft.getInstance().font;
            int width = displays.stream().mapToInt(display -> Math.max(TOAST_WIDTH, 30 + Math.max(font.width(display.getType().getDisplayName()), font.width(display.getTitle())) + 5)).max().orElse(TOAST_WIDTH);
            int columns = Math.max(1, Math.min(type.columns(), displays.size())), rows = (displays.size() + columns - 1) / columns;
            // a gap only between toasts in a grid, a single column stacks them like the game does
            int gap = columns > 1 ? 4 : 0;
            save(file, columns * (width + gap) - gap + 4, rows * (TOAST_HEIGHT + gap) - gap + 4, false, null, graphics -> {
                graphics.pose().pushPose();
                graphics.pose().translate(2, 2, 0);
                for (int index = 0; index < displays.size(); index++) {
                    DisplayInfo display = displays.get(index);
                    graphics.pose().pushPose();
                    graphics.pose().translate(index % columns * (width + gap), index / columns * (TOAST_HEIGHT + gap), 0);
                    toastBackground(graphics, width);
                    graphics.drawString(font, display.getType().getDisplayName(), 30, 7, display.getType() == AdvancementType.CHALLENGE ? 0xFFFF88FF : 0xFFFFFF00, false);
                    graphics.drawString(font, display.getTitle(), 30, 18, 0xFFFFFFFF, false);
                    graphics.renderFakeItem(display.getIcon(), 8, 8);
                    graphics.pose().popPose();
                }
                graphics.pose().popPose();
            });
        }));
    }

    private static final ResourceLocation TOAST = ResourceLocation.withDefaultNamespace("toast/advancement");
    private static final int TOAST_WIDTH = 160, TOAST_HEIGHT = 32, TOAST_BORDER = 4;

    // the sprite's ends as they are, and its plain middle repeated as far as it needs to go
    private static void toastBackground(GuiGraphics graphics, int width) {
        int middle = TOAST_WIDTH - TOAST_BORDER * 2;
        graphics.blitSprite(TOAST, TOAST_WIDTH, TOAST_HEIGHT, 0, 0, 0, 0, TOAST_BORDER, TOAST_HEIGHT);
        for (int x = TOAST_BORDER; x < width - TOAST_BORDER; x += middle) {
            graphics.blitSprite(TOAST, TOAST_WIDTH, TOAST_HEIGHT, TOAST_BORDER, 0, x, 0, Math.min(middle, width - TOAST_BORDER - x), TOAST_HEIGHT);
        }
        graphics.blitSprite(TOAST, TOAST_WIDTH, TOAST_HEIGHT, TOAST_WIDTH - TOAST_BORDER, 0, width - TOAST_BORDER, 0, TOAST_BORDER, TOAST_HEIGHT);
    }

    // gives the player the emc, waits for the display's per second gain to go back to nothing after the jump, then draws the display on its own
    private static void emcDisplay(Image image, Path file) {
        TASKS.add(new Task(image.name() + " setup", () -> true, () -> {
            BigInteger emc = (BigInteger) image.call();
            MinecraftServer server = server();
            server.execute(() -> WikiPlayer.setEmc(player(server), emc));
        }));
        // it averages what was gained over the last two seconds, checked once a second
        waitTicks(65);
        TASKS.add(new Task(image.name(), () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            // it's placed by the window's size, so the canvas matches it
            save(file, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), false, null, graphics -> EMCDisplay.INSTANCE.render(graphics, DeltaTracker.ZERO));
        }));
    }

    // runs the command & opens the chat with it typed again, so its usage shows under what it said, like right after running it
    private static void command(Image image, Path file) {
        Command command = image.type(Command.class);
        String[] text = new String[1];
        TASKS.add(new Task(image.name() + " setup", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            MinecraftServer server = server();
            server.execute(() -> text[0] = (String) image.call(player(server)));
        }));
        // what the setup changed reaches the client, then the chat's cleared of anything it said
        waitTicks(5);
        TASKS.add(new Task(image.name() + " run", () -> text[0] != null, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.getChat().clearMessages(false);
            if (command.suggestions()) return;
            if (command.console()) {
                MinecraftServer server = server();
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), text[0]));
            } else {
                Objects.requireNonNull(mc.player).connection.sendCommand(text[0]);
            }
        }));
        waitTicks(10);
        if (command.typed() || command.suggestions()) {
            TASKS.add(new Task(image.name() + " type", () -> true, () -> Minecraft.getInstance().setScreen(new ChatScreen("/" + text[0]))));
            // suggestions are worked out in the background
            waitTicks(10);
            if (command.suggestions()) {
                TASKS.add(new Task(image.name() + " suggest", () -> true, () -> suggestions((ChatScreen) Objects.requireNonNull(Minecraft.getInstance().screen)).showSuggestions(false)));
                waitTicks(2);
            }
        }
        TASKS.add(new Task(image.name(), () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screen chat = mc.screen;
            // the chat's placed by the window's size, so the canvas matches it, and the screen draws the chat itself while it's open
            save(file, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), false, null, graphics -> {
                if (chat instanceof ChatScreen) {
                    unfocus(chat);
                    chat.render(graphics, -1000, -1000, 0);
                }
                else mc.gui.getChat().render(graphics, mc.gui.getGuiTicks(), -1000, -1000, false);
            });
            mc.setScreen(null);
        }));
    }

    static void setMouse(Minecraft mc, double x, double y) {
        try {
            for (String name : new String[] { "xpos", "ypos" }) {
                Field field = MouseHandler.class.getDeclaredField(name);
                field.setAccessible(true);
                field.setDouble(mc.mouseHandler, name.equals("xpos") ? x : y);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't move the mouse", e);
        }
    }

    // no blinking cursor in a text box, which comes and goes with the time
    // some, like the chat's, are set to never lose focus, so that's allowed first
    private static void unfocus(Screen screen) {
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof EditBox box)) continue;
            box.setCanLoseFocus(true);
            box.setFocused(false);
        }
        screen.setFocused(null);
    }

    // it's not public, and only opens itself when something's typed
    private static CommandSuggestions suggestions(ChatScreen chat) {
        try {
            Field field = ChatScreen.class.getDeclaredField("commandSuggestions");
            field.setAccessible(true);
            return (CommandSuggestions) field.get(chat);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't get the chat's suggestions", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void itemRow(Image image, Path file) {
        TASKS.add(new Task(image.name(), () -> true, () -> {
            List<ItemStack> stacks = (List<ItemStack>) image.call();
            int scale = image.type(ItemRow.class).scale(), size = 16 * scale, gap = 4 * scale;
            save(file, stacks.size() * (size + gap) + 4, size + 4, false, null, graphics -> {
                for (int i = 0; i < stacks.size(); i++) WikiRenderer.bigItem(graphics, stacks.get(i), 2 + i * (size + gap), 2, scale);
            });
        }));
    }

    @SuppressWarnings("unchecked")
    private static void itemGrid(Image image, Path file) {
        TASKS.add(new Task(image.name(), () -> true, () -> {
            List<List<ItemStack>> rows = (List<List<ItemStack>>) image.call();
            int columns = rows.stream().mapToInt(List::size).max().orElse(0);
            int pad = WikiRenderer.PAD, slot = WikiRenderer.SLOT;
            int width = pad + columns * slot + pad, height = pad + rows.size() * slot + pad;
            save(file, width + 4, height + 4, false, null, graphics -> {
                WikiRenderer.panel(graphics, 2, 2, width, height);
                for (int row = 0; row < rows.size(); row++) {
                    for (int column = 0; column < columns; column++) {
                        int x = 2 + pad + column * slot, y = 2 + pad + row * slot;
                        WikiRenderer.slot(graphics, x, y, slot);
                        if (column < rows.get(row).size()) WikiRenderer.item(graphics, rows.get(row).get(column), x + 1, y + 1);
                    }
                }
            });
        }));
    }

    private static void save(Path file, int width, int height, boolean onlyOpaque, int @Nullable [] area, WikiRenderer.Drawing drawing) {
        try {
            WikiRenderer.save(file, width, height, onlyOpaque, area, drawing);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        saved(file);
    }

    /*********
     * Menus *
     *********/

    // sets up around a spot beside the player on the server, opens the menu once the client has the blocks, then saves it once it's had time to fill in
    private static void menu(Image image, Path file) {
        Menu menu = image.type(Menu.class);
        Class<? extends Screen> screen = menu.screen();
        Runnable[] opener = new Runnable[1];
        TASKS.add(new Task(image.name() + " setup", () -> true, () -> {
            Minecraft.getInstance().setScreen(null);
            MinecraftServer server = server();
            server.execute(() -> {
                ServerPlayer player = player(server);
                // an empty inventory, so nothing from an earlier menu shows up in this one
                player.getInventory().clearContent();
                // close enough that the menu doesn't close itself
                opener[0] = (Runnable) image.call(player, player.blockPosition().east(2));
            });
        }));
        // menus look up their block entity on the client when they open, so it has to have arrived first
        waitTicks(10);
        int[] setupLeft = { 200 };
        TASKS.add(new Task(image.name() + " open", () -> opener[0] != null || setupLeft[0]-- <= 0, () -> {
            if (opener[0] == null) throw new IllegalStateException("Setting up " + image.name() + " failed, see the server's error");
            server().execute(opener[0]);
        }));
        int[] openLeft = { 200 };
        TASKS.add(new Task(image.name() + " opened", () -> screen.isInstance(Minecraft.getInstance().screen) || openLeft[0]-- <= 0, () -> {}));
        waitTicks(menu.settleTicks());
        if (menu.shift()) {
            TASKS.add(new Task(image.name() + " shift", () -> true, () -> WikiKeys.shift(true)));
            int[] shiftLeft = { 40 };
            TASKS.add(new Task(image.name() + " shift held", () -> WikiKeys.shiftDown() || shiftLeft[0]-- <= 0, () -> {
                if (!WikiKeys.shiftDown()) throw new IllegalStateException("The game never saw shift held");
            }));
        }
        int[] mouse = menu.mouse();
        if (mouse.length == 2 && !image.cutout()) {
            // where the game thinks the cursor is for a whole window screenshot, which the next frame picks up
            // moving the real one doesn't reach the game on a virtual display
            TASKS.add(new Task(image.name() + " mouse", () -> true, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) return;
                int[] origin = origin(mc.screen);
                double scale = mc.getWindow().getGuiScale();
                setMouse(mc, (origin[0] + mouse[0]) * scale, (origin[1] + mouse[1]) * scale);
            }));
            waitTicks(2);
        }
        int[] crop = menu.crop();
        TASKS.add(new Task(image.name(), () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            try {
                if (!screen.isInstance(mc.screen)) throw new IllegalStateException(screen.getSimpleName() + " never opened");
                if (!image.cutout()) {
                    int[] pixels = null;
                    if (crop.length == 4) {
                        int[] origin = origin(mc.screen);
                        double scale = mc.getWindow().getGuiScale();
                        pixels = new int[] { (int) ((origin[0] + crop[0]) * scale), (int) ((origin[1] + crop[1]) * scale), (int) (crop[2] * scale), (int) (crop[3] * scale) };
                    }
                    screenshot(file, pixels);
                    return;
                }
            } finally {
                if (menu.shift()) WikiKeys.shift(false);
            }
            Screen open = mc.screen;
            // wide enough for a long tooltip off the side of a menu
            int width = 640, height = 300;
            // laid out for the canvas while it's drawn, then put back for the window
            // tooltips wrap to fit the window, so it's scaled to be at least as big as the canvas meanwhile
            double guiScale = mc.getWindow().getGuiScale();
            mc.getWindow().setGuiScale(Math.min(guiScale, Math.floor(Math.min((double) mc.getWindow().getWidth() / width, (double) mc.getWindow().getHeight() / height))));
            open.resize(mc, width, height);
            try {
                // just the menu itself, not the item lists other mods put beside it
                int[] area = open instanceof AbstractContainerScreen<?> container ? crop.length == 4 ? new int[] { container.getGuiLeft() + crop[0], container.getGuiTop() + crop[1], crop[2], crop[3] } : new int[] { container.getGuiLeft(), container.getGuiTop(), container.getXSize(), container.getYSize() } : null;
                AbstractContainerScreen<?> container = open instanceof AbstractContainerScreen<?> c ? c : null;
                int mouseX = mouse.length == 2 && container != null ? container.getGuiLeft() + mouse[0] : -1000, mouseY = mouse.length == 2 && container != null ? container.getGuiTop() + mouse[1] : -1000;
                unfocus(open);
                // emi's lists would show up behind a tooltip or in a crop past the menu
                emi(false);
                save(file, width, height, true, area, graphics -> open.renderWithTooltip(graphics, mouseX, mouseY, 0));
            } finally {
                emi(true);
                mc.getWindow().setGuiScale(guiScale);
                open.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        }));
    }

    /**********
     * Scenes *
     **********/

    // the block under each scene's first block, away from the menus & up in the sky
    private static BlockPos sceneOrigin = BlockPos.ZERO;
    // how far around the origin is cleared, and how high
    private static final int SCENE_RADIUS = 64, SCENE_HEIGHT = 16;
    private static final int FOV = 70;
    private static boolean cutoutScene;
    // an item that just landed starts at 5 and counts down a tick at a time, squished the most at the start, this is about halfway through
    private static final int POP_TIME = 4;

    private static void scenesStart() {
        TASKS.add(new Task("scenes start", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            sceneOrigin = Objects.requireNonNull(mc.player).blockPosition().offset(100, 40, 100);
            // no hud, hand or block outline, and nothing that changes from run to run
            mc.options.hideGui = true;
            mc.options.cloudStatus().set(CloudStatus.OFF);
            mc.options.bobView().set(false);
            mc.options.fovEffectScale().set(0D);
            mc.options.fov().set(FOV);
            // nothing over a scene's hud but vanilla's own, see guiLayer & toast
            inScenes = true;
            mc.getToasts().clear();
            mc.getTutorial().setStep(TutorialSteps.NONE);
            JadeTooltip.hide();
        }));
    }

    private static boolean inScenes;

    // other mods' hud, like the emc display, isn't part of what a scene's showing
    @SubscribeEvent
    public static void guiLayer(RenderGuiLayerEvent.Pre event) {
        if (inScenes && !event.getName().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) event.setCanceled(true);
    }

    // the recipes & advancements unlocked by items given to the player would cover menus & scenes
    @SubscribeEvent
    public static void toast(ToastAddEvent event) {
        if (running) event.setCanceled(true);
    }

    // how long an emc reload took changes every run, so the image always says the same time
    @SubscribeEvent
    public static void chat(ClientChatReceivedEvent.System event) {
        if (!running || !(event.getMessage().getContents() instanceof TranslatableContents contents) || !contents.getKey().equals(Lang.Commands.RELOAD_EMC_SUCCESS.getTranslationKey()) || contents.getArgs().length != 2) return;
        Object[] args = contents.getArgs().clone();
        args[1] = Component.literal("1500ms").withStyle(ChatFormatting.GRAY);
        event.setMessage(Component.translatable(contents.getKey(), args).setStyle(event.getMessage().getStyle()));
    }

    private static void scenesEnd() {
        TASKS.add(new Task("scenes end", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            inScenes = false;
            JadeTooltip.restore();
            cutoutScene = false;
            SceneEffects.restore(Objects.requireNonNull(mc.level));
            MinecraftServer server = server();
            server.execute(() -> setTimeAndWeather(player(server), 6000, Scene.Weather.CLEAR));
        }));
    }

    // black while saving cutout scenes, so see through blocks blended over the cleared background can be taken back out of it, see WikiRenderer.saveCutout
    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!cutoutScene) return;
        event.setRed(0);
        event.setGreen(0);
        event.setBlue(0);
    }

    // builds the scene in the empty sky, frames whatever was built, then saves it, without the sky when it's a cutout
    private static void scene(Image image, Path file) {
        Scene scene = image.type(Scene.class);
        boolean[] built = { false };
        Runnable[] action = new Runnable[1];
        Framed[] framed = new Framed[1];
        TASKS.add(new Task(image.name() + " clear", () -> true, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = !scene.hud();
            // no sky, clouds or rain to cut around, or the normal sky without anything under the horizon
            cutoutScene = image.cutout();
            SceneEffects.apply(Objects.requireNonNull(mc.level), cutoutScene);
            // the last scene's cleared a tick before this one's built, or a block put back where the same one was never gets to the client
            // as removed, and the client keeps its old block entity, like an open condenser's lid
            server().execute(() -> clearScene(player(server()).serverLevel()));
        }));
        waitTicks(2);
        TASKS.add(new Task(image.name() + " build", () -> true, () -> {
            MinecraftServer server = server();
            server.execute(() -> {
                ServerPlayer player = player(server);
                ServerLevel level = player.serverLevel();
                setTimeAndWeather(player, scene.time(), scene.weather());
                // an empty hotbar with the first slot picked, and no emc, so a scene doesn't depend on the ones before it, it can set what it needs
                player.getInventory().clearContent();
                player.getInventory().selected = 0;
                WikiPlayer.setEmc(player, 0);
                // a scene can switch to survival, like for a block to drop something when it's broken
                player.setGameMode(GameType.CREATIVE);
                if (image.method().getParameterTypes()[0] == ServerPlayer.class) action[0] = (Runnable) image.call(player, sceneOrigin.above());
                else image.call(level, sceneOrigin.above());
                player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
                float yaw = Float.isNaN(scene.yaw()) ? scene.view().yaw : scene.yaw(), pitch = Float.isNaN(scene.pitch()) ? scene.view().pitch : scene.pitch();
                AABB bounds = frame(scene, sceneOrigin.above());
                Vec3 eye = camera(bounds == null ? sceneBounds(level) : bounds, scene.fill(), yaw, pitch);
                if (bounds != null) framed[0] = new Framed(bounds, eye, yaw, pitch);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.setDeltaMovement(Vec3.ZERO);
                player.connection.teleport(eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
                built[0] = true;
            });
        }));
        int[] buildLeft = { 200 };
        TASKS.add(new Task(image.name() + " built", () -> built[0] || buildLeft[0]-- <= 0, () -> {}));
        // chunks, block entities & lighting need a moment to get to the client and be drawn
        waitTicks(40);
        int[] drawLeft = { 600 };
        TASKS.add(new Task(image.name() + " drawn", () -> Minecraft.getInstance().levelRenderer.hasRenderedAllSections() || drawLeft[0]-- <= 0, () -> {}));
        // long enough for anything it does to show, but before a message above the hotbar starts fading
        TASKS.add(new Task(image.name() + " action", () -> true, () -> {
            // anything said while it was built, like advancements made from the items it gave
            Minecraft.getInstance().gui.getChat().clearMessages(false);
            if (action[0] != null) server().execute(action[0]);
        }));
        waitTicks(scene.actionTicks());
        if (scene.pop() >= 0) {
            // the item's sent to the client a tick or so after it's added, so the squish is started here instead, and the next tick gets the frame drawn partway through
            TASKS.add(new Task(image.name() + " pop", () -> true, () -> {
                ItemStack stack = Objects.requireNonNull(Minecraft.getInstance().player).getInventory().getItem(scene.pop());
                if (stack.isEmpty()) throw new IllegalStateException("There's nothing in hotbar slot " + scene.pop() + " to pop");
                stack.setPopTime(POP_TIME);
            }));
            waitTicks(1);
        }
        TASKS.add(new Task(image.name(), () -> true, () -> {
            if (!built[0]) throw new IllegalStateException("Building " + image.name() + " failed, see the server's error");
            screenshot(file, image.cutout(), framed[0]);
        }));
    }

    // a frame drawn right now at the very start of the tick, rather than whenever the game last drew one, which is somewhere between ticks
    // so anything that moves smoothly between them, like an item popping into the hotbar, is in the same spot every time
    private static void drawFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) unfocus(mc.screen);
        RenderTarget target = mc.getMainRenderTarget();
        target.bindWrite(true);
        FogRenderer.setupNoFog();
        RenderSystem.enableCull();
        mc.gameRenderer.render(DeltaTracker.ZERO, true);
        target.unbindWrite();
    }

    // the game window, drawn right then, cutout keeps it see through where nothing was drawn & crops it
    private static void screenshot(Path file, boolean cutout, @Nullable Framed framed) {
        drawFrame();
        try (NativeImage screenshot = cutout ? WikiRenderer.readFrame() : Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            if (cutout) {
                WikiRenderer.saveCutout(file, screenshot, framed == null ? null : framed.crop(screenshot.getWidth(), screenshot.getHeight()));
            } else {
                Files.createDirectories(file.getParent());
                screenshot.writeToFile(file);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        saved(file);
    }

    // where a menu's mouse & crop are measured from, the menu's top left, or the window's for a screen that isn't a menu
    private static int[] origin(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> container ? new int[] { container.getGuiLeft(), container.getGuiTop() } : new int[] { 0, 0 };
    }

    // turns emi's item lists & buttons off or back on, when it's there
    private static void emi(boolean enabled) {
        try {
            Class.forName("dev.emi.emi.config.EmiConfig").getField("enabled").setBoolean(null, enabled);
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't toggle emi", e);
        }
    }

    // the whole window, or just {x, y, width, height} of it in pixels
    private static void screenshot(Path file, int @Nullable [] crop) {
        drawFrame();
        try (NativeImage screenshot = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            Files.createDirectories(file.getParent());
            if (crop == null) {
                screenshot.writeToFile(file);
            } else {
                int x = Math.max(crop[0], 0), y = Math.max(crop[1], 0);
                int width = Math.min(crop[0] + crop[2], screenshot.getWidth()) - x, height = Math.min(crop[1] + crop[3], screenshot.getHeight()) - y;
                try (NativeImage part = new NativeImage(width, height, false)) {
                    screenshot.copyRect(part, x, y, 0, 0, width, height, false, false);
                    part.writeToFile(file);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        saved(file);
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
        // dropped items, item frames, & anything else a scene added, but not players
        level.getEntitiesOfClass(Entity.class, new AABB(sceneOrigin).inflate(SCENE_RADIUS, SCENE_HEIGHT, SCENE_RADIUS), entity -> !(entity instanceof Player)).forEach(Entity::discard);
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

    // the scene's frame box, when it has one
    private static @Nullable AABB frame(Scene scene, BlockPos pos) {
        double[] frame = scene.frame();
        if (frame.length == 0) return null;
        if (frame.length != 6) throw new IllegalStateException("A scene's frame has to be {x1, y1, z1, x2, y2, z2}");
        return new AABB(frame[0], frame[1], frame[2], frame[3], frame[4], frame[5]).move(pos);
    }

    // a scene's frame box & the camera looking at it
    private record Framed(AABB box, Vec3 eye, float yaw, float pitch) {
        // where the box's corners land in a screenshot, as {x, y, width, height} with a little room around it
        private int[] crop(int width, int height) {
            Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
            // from the yaw alone, so looking straight down still has a right
            Vec3 right = Vec3.directionFromRotation(0, yaw + 90);
            Vec3 up = right.cross(forward);
            double tan = Math.tan(Math.toRadians(FOV / 2D)), aspect = (double) width / height;
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (int i = 0; i < 8; i++) {
                Vec3 corner = new Vec3((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
                Vec3 to = corner.subtract(eye);
                double depth = to.dot(forward);
                double x = (to.dot(right) / depth / (tan * aspect) + 1) / 2 * width, y = (1 - to.dot(up) / depth / tan) / 2 * height;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
            double margin = height * 0.02;
            return new int[] { (int) Math.floor(minX - margin), (int) Math.floor(minY - margin), (int) Math.ceil(maxX - minX + margin * 2), (int) Math.ceil(maxY - minY + margin * 2) };
        }
    }

    // far enough back along the view that the scene's bounding sphere takes up fill of the view's height
    private static Vec3 camera(AABB bounds, double fill, float yaw, float pitch) {
        double radius = Math.sqrt(bounds.getXsize() * bounds.getXsize() + bounds.getYsize() * bounds.getYsize() + bounds.getZsize() * bounds.getZsize()) / 2;
        double distance = radius / Math.sin(Math.toRadians(FOV / 2D * fill));
        return bounds.getCenter().subtract(Vec3.directionFromRotation(pitch, yaw).scale(distance));
    }

    // straight to the new weather, rather than easing into it like the game normally does
    private static void setTimeAndWeather(ServerPlayer player, long time, Scene.Weather weather) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(time);
        boolean raining = weather != Scene.Weather.CLEAR;
        level.setWeatherParameters(raining ? 0 : 1_000_000, raining ? 1_000_000 : 0, raining, weather == Scene.Weather.THUNDER);
        level.setRainLevel(weather.rain);
        level.setThunderLevel(weather.thunder);
        // the level only tells players when the levels change, so it's done here instead
        player.connection.send(new ClientboundGameEventPacket(raining ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0));
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rain));
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, weather.thunder));
        player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(), level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
    }
}
