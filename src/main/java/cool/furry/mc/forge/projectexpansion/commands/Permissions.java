package cool.furry.mc.forge.projectexpansion.commands;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContextKey;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionType;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Permissions {
    private static final ArrayList<PermissionNode<?>> NODES = new ArrayList<>();
    private static final PermissionNode.PermissionResolver<Boolean> PLAYER_IS_OP = (player, uuid, context) -> player != null && player.hasPermissions(Commands.LEVEL_GAMEMASTERS);
    public static final CommandPermissionNode EMC = nodeOpCommand("emc");
    public static final CommandPermissionNode EMC_ADD = nodeOpCommand("emc.add");
    public static final CommandPermissionNode EMC_GET = nodeOpCommand("emc.get");
    public static final CommandPermissionNode EMC_REMOVE = nodeOpCommand("emc.remove");
    public static final CommandPermissionNode EMC_SET = nodeOpCommand("emc.set");
    public static final CommandPermissionNode EMC_TEST = nodeOpCommand("emc.test");
    public static final CommandPermissionNode KNOWLEDGE = nodeOpCommand("knowledge");
    public static final CommandPermissionNode KNOWLEDGE_CLEAR = nodeOpCommand("knowledge.clear");
    public static final CommandPermissionNode KNOWLEDGE_LEARN = nodeOpCommand("knowledge.learn");
    public static final CommandPermissionNode KNOWLEDGE_TEST = nodeOpCommand("knowledge.test");
    public static final CommandPermissionNode KNOWLEDGE_UNLEARN = nodeOpCommand("knowledge.unlearn");
    public static final CommandPermissionNode BOOK = nodeOpCommand("book");
    public static final CommandPermissionNode BOOK_ADD = nodeOpCommand("book.add");
    public static final CommandPermissionNode BOOK_ADD_HAND = nodeOpCommand("book.add.hand");
    public static final CommandPermissionNode BOOK_ADD_PLAYER = nodeOpCommand("book.add.player");
    public static final CommandPermissionNode BOOK_CLEAR = nodeOpCommand("book.clear");
    public static final CommandPermissionNode BOOK_CLEAR_HAND = nodeOpCommand("book.clear.hand");
    public static final CommandPermissionNode BOOK_CLEAR_PLAYER = nodeOpCommand("book.clear.player");
    public static final CommandPermissionNode BOOK_DUMP = nodeOpCommand("book.dump");
    public static final CommandPermissionNode BOOK_DUMP_HAND = nodeOpCommand("book.dump.hand");
    public static final CommandPermissionNode BOOK_DUMP_PLAYER = nodeOpCommand("book.dump.player");
    public static final CommandPermissionNode BOOK_LIST = nodeOpCommand("book.list");
    public static final CommandPermissionNode BOOK_LIST_HAND = nodeOpCommand("book.list.hand");
    public static final CommandPermissionNode BOOK_LIST_PLAYER = nodeOpCommand("book.list.player");
    public static final CommandPermissionNode BOOK_REMOVE = nodeOpCommand("book.remove");
    public static final CommandPermissionNode BOOK_REMOVE_HAND = nodeOpCommand("book.remove.hand");
    public static final CommandPermissionNode BOOK_REMOVE_PLAYER = nodeOpCommand("book.remove.player");

    private static CommandPermissionNode nodeOpCommand(String nodeName) {
        PermissionNode<Boolean> node = node("command." + nodeName, PermissionTypes.BOOLEAN, PLAYER_IS_OP);
        return new CommandPermissionNode(node, Commands.LEVEL_GAMEMASTERS);
    }

    @SafeVarargs
    private static <T> PermissionNode<T> node(String nodeName, PermissionType<T> type, PermissionNode.PermissionResolver<T> defaultResolver, PermissionDynamicContextKey<T>... dynamics) {
        PermissionNode<T> node = new PermissionNode<>(Main.MOD_ID, nodeName, type, defaultResolver, dynamics);
        NODES.add(node);
        return node;
    }

    public record CommandPermissionNode(PermissionNode<Boolean> node, int fallbackLevel) implements Predicate<CommandSourceStack> {

        @Override
        public boolean test(CommandSourceStack source) {
            return source.source instanceof ServerPlayer player ? PermissionAPI.getPermission(player, node) : source.hasPermission(fallbackLevel);
        }
    }

    @SubscribeEvent
    public static void registerPermissionNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES);
    }
}
