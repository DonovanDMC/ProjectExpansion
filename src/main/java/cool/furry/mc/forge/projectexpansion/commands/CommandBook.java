package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
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
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
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
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

// TODO: consolidate player/hand & add autocomplete to location
public class CommandBook {
    public static LiteralArgumentBuilder<CommandSource> getArguments() {
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
            if(player == null) throw new NullPointerException("Player is null");
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
                throw new SimpleCommandExceptionType(Lang.Commands.BOOK_INVALID_HAND_ITEM.translate()).create();
            }

            return stack;
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
                    PlayerEntity player = ((ItemAlchemicalBook) stack.getItem()).getPlayer(target.itemStackOrException());
                    ITextComponent playerDisplay = player == null ? new StringTextComponent(target.itemStackOrException().getOrCreateTag().getString(NBTNames.OWNER_NAME)).withStyle(TextFormatting.DARK_AQUA) : player.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA);
                    ITextComponent command = new StringTextComponent(String.format("/%s book %s player %s", CommandRegistry.COMMAND_BASE, commandSource, playerDisplay.getString())).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("/%s book %s player %s", CommandRegistry.COMMAND_BASE, commandSource, playerDisplay.getString()))).withColor(TextFormatting.RED));
                    Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_BOUND_TO_PLAYER.extendColored(commandSource, TextFormatting.RED, playerDisplay, command));
                }
            }
        } catch (IllegalStateException e) {
            Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_FAILED_TO_GET_CAPABILITY.translateColored(TextFormatting.RED));
            Main.Logger.error("Failed to get capability:");
            Main.Logger.error(e);
            return null;
        }
        return provider;
    }

    private static @Nullable ServerPlayerEntity getPlayer(CommandContext<CommandSource> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ignore) {
            return null;
        }
    }

    private static ITextComponent getSourceName(CommandSource source) {
        try {
            return source.getPlayerOrException().getDisplayName();
        } catch (CommandSyntaxException e) {
            return Lang.Commands.CONSOLE.translateColored(TextFormatting.RED);
        }
    }

    private static int handleDump(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "dump");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_EMPTY.translateColored(TextFormatting.GREEN), false);
            return 0;
        }

        String content = provider.serializeNBT().toString();
        ctx.getSource().sendSuccess(new StringTextComponent(content).withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Lang.Commands.BOOK_CLICK_TO_COPY.translateColored(TextFormatting.AQUA)))).withStyle(TextFormatting.GRAY), false);
        return 1;
    }

    private static Style suggestTeleportPos(CommandContext<CommandSource> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(getPlayer(ctx)).level.dimension().equals(location.dimension());

        if(isSameDimension) {
            return Util.suggestCommand(style, String.format("/tp %s %s %s", location.x(), location.y(), location.z())).withUnderlined(true);
        } else {
            return Util.suggestCommand(style, String.format("/execute in %s run tp %s %s %s", location.dimension().location(), location.x(), location.y(), location.z())).withUnderlined(true);
        }
    }

    private static Style suggestTeleportDimension(CommandContext<CommandSource> ctx, Style style, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean isSameDimension = Objects.requireNonNull(getPlayer(ctx)).level.dimension().equals(location.dimension());

        if(!isSameDimension) {
            return Util.suggestCommand(style, String.format("/execute in %s run tp ~ ~ ~", location.dimension().location())).withUnderlined(true);
        }
        return style;
    }

    private static ITextComponent formatLocation(CommandContext<CommandSource> ctx, CapabilityAlchemicalBookLocations.TeleportLocation location) {
        boolean shouldSuggestCommand = getPlayer(ctx) != null;

        ITextComponent pos = new StringTextComponent(String.format("%s %s %s", location.x(), location.y(), location.z())).withStyle(style -> shouldSuggestCommand ? suggestTeleportPos(ctx, style, location) : style).withStyle(TextFormatting.DARK_AQUA);
        ITextComponent dimension = new StringTextComponent(location.dimension().location().toString()).withStyle(style -> shouldSuggestCommand ? suggestTeleportDimension(ctx, style, location) : style).withStyle(TextFormatting.DARK_AQUA);
        return Lang.Commands.BOOK_LIST_LOCATION.translateColored(TextFormatting.AQUA, new StringTextComponent(location.name()).withStyle(TextFormatting.DARK_AQUA), pos, dimension);
    }

    private static int handleList(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "list");
        if(provider == null) {
            return 0;
        }

        if (provider.getLocations().isEmpty()) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_EMPTY.translateColored(TextFormatting.GREEN), false);
            return 0;
        }

        for(CapabilityAlchemicalBookLocations.TeleportLocation location : provider.getLocations()) {
            Util.sendSystemMessage(ctx.getSource(), formatLocation(ctx, location));
        }
        return 1;
    }

    private static int handleClear(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "clear");
        if(provider == null) {
            return 0;
        }

        List<CapabilityAlchemicalBookLocations.TeleportLocation> locations = new ArrayList<>(provider.getLocations());
        if(locations.isEmpty()) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_EMPTY.translateColored(TextFormatting.RED), false);
            return 0;
        }

        provider.resetLocations();

        if(provider.getMode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable PlayerEntity sourcePlayer = getPlayer(ctx);
            ServerPlayerEntity targetPlayer = target.playerOrException();
            if(sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_CLEAR_PLAYER_SUCCESS_SELF.translateColored(TextFormatting.GREEN));
                return 1;
            }

            if(Config.notifyCommandChanges.get()) {
                Util.sendSystemMessage(target.playerOrException(), Lang.Commands.BOOK_CLEAR_PLAYER_NOTIFICATION.translate(getSourceName(ctx.getSource())));
            }

            Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_CLEAR_PLAYER_SUCCESS.translateColored(TextFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)));
            return 1;
        }

        Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_CLEAR_ITEMSTACK_SUCCESS.translateColored(TextFormatting.GREEN));
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
                ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_INTERNAL_LOCATION.translateColored(TextFormatting.RED), false);
                return 0;
            }
            provider.removeLocation(name);
        } catch (CapabilityAlchemicalBookLocations.BookError.NameNotFoundError ignore) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_INVALID_LOCATION.translateColored(TextFormatting.RED), false);
            return 0;
        }

        String locationDump = location.serialize().toString();
        ctx.getSource().sendSuccess(Lang.Commands.BOOK_REMOVE_BACKUP.translateColored(TextFormatting.AQUA, new StringTextComponent(locationDump).withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, locationDump)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Lang.Commands.BOOK_REMOVE_BACKUP_INFO.translateColored(TextFormatting.AQUA)))).withStyle(TextFormatting.GRAY)), false);

        if(provider.getMode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable PlayerEntity sourcePlayer = getPlayer(ctx);
            ServerPlayerEntity targetPlayer = target.playerOrException();
            if(sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_REMOVE_PLAYER_SUCCESS_SELF.translateColored(TextFormatting.GREEN));
                return 1;
            }

            if (Config.notifyCommandChanges.get()) {
                Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_REMOVE_PLAYER_NOTIFICATION.translate(name, getSourceName(ctx.getSource())), false);
            }

            Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_REMOVE_PLAYER_SUCCESS.translateColored(TextFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)));
            return 1;
        }

        Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_REMOVE_ITEMSTACK_SUCCESS.translateColored(TextFormatting.GREEN));
        return 1;
    }

    // 1.16 does not have a BlockPosArgument.getSpawnablePos method
    private static final SimpleCommandExceptionType ERROR_OUT_OF_BOUNDS = new SimpleCommandExceptionType(Lang.POS_OUTOFBOUNDS.translate());
    private static BlockPos getSpawnablePos(CommandContext<CommandSource> ctx, String name) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, name);
        if(!World.isInSpawnableBounds(pos)) {
            throw ERROR_OUT_OF_BOUNDS.create();
        }

        return pos;
    }

    private static int handleAdd(CommandContext<CommandSource> ctx, BookTarget target) throws CommandSyntaxException {
        @Nullable IAlchemicalBookLocationsProvider provider = getCapability(ctx, target, "remove");
        if(provider == null) {
            return 0;
        }

        BlockPos pos = getSpawnablePos(ctx, "pos");
        ServerWorld dimension = DimensionArgument.getDimension(ctx, "dimension");
        String name = StringArgumentType.getString(ctx, "name");

        if(CapabilityAlchemicalBookLocations.isForbiddenName(name)) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_ADD_INVALID_NAME.translateColored(TextFormatting.RED), false);
            return 0;
        }

        try {
            provider.addLocation(name, GlobalPos.of(dimension.dimension(), pos));
        } catch (CapabilityAlchemicalBookLocations.BookError.DuplicateNameError e) {
            ctx.getSource().sendSuccess(Lang.Commands.BOOK_ADD_DUPLICATE_NAME.translateColored(TextFormatting.RED), false);
            return 0;
        }

        if(provider.getMode() == ItemAlchemicalBook.Mode.PLAYER) {
            provider.syncToOtherPlayers();
            @Nullable PlayerEntity sourcePlayer = getPlayer(ctx);
            ServerPlayerEntity targetPlayer = target.playerOrException();
            if(sourcePlayer != null && sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_ADD_PLAYER_SUCCESS_SELF.translateColored(TextFormatting.GREEN));
                return 1;
            }

            if (Config.notifyCommandChanges.get()) {
                Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_ADD_PLAYER_NOTIFICATION.translate(name, getSourceName(ctx.getSource())), false);
            }

            Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_ADD_PLAYER_SUCCESS.translateColored(TextFormatting.GREEN, targetPlayer.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)));
            return 1;
        }

        Util.sendSystemMessage(ctx.getSource(), Lang.Commands.BOOK_ADD_ITEMSTACK_SUCCESS.translateColored(TextFormatting.GREEN));
        return 1;
    }
}