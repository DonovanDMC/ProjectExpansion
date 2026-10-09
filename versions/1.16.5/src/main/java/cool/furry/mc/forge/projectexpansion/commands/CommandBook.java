package cool.furry.mc.forge.projectexpansion.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.OwnerData;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// TODO: consolidate player/hand & add autocomplete to location
@SuppressWarnings("unused")
public class CommandBook {
    public static LiteralArgumentBuilder<CommandSource> getArguments() {
        return Commands.literal("book")
                .then(Commands.literal("player").then(Arguments.addAll(Commands.argument("player", EntityArgument.player()), true)))
                .then(Arguments.addAll(Commands.literal("hand"), false));
    }

    private static final class Arguments {
        private final ArgumentBuilder<CommandSource, ?> builder;
        private final boolean isPlayer;

        public Arguments(ArgumentBuilder<CommandSource, ?> builder, boolean isPlayer) {
            this.builder = builder;
            this.isPlayer = isPlayer;
        }

        public ArgumentBuilder<CommandSource, ?> builder() {
            return builder;
        }

        public boolean isPlayer() {
            return isPlayer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Arguments that = (Arguments) o;
            return Objects.equals(builder, that.builder) && Objects.equals(isPlayer, that.isPlayer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder, isPlayer);
        }

        @Override
        public String toString() {
            return "Arguments[builder=" + builder + ", isPlayer=" + isPlayer + "]";
        }

        private static ArgumentBuilder<CommandSource, ?> addAll(ArgumentBuilder<CommandSource, ?> builder, boolean isPlayer) {
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

    private static @Nullable IAlchemicalBookLocationsProvider getCapability(CommandContext<CommandSource> ctx, BookTarget target, String commandSource) throws CommandSyntaxException {
        IAlchemicalBookLocationsProvider provider;
        try {
            if (target.isPlayer()) {
                provider = CapabilityAlchemicalBookLocations.fromPlayer(target.playerOrException());
            } else {
                ItemStack stack = target.itemStackOrException();
                provider = CapabilityAlchemicalBookLocations.fromItemStack(stack);
                if(stack.getItem() instanceof ItemAlchemicalBook && ((ItemAlchemicalBook) stack.getItem()).getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
                    ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();
                    PlayerEntity player = book.getPlayer(target.itemStackOrException());
                    ITextComponent playerDisplay = player == null ? new StringTextComponent(Objects.requireNonNull(OwnerData.get(target.itemStackOrException())).name()).withStyle(TextFormatting.DARK_AQUA) : player.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA);
                    String commandString = String.format("/%s book player %s %s", CommandRegistry.COMMAND_BASE, playerDisplay.getString(), commandSource);
                    ITextComponent command = new StringTextComponent(commandString).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandString)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(commandString))).withColor(TextFormatting.RED).withUnderlined(true));
                    ctx.getSource().sendSuccess(Lang.Commands.BOOK_BOUND_TO_PLAYER.extendColored(commandSource, TextFormatting.RED, playerDisplay, command), false);
                }
            }
        } catch (IllegalStateException e) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_FAILED_TO_GET_CAPABILITY.translateColored(TextFormatting.RED), false);
            Main.Logger.error("Failed to get capability:");
            Main.Logger.error(e);
            return null;
        }
        return provider;
    }

    private static ITextComponent getSourceName(CommandSource source) {
        try {
            return source.getPlayerOrException().getDisplayName();
        } catch (CommandSyntaxException e) {
            return Lang.Commands.CONSOLE.translateColored(TextFormatting.RED);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void sendSuccess(CommandSource source, ITextComponent message, boolean notify) {
        source.sendSuccess(message, notify);
    }

    private static Style suggestTeleportPos(CommandContext<CommandSource> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(Util.getPlayer(ctx.getSource())).level.dimension().equals(location.dimension());

        if(isSameDimension) {
            return Util.suggestCommand(style, String.format("/tp %s %s %s", location.x(), location.y(), location.z())).withUnderlined(true);
        } else {
            return Util.suggestCommand(style, String.format("/execute in %s run tp %s %s %s", location.dimension().location(), location.x(), location.y(), location.z())).withUnderlined(true);
        }
    }

    private static Style suggestTeleportDimension(CommandContext<CommandSource> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(Util.getPlayer(ctx.getSource())).level.dimension().equals(location.dimension());

        if(!isSameDimension) {
            return Util.suggestCommand(style, String.format("/execute in %s run tp ~ ~ ~", location.dimension().location())).withUnderlined(true);
        }
        return style;
    }

    private static ITextComponent formatLocation(CommandContext<CommandSource> ctx, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean shouldSuggestCommand = Util.getPlayer(ctx.getSource()) != null;

        ITextComponent pos = new StringTextComponent(String.format("%s %s %s", location.x(), location.y(), location.z())).withStyle(style -> shouldSuggestCommand ? suggestTeleportPos(ctx, style, location) : style).withStyle(TextFormatting.DARK_AQUA);
        ITextComponent dimension = new StringTextComponent(location.dimension().location().toString()).withStyle(style -> shouldSuggestCommand ? suggestTeleportDimension(ctx, style, location) : style).withStyle(TextFormatting.DARK_AQUA);
        return Lang.Commands.BOOK_LIST_LOCATION.translateColored(TextFormatting.AQUA, new StringTextComponent(location.name()).withStyle(TextFormatting.DARK_AQUA), pos, dimension);
    }

    private static int handleAdd(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "remove");
        if(provider == null) {
            return 0;
        }

        BlockPos pos = BlockPosArgument.getOrLoadBlockPos(ctx, "pos");
        ServerWorld dimension = DimensionArgument.getDimension(ctx, "dimension");
        String name = StringArgumentType.getString(ctx, "name");

        if(CapabilityAlchemicalBookLocations.isForbiddenName(name)) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_ADD_INVALID_NAME.translateColored(TextFormatting.RED), false);
            return 0;
        }

        try {
            provider.addLocation(name, GlobalPos.of(dimension.dimension(), pos));
        } catch (CapabilityAlchemicalBookLocations.BookError.DuplicateNameError e) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_ADD_DUPLICATE_NAME.translateColored(TextFormatting.RED), false);
            return 0;
        }

        if(provider.mode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable PlayerEntity sourcePlayer = Util.getPlayer(ctx.getSource());
            ServerPlayerEntity targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSuccess(Lang.Commands.BOOK_ADD_PLAYER_SUCCESS_SELF.translateColored(TextFormatting.GREEN), false);
                return 1;
            }

            if (Config.server.notifyCommandChanges.get()) {
                target.playerOrException().displayClientMessage(Lang.Commands.BOOK_ADD_PLAYER_NOTIFICATION.translateColored(TextFormatting.GREEN, name, getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSuccess(Lang.Commands.BOOK_ADD_PLAYER_SUCCESS.translateColored(TextFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)), false);
            return 1;
        }

        ctx.getSource().sendSuccess(Lang.Commands.BOOK_ADD_ITEMSTACK_SUCCESS.translateColored(TextFormatting.GREEN), false);
        return 1;
    }

    private static int handleClear(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "clear");
        if(provider == null) {
            return 0;
        }

        List<CapabilityAlchemicalBookLocations.TeleportLocation> locations = provider.getLocations().stream().collect(Collectors.toList());
        if(locations.isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(TextFormatting.RED), false);
            return 0;
        }

        provider.resetLocations();

        if(provider.mode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable PlayerEntity sourcePlayer = Util.getPlayer(ctx.getSource());
            ServerPlayerEntity targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSuccess(Lang.Commands.BOOK_CLEAR_PLAYER_SUCCESS_SELF.translateColored(TextFormatting.GREEN), false);
                return 1;
            }

            if(Config.server.notifyCommandChanges.get()) {
                target.playerOrException().displayClientMessage(Lang.Commands.BOOK_CLEAR_PLAYER_NOTIFICATION.translate(getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSuccess(Lang.Commands.BOOK_CLEAR_PLAYER_SUCCESS.translateColored(TextFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)), false);
            return 1;
        }

        ctx.getSource().sendSuccess(Lang.Commands.BOOK_CLEAR_ITEMSTACK_SUCCESS.translateColored(TextFormatting.GREEN), false);
        return 1;
    }

    private static int handleDump(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "dump");
        if(provider == null) {
            return 0;
        }

        ImmutableList<CapabilityAlchemicalBookLocations.TeleportLocation> locations = provider.getLocations();
        if (locations.isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(TextFormatting.GREEN), false);
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
        sendSuccess(ctx.getSource(), new StringTextComponent(content).withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Lang.Commands.BOOK_CLICK_TO_COPY.translateColored(TextFormatting.AQUA)))).withStyle(TextFormatting.GRAY), false);
        return 1;
    }

    private static int handleList(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "list");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(TextFormatting.GREEN), false);
            return 0;
        }


        for(CapabilityAlchemicalBookLocations.TeleportLocation location : provider.getLocations()) {
            ctx.getSource().sendSuccess(formatLocation(ctx, location), false);
        }
        return 1;
    }

    private static int handleReindex(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "reindex");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_EMPTY.translateColored(TextFormatting.GREEN), false);
            return 0;
        }

        provider.reindex();

        if(provider.mode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable PlayerEntity sourcePlayer = Util.getPlayer(ctx.getSource());
            ServerPlayerEntity targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSuccess(Lang.Commands.BOOK_REINDEX_PLAYER_SUCCESS_SELF.translateColored(TextFormatting.GREEN), false);
                return 1;
            }

            if(Config.server.notifyCommandChanges.get()) {
                target.playerOrException().displayClientMessage(Lang.Commands.BOOK_REINDEX_PLAYER_NOTIFICATION.translate(getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSuccess(Lang.Commands.BOOK_REINDEX_PLAYER_SUCCESS.translateColored(TextFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)), false);
            return 1;
        }

        ctx.getSource().sendSuccess(Lang.Commands.BOOK_REINDEX_ITEMSTACK_SUCCESS.translateColored(TextFormatting.GREEN), false);
        return 1;
    }

    private static int handleRemove(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "remove");
        if(provider == null) {
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "location");
        CapabilityAlchemicalBookLocations.TeleportLocation location;

        try {
            location = provider.getLocationOrThrow(name);
            if(location.isBack()) {
                sendSuccess(ctx.getSource(), Lang.Commands.BOOK_REMOVE_INTERNAL_LOCATION.translateColored(TextFormatting.RED), false);
                return 0;
            }
            provider.removeLocation(name);
        } catch (CapabilityAlchemicalBookLocations.BookError.NameNotFoundError ignore) {
            sendSuccess(ctx.getSource(), Lang.Commands.BOOK_REMOVE_INVALID_LOCATION.translateColored(TextFormatting.RED), false);
            return 0;
        }

        ctx.getSource().sendSuccess(formatLocation(ctx, location), false);

        if(provider.mode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable PlayerEntity sourcePlayer = Util.getPlayer(ctx.getSource());
            ServerPlayerEntity targetPlayer = target.playerOrException();
            if (sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_PLAYER_SUCCESS_SELF.translateColored(TextFormatting.GREEN), false);
                return 1;
            }

            if(Config.server.notifyCommandChanges.get()) {
                target.playerOrException().displayClientMessage(Lang.Commands.BOOK_REMOVE_PLAYER_NOTIFICATION.translate(name, getSourceName(ctx.getSource())), false);
            }

            ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_PLAYER_SUCCESS.translateColored(TextFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)), false);
            return 1;
        }

        ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_ITEMSTACK_SUCCESS.translateColored(TextFormatting.GREEN), false);
        return 1;
    }

    public static class BookTarget {
        private final CommandContext<CommandSource> ctx;
        private final @Nullable ServerPlayerEntity player;
        public BookTarget(CommandContext<CommandSource> ctx) {
            @Nullable ServerPlayerEntity playerArg;
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

        public @Nullable ServerPlayerEntity player() {
            return player;
        }

        public ServerPlayerEntity playerOrException() {
            if(player == null) throw new NullPointerException("PlayerEntity is null");
            return player;
        }

        public @Nullable ItemStack itemStack() {
            ServerPlayerEntity executor;
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
            ServerPlayerEntity executor;
            try {
                executor = ctx.getSource().getPlayerOrException();
            } catch (CommandSyntaxException e) {
                throw new SimpleCommandExceptionType(Lang.Commands.PLAYER_ONLY.translate()).create();
            }

            ItemStack stack = executor.getMainHandItem();
            if(stack.isEmpty() || !(stack.getItem() instanceof ItemAlchemicalBook)) {
                throw new SimpleCommandExceptionType(Lang.Commands.BOOK_INVALID_HAND_ITEM.translateColored(TextFormatting.UNDERLINE)).create();
            }

            return stack;
        }
    }
}