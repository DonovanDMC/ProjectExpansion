package cool.furry.mc.projectexpansion.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.commands.CommandRegistry;
import cool.furry.mc.projectexpansion.commands.CommandReloadEMC;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.console;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.run;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.runOrFail;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class CommandGameTests {
    private static IKnowledgeProvider knowledge(ServerPlayer player) {
        return Objects.requireNonNull(Util.getKnowledgeProvider(player), "No knowledge provider for " + player.getScoreboardName());
    }

    private static void checkEmc(ServerPlayer player, long expected) {
        BigInteger emc = knowledge(player).getEmc();
        check(emc.equals(BigInteger.valueOf(expected)), "Expected " + player.getScoreboardName() + " to have " + expected + " EMC, they have " + emc);
    }

    // its own batch, since remapping EMC while other tests run would affect them
    @GameTest(template = EMPTY, batch = "reload_emc")
    public static void reloadEmc(GameTestHelper helper) {
        check(runOrFail(console(helper), "px reloademc") > 0, "/px reloademc didn't succeed");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void emcCommandTargetsPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        String name = player.getScoreboardName();
        CommandSourceStack console = console(helper);
        try {
            check(runOrFail(console, "px emc set " + name + " 1000") == 1, "/px emc set didn't succeed");
            checkEmc(player, 1000);
            check(runOrFail(console, "px emc add " + name + " 500") == 1, "/px emc add didn't succeed");
            checkEmc(player, 1500);
            check(runOrFail(console, "px emc remove " + name + " 200") == 1, "/px emc remove didn't succeed");
            checkEmc(player, 1300);
            check(runOrFail(console, "px emc remove " + name + " 5000") == 0, "/px emc remove took more EMC than the player had");
            checkEmc(player, 1300);
            check(runOrFail(console, "px emc test " + name + " 1300") == 1, "/px emc test failed for EMC the player has");
            check(runOrFail(console, "px emc test " + name + " 1301") == 0, "/px emc test passed for EMC the player doesn't have");
            check(runOrFail(console, "px emc get " + name) == 1, "/px emc get didn't succeed");
            check(runOrFail(console, "px emc set " + name + " nope") == 0, "/px emc set accepted an invalid number");
            checkEmc(player, 1300);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void emcCommandRunByPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, UUID.randomUUID(), Commands.LEVEL_GAMEMASTERS);
        try {
            check(runOrFail(player.createCommandSourceStack(), "px emc set @s 42") == 1, "/px emc set @s didn't succeed");
            checkEmc(player, 42);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void opCommandsNeedPermission(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        CommandSourceStack source = player.createCommandSourceStack();
        try {
            boolean ran;
            try {
                run(source, "px emc set @s 42");
                ran = true;
            } catch (CommandSyntaxException e) {
                ran = false;
            }
            check(!ran, "A player without permission could use /px emc");
            checkEmc(player, 0);
            check(runOrFail(source, "px wiki") == 1, "A player couldn't use /px wiki");
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    // the table is worked out from each fuel's EMC
    @GameTest(template = EMPTY, batch = "emc")
    public static void dumpFuelMapRuns(GameTestHelper helper) {
        check(runOrFail(console(helper), "px dumpfuelmap") == 1, "/px dumpfuelmap didn't succeed");
        helper.succeed();
    }

    // in the emc batch, as knowledge can only be learned for items with EMC
    @GameTest(template = EMPTY, batch = "emc")
    public static void knowledgeCommandTargetsPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        String name = player.getScoreboardName();
        CommandSourceStack console = console(helper);
        ItemInfo diamond = ItemInfo.fromItem(Items.DIAMOND);
        try {
            check(runOrFail(console, "px knowledge test " + name + " minecraft:diamond") == 0, "The player already knew diamonds");
            check(runOrFail(console, "px knowledge learn " + name + " minecraft:diamond") == 1, "/px knowledge learn didn't succeed");
            check(knowledge(player).hasKnowledge(diamond), "The player didn't learn diamonds");
            check(runOrFail(console, "px knowledge learn " + name + " minecraft:diamond") == 0, "/px knowledge learn succeeded for something already known");
            check(runOrFail(console, "px knowledge test " + name + " minecraft:diamond") == 1, "/px knowledge test failed for something known");
            check(runOrFail(console, "px knowledge unlearn " + name + " minecraft:diamond") == 1, "/px knowledge unlearn didn't succeed");
            check(!knowledge(player).hasKnowledge(diamond), "The player still knows diamonds");
            check(runOrFail(console, "px knowledge learn " + name + " minecraft:diamond") == 1, "/px knowledge learn didn't succeed");
            check(runOrFail(console, "px knowledge clear " + name) == 1, "/px knowledge clear didn't succeed");
            check(knowledge(player).getKnowledge().isEmpty(), "/px knowledge clear left " + knowledge(player).getKnowledge());
            check(runOrFail(console, "px knowledge clear " + name) == 0, "/px knowledge clear succeeded with nothing to clear");
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void setOwnerCommand(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        BlockPos absolute = helper.absolutePos(pos);
        ServerPlayer player = mockPlayer(helper);
        try {
            check(runOrFail(console(helper), String.format("px setowner %d %d %d %s", absolute.getX(), absolute.getY(), absolute.getZ(), player.getScoreboardName())) == 1, "/px setowner didn't succeed");
            BlockEntityPowerFlower flower = blockEntity(helper, pos, BlockEntityPowerFlower.class);
            check(flower.owner.equals(player.getUUID()), "The owner is " + flower.owner + ", not " + player.getUUID());
            check(flower.ownerName.equals(player.getScoreboardName()), "The owner's name is " + flower.ownerName + ", not " + player.getScoreboardName());
            BlockPos empty = helper.absolutePos(new BlockPos(2, 1, 1));
            check(runOrFail(console(helper), String.format("px setowner %d %d %d %s", empty.getX(), empty.getY(), empty.getZ(), player.getScoreboardName())) == 0, "/px setowner succeeded on air");
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    // reloading mid-batch would remap values other tests are reading, so this gets a batch of its own
    @BeforeBatch(batch = "emc_command")
    public static void prepareEmc(ServerLevel level) {
        Platform.prepareEmc(level.getServer());
    }

    @GameTest(template = EMPTY, batch = "emc_command")
    public static void emcCommandsReloadImmediately(GameTestHelper helper) {
        long original = Platform.emcValue(Items.DEAD_BUSH);
        CommandSourceStack console = console(helper);
        CommandReloadEMC.forceReloadAfterCommands = true;
        try {
            check(runOrFail(console, "projecte setemc 12345 minecraft:dead_bush") == 1, "/projecte setemc didn't succeed");
            check(Platform.emcValue(Items.DEAD_BUSH) == 12345, "A dead bush is worth " + Platform.emcValue(Items.DEAD_BUSH) + " right after /projecte setemc, not 12345");
        } finally {
            runOrFail(console, "projecte resetemc minecraft:dead_bush");
            CommandReloadEMC.forceReloadAfterCommands = false;
        }
        check(Platform.emcValue(Items.DEAD_BUSH) == original, "A dead bush is worth " + Platform.emcValue(Items.DEAD_BUSH) + " after /projecte resetemc, not " + original);
        helper.succeed();
    }

    // the commands a player's client is sent, built the same way the server builds them
    @SuppressWarnings("unchecked")
    private static RootCommandNode<SharedSuggestionProvider> clientTree(ServerPlayer player) {
        Commands commands = player.server.getCommands();
        RootCommandNode<SharedSuggestionProvider> root = new RootCommandNode<>();
        Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map = new HashMap<>();
        map.put(commands.getDispatcher().getRoot(), root);
        try {
            Method fill = Commands.class.getDeclaredMethod("fillUsableCommands", CommandNode.class, CommandNode.class, CommandSourceStack.class, Map.class);
            fill.setAccessible(true);
            fill.invoke(commands, commands.getDispatcher().getRoot(), root, player.createCommandSourceStack(), map);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("Couldn't build the commands sent to players: " + e);
        }
        return root;
    }

    // pex & projectexpansion used to redirect to px, which was lost from what players were sent, so they had no suggestions
    @GameTest(template = EMPTY)
    public static void commandNamesReachPlayers(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        RootCommandNode<SharedSuggestionProvider> root = clientTree(player);
        for (String name : CommandRegistry.NAMES) {
            CommandNode<SharedSuggestionProvider> node = root.getChild(name);
            check(node != null && node.getChild("dumpfuelmap") != null, "/" + name + " reaches players without its subcommands");
        }
        removePlayer(player);
        helper.succeed();
    }
}
