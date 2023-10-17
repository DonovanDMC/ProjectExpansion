package cool.furry.mc.forge.projectexpansion.commands;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.ArrayList;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Permissions {
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_MODERATORS = 1;
    public static final int LEVEL_GAMEMASTERS = 2;
    public static final int LEVEL_ADMINS = 3;
    public static final int LEVEL_OWNERS = 4;

    private static final ArrayList<String> NODES = new ArrayList<>();
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
        NODES.add(nodeName);
        return new CommandPermissionNode(nodeName, LEVEL_GAMEMASTERS);
    }

    public static final class CommandPermissionNode implements Predicate<CommandSource> {
        private final String node;
        private final int fallbackLevel;

        public CommandPermissionNode(String node, int fallbackLevel) {
            this.node = node;
            this.fallbackLevel = fallbackLevel;
        }

        @Override
        public boolean test(CommandSource source) {
            return source.source instanceof ServerPlayerEntity ? PermissionAPI.hasPermission(((ServerPlayerEntity) source.source), node) : source.hasPermission(fallbackLevel);
        }

        public String node() {
            return node;
        }

        public int fallbackLevel() {
            return fallbackLevel;
        }

    }

    public static void registerPermissionNodes() {
        NODES.forEach(node -> PermissionAPI.registerNode(node, DefaultPermissionLevel.NONE, ""));
    }
}
