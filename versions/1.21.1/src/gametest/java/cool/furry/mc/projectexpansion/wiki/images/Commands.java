package cool.furry.mc.projectexpansion.wiki.images;

import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.wiki.Command;
import cool.furry.mc.projectexpansion.wiki.WikiImage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.wiki.WikiBlocks.*;
import static cool.furry.mc.projectexpansion.wiki.WikiPlayer.*;

// see WikiImage, listed in WikiImages.DEFINITIONS
public final class Commands {
    private Commands() {}

    /***************
     * Book Command *
     ***************/

    @WikiImage("Management-Commands/book-add.png")
    @Command
    public static String bookAdd(ServerPlayer player) {
        runCommand(player, book(player, "clear"));
        return book(player, "add ~ ~ ~ minecraft:overworld test");
    }

    @WikiImage("Management-Commands/book-clear.png")
    @Command
    public static String bookClear(ServerPlayer player) {
        withLocation(player);
        return book(player, "clear");
    }

    @WikiImage("Management-Commands/book-dump.png")
    @Command
    public static String bookDump(ServerPlayer player) {
        withLocation(player);
        return book(player, "dump");
    }

    @WikiImage("Management-Commands/book-list.png")
    @Command
    public static String bookList(ServerPlayer player) {
        withLocation(player);
        return book(player, "list");
    }

    // what the player's told when the console adds a location for them
    @WikiImage("Management-Commands/book-notification.png")
    @Command(console = true, typed = false)
    public static String bookNotification(ServerPlayer player) {
        runCommand(player, book(player, "clear"));
        return book(player, "add 0 64 0 minecraft:overworld test");
    }

    @WikiImage("Management-Commands/book-reindex.png")
    @Command
    public static String bookReindex(ServerPlayer player) {
        withLocation(player);
        return book(player, "reindex");
    }

    @WikiImage("Management-Commands/book-remove.png")
    @Command
    public static String bookRemove(ServerPlayer player) {
        withLocation(player);
        return book(player, "remove test");
    }

    @WikiImage("Management-Commands/book-subcommands.png")
    @Command(suggestions = true)
    public static String bookSubcommands(ServerPlayer player) {
        return book(player, "");
    }

    /***************
     * EMC Command *
     ***************/

    @WikiImage("Management-Commands/emc-add.png")
    @Command
    public static String emcAdd(ServerPlayer player) {
        setEmc(player, 0);
        return "pex emc add " + name(player) + " 50";
    }

    @WikiImage("Management-Commands/emc-get.png")
    @Command
    public static String emcGet(ServerPlayer player) {
        setEmc(player, 50);
        return "pex emc get " + name(player);
    }

    @WikiImage("Management-Commands/emc-remove.png")
    @Command
    public static String emcRemove(ServerPlayer player) {
        setEmc(player, 50);
        return "pex emc remove " + name(player) + " 25";
    }

    @WikiImage("Management-Commands/emc-set.png")
    @Command
    public static String emcSet(ServerPlayer player) {
        return "pex emc set " + name(player) + " 75";
    }

    @WikiImage("Management-Commands/emc-subcommands.png")
    @Command(suggestions = true)
    public static String emcSubcommands(ServerPlayer player) {
        return "pex emc ";
    }

    @WikiImage("Management-Commands/emc-test-fail.png")
    @Command
    public static String emcTestFail(ServerPlayer player) {
        setEmc(player, 50);
        return "pex emc test " + name(player) + " 100";
    }

    @WikiImage("Management-Commands/emc-test-pass.png")
    @Command
    public static String emcTestPass(ServerPlayer player) {
        setEmc(player, 50);
        return "pex emc test " + name(player) + " 50";
    }

    // what the player's told when the console changes their emc
    @WikiImage("Management-Commands/change-notification.png")
    @Command(console = true, typed = false)
    public static String emcChangeNotification(ServerPlayer player) {
        return "pex emc set " + name(player) + " 20";
    }

    /*********************
     * Knowledge Command *
     *********************/

    @WikiImage("Management-Commands/knowledge-clear.png")
    @Command
    public static String knowledgeClear(ServerPlayer player) {
        learn(player, new ItemStack(Items.STONE));
        return "pex knowledge clear " + name(player);
    }

    @WikiImage("Management-Commands/knowledge-learn.png")
    @Command
    public static String knowledgeLearn(ServerPlayer player) {
        forget(player, new ItemStack(Items.STONE));
        return "pex knowledge learn " + name(player) + " minecraft:stone";
    }

    @WikiImage("Management-Commands/knowledge-subcommands.png")
    @Command(suggestions = true)
    public static String knowledgeSubcommands(ServerPlayer player) {
        return "pex knowledge ";
    }

    @WikiImage("Management-Commands/knowledge-test-fail.png")
    @Command
    public static String knowledgeTestFail(ServerPlayer player) {
        forget(player, new ItemStack(Items.OAK_PLANKS));
        return "pex knowledge test " + name(player) + " minecraft:oak_planks";
    }

    @WikiImage("Management-Commands/knowledge-test-pass.png")
    @Command
    public static String knowledgeTestPass(ServerPlayer player) {
        learn(player, new ItemStack(Items.STONE));
        return "pex knowledge test " + name(player) + " minecraft:stone";
    }

    @WikiImage("Management-Commands/knowledge-unlearn.png")
    @Command
    public static String knowledgeUnlearn(ServerPlayer player) {
        learn(player, new ItemStack(Items.STONE));
        return "pex knowledge unlearn " + name(player) + " minecraft:stone";
    }

    /******************
     * Other Commands *
     ******************/

    @WikiImage("Management-Commands/dump-fuel-map.png")
    @Command
    public static String dumpFuelMap(ServerPlayer player) {
        return "pex dumpfuelmap";
    }

    @WikiImage("Management-Commands/reload-emc.png")
    @Command
    public static String reloadEmc(ServerPlayer player) {
        return "pex reloademc";
    }

    // a power flower beside the player, which has an owner to change
    @WikiImage("Management-Commands/set-owner.png")
    @Command
    public static String setOwner(ServerPlayer player) {
        BlockPos pos = player.blockPosition().east(2);
        clear(player.serverLevel(), pos);
        place(player.serverLevel(), pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        return String.format("pex setowner %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), name(player));
    }

    /***********
     * Helpers *
     ***********/

    private static String name(ServerPlayer player) {
        return player.getScoreboardName();
    }

    // the player's own teleport locations, which aren't on a book
    private static String book(ServerPlayer player, String subcommand) {
        return "pex book player " + name(player) + " " + subcommand;
    }

    // a location called test, and nothing else
    private static void withLocation(ServerPlayer player) {
        runCommand(player, book(player, "clear"));
        runCommand(player, book(player, "add ~ ~ ~ minecraft:overworld test"));
    }
}
