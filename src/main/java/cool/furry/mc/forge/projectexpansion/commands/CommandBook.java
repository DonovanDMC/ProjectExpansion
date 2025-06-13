package cool.furry.mc.neoforge.projectexpansion.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

// TODO: consolidate player/hand & add autocomplete to location
@SuppressWarnings("unused")
public class CommandBook {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("book")
                .then(Commands.literal("player").then(Arguments.addAll(Commands.argument("player", EntityArgument.player()), true)))
                .then(Arguments.addAll(Commands.literal("hand"), false));
    }

    private record Arguments(ArgumentBuilder<CommandSourceStack, ?> builder, boolean isPlayer) {
        private static ArgumentBuilder<CommandSourceStack, ?> addAll(ArgumentBuilder<CommandSourceStack, ?> builder, boolean isPlayer) {
            Arguments arguments = new Arguments(builder, isPlayer);
            arguments.add();
            arguments.clear();
            arguments.dump();
            arguments.list();
            arguments.reindex();
            arguments.remove();

            return builder;
        }

        private void add() {
            builder.then(
                Commands.literal("add")
                    .requires(isPlayer ? Permissions.BOOK_ADD_PLAYER : Permissions.BOOK_ADD_HAND)
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> handleAdd(ctx, new BookTarget(ctx)))
                            )
                        )
                    )
            );
        }

        private void clear() {
            builder.then(
                Commands.literal("clear")
                    .requires(isPlayer ? Permissions.BOOK_CLEAR_PLAYER : Permissions.BOOK_CLEAR_HAND)
                    .executes((ctx) -> handleClear(ctx, new BookTarget(ctx)))
            );
        }

        private void dump() {
            builder.then(
                Commands.literal("dump")
                    .requires(isPlayer ? Permissions.BOOK_DUMP_PLAYER : Permissions.BOOK_DUMP_HAND)
                        .executes((ctx) -> handleDump(ctx, new BookTarget(ctx)))
            );
        }

        private void list() {
            builder.then(
                Commands.literal("list")
                    .requires(isPlayer ? Permissions.BOOK_LIST_PLAYER : Permissions.BOOK_LIST_HAND)
                    .executes((ctx) -> handleList(ctx, new BookTarget(ctx)))
            );
        }

        private void reindex() {
            builder.then(
                Commands.literal("reindex")
                    .requires(isPlayer ? Permissions.BOOK_REINDEX_PLAYER : Permissions.BOOK_REINDEX_HAND)
                    .executes((ctx) -> handleReindex(ctx, new BookTarget(ctx)))
            );
        }

        private void remove() {
            builder.then(
                Commands.literal("remove")
                    .requires(isPlayer ? Permissions.BOOK_REMOVE_PLAYER : Permissions.BOOK_REMOVE_HAND)
                    .then(Commands.argument("location", StringArgumentType.string())
                        .executes(ctx -> handleRemove(ctx, new BookTarget(ctx)))
                    )
            );
        }
    }

    private static @Nullable IAlchemicalBookLocationsProvider getCapability(CommandContext<CommandSourceStack> ctx, BookTarget target, String commandSource) throws CommandSyntaxException {
        IAlchemicalBookLocationsProvider provider;
        try {
            if (target.isPlayer()) {
                provider = CapabilityAlchemicalBookLocations.fromPlayer(target.playerOrException());
            } else {
                ItemStack stack = target.itemStackOrException();
                provider = CapabilityAlchemicalBookLocations.fromItemStack(stack);
                if(stack.getItem() instanceof ItemAlchemicalBook book && book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
                    Player player = book.getPlayer(target.itemStackOrException());
                    Component playerDisplay = player == null ? Component.literal(Objects.requireNonNull(target.itemStackOrException().get(DataComponentTypes.OWNER)).name()).withStyle(ChatFormatting.DARK_AQUA) : player.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA);
                    String commandString = String.format("/%s book player %s %s", CommandRegistry.COMMAND_BASE, playerDisplay.getString(), commandSource);
                    Component command = Component.literal(commandString).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandString)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(commandString))).withColor(ChatFormatting.RED).withUnderlined(true));
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

    private static void sendSuccess(CommandSourceStack source, Component message, boolean notify) {
        source.sendSuccess(() -> message, notify);
    }

    private static Style suggestTeleportPos(CommandContext<CommandSourceStack> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(ctx.getSource().getPlayer()).level().dimension().equals(location.dimension());

        if(isSameDimension) {
            return Util.suggestCommand(style, String.format("/tp %s %s %s", location.x(), location.y(), location.z())).withUnderlined(true);
        } else {
            return Util.suggestCommand(style, String.format("/execute in %s run tp %s %s %s", location.dimension().location(), location.x(), location.y(), location.z())).withUnderlined(true);
        }
    }

    private static Style suggestTeleportDimension(CommandContext<CommandSourceStack> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(ctx.getSource().getPlayer()).level().dimension().equals(location.dimension());

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

    private static int handleAdd(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "remove");
        if(provider == null) {
            return 0;
        }

        BlockPos pos = BlockPosArgument.getSpawnablePos(ctx, "pos");
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        String name = StringArgumentType.getString(ctx, "name");

        if(CapabilityAlchemicalBookLocations.isForbiddenName(name)) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_ADD_INVALID_NAME.translateColored(ChatFormatting.RED), false);
            return 0;
        }

        try {
            provider.addLocation(name, GlobalPos.of(dimension.dimension(), pos));
        } catch (CapabilityAlchemicalBookLocations.BookError.DuplicateNameError e) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_ADD_DUPLICATE_NAME.translateColored(ChatFormatting.RED), false);
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

            if (Config.server.notifyCommandChanges.get()) {
                target.playerOrException().sendSystemMessage(Lang.Commands.BOOK_ADD_PLAYER_NOTIFICATION.translateColored(ChatFormatting.GREEN, name, getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_ADD_PLAYER_SUCCESS.translateColored(ChatFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
            return 1;
        }

        ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_ADD_ITEMSTACK_SUCCESS.translateColored(ChatFormatting.GREEN));
        return 1;
    }

    private static int handleClear(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "clear");
        if(provider == null) {
            return 0;
        }

        List<CapabilityAlchemicalBookLocations.TeleportLocation> locations = provider.getLocations().stream().toList();
        if(locations.isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(ChatFormatting.RED), false);
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

            if(Config.server.notifyCommandChanges.get()) {
                target.playerOrException().sendSystemMessage(Lang.Commands.BOOK_CLEAR_PLAYER_NOTIFICATION.translate(getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_CLEAR_PLAYER_SUCCESS.translateColored(ChatFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
            return 1;
        }

        ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_CLEAR_ITEMSTACK_SUCCESS.translateColored(ChatFormatting.GREEN));
        return 1;
    }

    private static int handleDump(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "dump");
        if(provider == null) {
            return 0;
        }

        ImmutableList<CapabilityAlchemicalBookLocations.TeleportLocation> locations = provider.getLocations();
        if (locations.isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(ChatFormatting.GREEN), false);
            return 0;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (CapabilityAlchemicalBookLocations.TeleportLocation location : locations) {
            builder.append(location.serialize());
            builder.append(",");
        }

        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");
        String content = builder.toString();
        sendSuccess(ctx.getSource(), Component.literal(content).withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Lang.Commands.BOOK_CLICK_TO_COPY.translateColored(ChatFormatting.AQUA)))).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int handleList(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "list");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(ChatFormatting.GREEN), false);
            return 0;
        }


        for(CapabilityAlchemicalBookLocations.TeleportLocation location : provider.getLocations()) {
            ctx.getSource().sendSystemMessage(formatLocation(ctx, location));
        }
        return 1;
    }

    private static int handleReindex(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "reindex");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(ChatFormatting.GREEN), false);
            return 0;
        }

        provider.reindex();

        if(provider.getMode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable Player sourcePlayer = ctx.getSource().getPlayer();
            ServerPlayer targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REINDEX_PLAYER_SUCCESS_SELF.translateColored(ChatFormatting.GREEN));
                return 1;
            }

            if(Config.server.notifyCommandChanges.get()) {
                target.playerOrException().sendSystemMessage(Lang.Commands.BOOK_REINDEX_PLAYER_NOTIFICATION.translate(getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REINDEX_PLAYER_SUCCESS.translateColored(ChatFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
            return 1;
        }

        ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REINDEX_ITEMSTACK_SUCCESS.translateColored(ChatFormatting.GREEN));
        return 1;
    }

    private static int handleRemove(CommandContext<CommandSourceStack> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "remove");
        if(provider == null) {
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "location");
        CapabilityAlchemicalBookLocations.TeleportLocation location;

        try {
            location = provider.getLocationOrThrow(name);
            if(location.isBack()) {
                sendSuccess(ctx.getSource(), Lang.Commands.BOOK_REMOVE_INTERNAL_LOCATION.translateColored(ChatFormatting.RED), false);
                return 0;
            }
            provider.removeLocation(name);
        } catch (CapabilityAlchemicalBookLocations.BookError.NameNotFoundError ignore) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_REMOVE_INVALID_LOCATION.translateColored(ChatFormatting.RED), false);
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

            if(Config.server.notifyCommandChanges.get()) {
                target.playerOrException().sendSystemMessage(Lang.Commands.BOOK_REMOVE_PLAYER_NOTIFICATION.translate(name, getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REMOVE_PLAYER_SUCCESS.translateColored(ChatFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
            return 1;
        }

        ctx.getSource().sendSystemMessage(Lang.Commands.BOOK_REMOVE_ITEMSTACK_SUCCESS.translateColored(ChatFormatting.GREEN));
        return 1;
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
                throw new SimpleCommandExceptionType(Lang.Commands.BOOK_INVALID_HAND_ITEM.translateColored(ChatFormatting.UNDERLINE)).create();
            }

            return stack;
        }
    }
}