package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemialBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

// TODO: consolidate player/hand & add autocomplete to location
public class CommandBook {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("book")
            .requires(Permissions.BOOK)
            .then(Commands.literal("add")
                .requires(Permissions.BOOK_ADD)
                .then(Commands.literal("player")
                    .requires(Permissions.BOOK_ADD_PLAYER)
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                    .executes(ctx -> handleAdd(ctx, new BookTarget(ctx)))
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("hand")
                    .requires(Permissions.BOOK_ADD_HAND)
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> handleAdd(ctx, new BookTarget(ctx)))
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("clear")
                .requires(Permissions.BOOK_CLEAR)
                .then(Commands.literal("player")
                        .requires(Permissions.BOOK_CLEAR_PLAYER)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes((ctx) -> handleClear(ctx, new BookTarget(ctx)))
                    )
                )
                .then(Commands.literal("hand")
                        .requires(Permissions.BOOK_CLEAR_HAND)
                    .executes((ctx) -> handleClear(ctx, new BookTarget(ctx)))
                )
            )
            .then(Commands.literal("dump")
                .requires(Permissions.BOOK_DUMP)
                .then(Commands.literal("player")
                    .requires(Permissions.BOOK_DUMP_PLAYER)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes((ctx) -> handleDump(ctx, new BookTarget(ctx)))
                    )
                )
                .then(Commands.literal("hand")
                    .requires(Permissions.BOOK_DUMP_HAND)
                    .executes((ctx) -> handleDump(ctx, new BookTarget(ctx)))
                )
            )
                .then(Commands.literal("list")
                    .requires(Permissions.BOOK_LIST)
                    .then(Commands.literal("player")
                        .requires(Permissions.BOOK_LIST_PLAYER)
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes((ctx) -> handleList(ctx, new BookTarget(ctx)))
                        )
                    )
                    .then(Commands.literal("hand")
                        .requires(Permissions.BOOK_LIST_HAND)
                        .executes((ctx) -> handleList(ctx, new BookTarget(ctx)))
                    )
                )
            .then(Commands.literal("remove")
                .requires(Permissions.BOOK_REMOVE)
                .then(Commands.literal("player")
                    .requires(Permissions.BOOK_REMOVE_PLAYER)
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("location", StringArgumentType.string())
                            .executes((ctx) -> handleRemove(ctx, new BookTarget(ctx)))
                        )
                    )
                )
                .then(Commands.literal("hand")
                    .requires(Permissions.BOOK_REMOVE_HAND)
                    .then(Commands.argument("location", StringArgumentType.string())
                        .executes(ctx -> handleRemove(ctx, new BookTarget(ctx)))
                    )
                )
            );
    }

    public static class BookTarget {
        private final CommandContext<CommandSourceStack> ctx;
        private final @Nullable ServerPlayer player;
        public BookTarget(CommandContext<CommandSourceStack> ctx) {
            @Nullable ServerPlayer playerArg;
            try {
                playerArg = EntityArgument.getPlayer(ctx, "player");
            } catch (CommandSyntaxException | IllegalArgumentException e) {
                playerArg = null;
            }

            this.ctx = ctx;
            this.player = playerArg;
        }

        public boolean isItemStack() {
            return player == null;
        }

        public boolean isPlayer() {
            return player != null;
        }

        public @Nullable ServerPlayer player() {
            return player;
        }

        public ServerPlayer playerOrException() {
            if(player == null) throw new NullPointerException("Player is null");
            return player;
        }

        public @Nullable ItemStack itemStack() {
            ServerPlayer executor;
            try {
                executor = ctx.getSource().getPlayerOrException();
            } catch (CommandSyntaxException e) {
                return null;
            }

            ItemStack stack = executor.getMainHandItem();
            if(stack.isEmpty() || !(stack.getItem() instanceof ItemAlchemicalBook)) {
                return null;
            }

            return stack;
        }

        public ItemStack itemStackOrException() throws CommandSyntaxException {
            ServerPlayer executor;
            try {
                executor = ctx.getSource().getPlayerOrException();
            } catch (CommandSyntaxException e) {
                throw new SimpleCommandExceptionType(Lang.Commands.PLAYER_ONLY.translate()).create();
            }

            ItemStack stack = executor.getMainHandItem();
            if(stack.isEmpty() || !(stack.getItem() instanceof ItemAlchemicalBook)) {
                throw new SimpleCommandExceptionType(Lang.Commands.BOOK_INVALID_HAND_ITEM.translate()).create();
            }

            return stack;
        }
    }

    private static @Nullable IAlchemialBookLocationsProvider getCapability(CommandContext<CommandSourceStack> ctx, BookTarget target, String commandSource) throws CommandSyntaxException {
        IAlchemialBookLocationsProvider provider;
        try {
            if (target.isPlayer()) {
                provider = CapabilityAlchemicalBookLocations.fromPlayer(target.playerOrException());
            } else {
                ItemStack stack = target.itemStackOrException();
                provider = CapabilityAlchemicalBookLocations.fromItemStack(stack);
                if(stack.getItem() instanceof ItemAlchemicalBook book && book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
                    Player player = book.getPlayer(target.itemStackOrException());
                    Component playerDisplay = player == null ? Component.literal(target.itemStackOrException().getOrCreateTag().getString(TagNames.OWNER_NAME)).withStyle(ChatFormatting.DARK_AQUA) : player.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA);
                    Component command = Component.literal(String.format("/%s book %s player %s", CommandRegistry.COMMAND_BASE, commandSource, playerDisplay.getString())).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("/%s book %s player %s", CommandRegistry.COMMAND_BASE, commandSource, playerDisplay.getString()))).withColor(ChatFormatting.RED));
                    ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_BOUND_TO_PLAYER.extendColored(commandSource, ChatFormatting.RED, playerDisplay, command));
                }
            }
        } catch (IllegalStateException e) {
            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_FAILED_TO_GET_CAPABILITY.translateColored(ChatFormatting.RED));
            Main.Logger.error("Failed to get capability:");
            Main.Logger.error(e);
            return null;
        }
        return provider;
    }

    private static Component getSourceName(CommandSourceStack source) {
        try {
            return source.getPlayerOrException().getDisplayName();
        } catch (CommandSyntaxException e) {
            return Lang.Commands.CONSOLE.translateColored(ChatFormatting.RED);
        }
    }

    private static int handleDump(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemialBookLocationsProvider provider = getCapability(ctx, target, "dump");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_EMPTY.translateColored(ChatFormatting.GREEN), false);
            return 0;
        }

        String content = provider.serializeNBT().toString();
        ctx.getSource().sendSuccess(Component.literal(content).withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Lang.Commands.BOOK_CLICK_TO_COPY.translateColored(ChatFormatting.AQUA)))).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static Style suggestTeleportPos(CommandContext<CommandSourceStack> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(ctx.getSource().getPlayer()).level.dimension().equals(location.dimension());

        if(isSameDimension) {
            return Util.suggestCommand(style, String.format("/tp %s %s %s", location.x(), location.y(), location.z())).withUnderlined(true);
        } else {
            return Util.suggestCommand(style, String.format("/execute in %s run tp %s %s %s", location.dimension().location(), location.x(), location.y(), location.z())).withUnderlined(true);
        }
    }

    private static Style suggestTeleportDimension(CommandContext<CommandSourceStack> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(ctx.getSource().getPlayer()).level.dimension().equals(location.dimension());

        if(!isSameDimension) {
            return Util.suggestCommand(style, String.format("/execute in %s run tp ~ ~ ~", location.dimension().location())).withUnderlined(true);
        }
        return style;
    }

    private static Component formatLocation(CommandContext<CommandSourceStack> ctx, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean shouldSuggestCommand = ctx.getSource().getPlayer() != null;

        Component pos = Component.literal(String.format("%s %s %s", location.x(), location.y(), location.z())).withStyle(style -> shouldSuggestCommand ? suggestTeleportPos(ctx, style, location) : style).withStyle(ChatFormatting.DARK_AQUA);
        Component dimension = Component.literal(location.dimension().location().toString()).withStyle(style -> shouldSuggestCommand ? suggestTeleportDimension(ctx, style, location) : style).withStyle(ChatFormatting.DARK_AQUA);
        return Lang.Commands.BOOK_LIST_LOCATION.translateColored(ChatFormatting.AQUA, Component.literal(location.name()).withStyle(ChatFormatting.DARK_AQUA), pos, dimension);
    }

    private static int handleList(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemialBookLocationsProvider provider = getCapability(ctx, target, "list");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_EMPTY.translateColored(ChatFormatting.GREEN), false);
            return 0;
        }


        for(CapabilityAlchemicalBookLocations.TeleportLocation location : provider.getLocations()) {
            ctx.getSource().sendSystemMessage(formatLocation(ctx, location));
        }
        return 1;
    }

    private static int handleClear(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemialBookLocationsProvider provider = getCapability(ctx, target, "clear");
        if(provider == null) {
            return 0;
        }

        List<CapabilityAlchemicalBookLocations.TeleportLocation> locations = provider.getLocations().stream().toList();
        if(locations.isEmpty()) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_EMPTY.translateColored(ChatFormatting.RED), false);
            return 0;
        }

        provider.resetLocations();

        if(provider.getMode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable Player sourcePlayer = ctx.getSource().getPlayer();
            ServerPlayer targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_CLEAR_PLAYER_SUCCESS_SELF.translateColored(ChatFormatting.GREEN));
                return 1;
            }

            if(Config.notifyCommandChanges.get()) {
                target.playerOrException().sendSystemMessage(Lang.Commands.BOOK_CLEAR_PLAYER_NOTIFICATION.translate(getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_CLEAR_PLAYER_SUCCESS.translateColored(ChatFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
            return 1;
        }

        ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_CLEAR_ITEMSTACK_SUCCESS.translateColored(ChatFormatting.GREEN));
        return 1;
    }

    private static int handleRemove(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemialBookLocationsProvider provider = getCapability(ctx, target, "remove");
        if(provider == null) {
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "location");
        CapabilityAlchemicalBookLocations.TeleportLocation location;

        try {
            location = provider.getLocationOrThrow(name);
            if(location.isBack()) {
                ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_INTERNAL_LOCATION.translateColored(ChatFormatting.RED), false);
                return 0;
            }
            provider.removeLocation(name);
        } catch (CapabilityAlchemicalBookLocations.BookError.NameNotFoundError ignore) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_INVALID_LOCATION.translateColored(ChatFormatting.RED), false);
            return 0;
        }

        ctx.getSource().sendSystemMessage(formatLocation(ctx, location));

        if(provider.getMode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable Player sourcePlayer = ctx.getSource().getPlayer();
            ServerPlayer targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REMOVE_PLAYER_SUCCESS_SELF.translateColored(ChatFormatting.GREEN));
                return 1;
            }

            if(Config.notifyCommandChanges.get()) {
                target.playerOrException().sendSystemMessage(Lang.Commands.BOOK_REMOVE_PLAYER_NOTIFICATION.translate(name, getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REMOVE_PLAYER_SUCCESS.translateColored(ChatFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
            return 1;
        }

        ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REMOVE_ITEMSTACK_SUCCESS.translateColored(ChatFormatting.GREEN));
        return 1;
    }

    private static int handleAdd(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemialBookLocationsProvider provider = getCapability(ctx, target, "remove");
        if(provider == null) {
            return 0;
        }

        BlockPos pos = BlockPosArgument.getSpawnablePos(ctx, "pos");
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        String name = StringArgumentType.getString(ctx, "name");

        if(CapabilityAlchemicalBookLocations.isForbiddenName(name)) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_ADD_INVALID_NAME.translateColored(ChatFormatting.RED), false);
            return 0;
        }

        try {
            provider.addLocation(name, GlobalPos.of(dimension.dimension(), pos));
        } catch (CapabilityAlchemicalBookLocations.BookError.DuplicateNameError e) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_ADD_DUPLICATE_NAME.translateColored(ChatFormatting.RED), false);
            return 0;
        }

        if(provider.getMode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable Player sourcePlayer = ctx.getSource().getPlayer();
            ServerPlayer targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_ADD_PLAYER_SUCCESS_SELF.translateColored(ChatFormatting.GREEN));
                return 1;
            }

            if (Config.notifyCommandChanges.get()) {
                target.playerOrException().sendSystemMessage(Lang.Commands.BOOK_ADD_PLAYER_NOTIFICATION.translateColored(ChatFormatting.GREEN, name, getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_ADD_PLAYER_SUCCESS.translateColored(ChatFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
            return 1;
        }

        ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_ADD_ITEMSTACK_SUCCESS.translateColored(ChatFormatting.GREEN));
        return 1;
    }
}