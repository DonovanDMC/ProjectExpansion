package cool.furry.mc.forge.projectexpansion.commands;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

// permission nodes are plain strings before 1.18
public class Permissions {
    private static final Map<String, DefaultPermissionLevel> NODES = new LinkedHashMap<>();
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
    public static final CommandPermissionNode BOOK_ADD_HAND = nodeOpCommand("book.add.hand");
    public static final CommandPermissionNode BOOK_ADD_PLAYER = nodeOpCommand("book.add.player");
    public static final CommandPermissionNode BOOK_CLEAR_HAND = nodeOpCommand("book.clear.hand");
    public static final CommandPermissionNode BOOK_CLEAR_PLAYER = nodeOpCommand("book.clear.player");
    public static final CommandPermissionNode BOOK_DUMP_HAND = nodeOpCommand("book.dump.hand");
    public static final CommandPermissionNode BOOK_DUMP_PLAYER = nodeOpCommand("book.dump.player");
    public static final CommandPermissionNode BOOK_LIST_HAND = nodeOpCommand("book.list.hand");
    public static final CommandPermissionNode BOOK_LIST_PLAYER = nodeOpCommand("book.list.player");
    public static final CommandPermissionNode BOOK_REINDEX_HAND = nodeOpCommand("book.reindex.hand");
    public static final CommandPermissionNode BOOK_REINDEX_PLAYER = nodeOpCommand("book.reindex.player");
    public static final CommandPermissionNode BOOK_REMOVE_HAND = nodeOpCommand("book.remove.hand");
    public static final CommandPermissionNode BOOK_REMOVE_PLAYER = nodeOpCommand("book.remove.player");
    public static final CommandPermissionNode DUMP_FUEL_MAP = nodeAllCommand("dump_fuel_map");
    public static final CommandPermissionNode WIKI = nodeAllCommand("wiki");
    public static final CommandPermissionNode SET_OWNER = nodeOpCommand("set_owner");
    public static final CommandPermissionNode RELOAD_EMC = nodeOpCommand("reload_emc");

    private static CommandPermissionNode nodeAllCommand(String nodeName) {
        return new CommandPermissionNode(node("command." + nodeName, DefaultPermissionLevel.ALL), 0);
    }

    private static CommandPermissionNode nodeOpCommand(String nodeName) {
        return new CommandPermissionNode(node("command." + nodeName, DefaultPermissionLevel.OP), 2);
    }

    private static String node(String nodeName, DefaultPermissionLevel level) {
        String node = String.format("%s.%s", Main.MOD_ID, nodeName);
        NODES.put(node, level);
        return node;
    }

    public static final class CommandPermissionNode implements Predicate<CommandSource> {
        private final String node;
        private final int fallbackLevel;

        public CommandPermissionNode(String node, int fallbackLevel) {
            this.node = node;
            this.fallbackLevel = fallbackLevel;
        }

        public String node() {
            return node;
        }

        public int fallbackLevel() {
            return fallbackLevel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommandPermissionNode that = (CommandPermissionNode) o;
            return Objects.equals(node, that.node) && fallbackLevel == that.fallbackLevel;
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, fallbackLevel);
        }

        @Override
        public String toString() {
            return "CommandPermissionNode[node=" + node + ", fallbackLevel=" + fallbackLevel + "]";
        }

        @Override
        public boolean test(CommandSource source) {
            return source.getEntity() instanceof ServerPlayerEntity ? PermissionAPI.hasPermission((ServerPlayerEntity) source.getEntity(), node) : source.hasPermission(fallbackLevel);
        }
    }

    public static void registerPermissionNodes() {
        NODES.forEach((node, level) -> PermissionAPI.registerNode(node, level, ""));
    }
}
