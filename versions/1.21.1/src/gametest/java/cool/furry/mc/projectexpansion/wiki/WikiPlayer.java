package cool.furry.mc.projectexpansion.wiki;

import com.mojang.authlib.GameProfile;
import cool.furry.mc.projectexpansion.util.Util;
import io.netty.channel.embedded.EmbeddedChannel;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

// a player's emc & knowledge for menus & scenes, sent to their client so the hud & tooltips match
public final class WikiPlayer {
    private WikiPlayer() {}

    public static void setEmc(ServerPlayer player, long emc) {
        setEmc(player, BigInteger.valueOf(emc));
    }

    public static void setEmc(ServerPlayer player, BigInteger emc) {
        IKnowledgeProvider knowledge = knowledge(player);
        knowledge.setEmc(emc);
        knowledge.syncEmc(player);
    }

    public static void learn(ServerPlayer player, ItemStack... stacks) {
        IKnowledgeProvider knowledge = knowledge(player);
        for (ItemStack stack : stacks) {
            ItemInfo info = ItemInfo.fromStack(stack);
            knowledge.addKnowledge(info);
            knowledge.syncKnowledgeChange(player, info, true);
        }
    }

    public static void forget(ServerPlayer player, ItemStack... stacks) {
        IKnowledgeProvider knowledge = knowledge(player);
        for (ItemStack stack : stacks) {
            ItemInfo info = ItemInfo.fromStack(stack);
            knowledge.removeKnowledge(info);
            knowledge.syncKnowledgeChange(player, info, false);
        }
    }

    // runs a command as them without anything it says, for setting things up
    public static void runCommand(ServerPlayer player, String command) {
        player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(), command);
    }

    // uses what's in their main hand on the air, like right clicking
    public static void useItem(ServerPlayer player) {
        player.gameMode.useItem(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    // another player in the player list, for things that need someone else online, like a knowledge sharing book's owner
    // packets sent to them go nowhere, and the same one's used again for the same name
    public static ServerPlayer otherPlayer(ServerLevel level, String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("wiki:" + name).getBytes(StandardCharsets.UTF_8));
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(uuid);
        if (online != null) return online;
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(uuid, name), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // otherwise sending them a mod's payload throws, as no channels were negotiated
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static IKnowledgeProvider knowledge(ServerPlayer player) {
        return Objects.requireNonNull(Util.getKnowledgeProvider(player), "No knowledge provider for " + player.getScoreboardName());
    }
}
