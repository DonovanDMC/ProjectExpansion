package cool.furry.mc.projectexpansion.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.projectexpansion.util.Util;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkConstants;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public class GameTestUtil {
    // an empty 8x4x8 area, see resources/data/projectexpansion/structures/empty.nbt
    public static final String EMPTY = "empty";
    // NetworkConstants.FML_NETVERSION isn't public, valueOf gives back the same key
    private static final AttributeKey<String> FML_NETVERSION = AttributeKey.valueOf("fml:netversion");

    public static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    public static <T extends BlockEntity> T blockEntity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (!type.isInstance(blockEntity)) {
            throw new GameTestAssertException(String.format("Expected a %s at %s, found %s", type.getSimpleName(), pos, blockEntity));
        }
        return type.cast(blockEntity);
    }

    public static int run(CommandSourceStack source, String command) throws CommandSyntaxException {
        return source.getServer().getCommands().getDispatcher().execute(command, source.withSuppressedOutput());
    }

    // for commands that should parse, a failure to parse fails the test
    public static int runOrFail(CommandSourceStack source, String command) {
        try {
            return run(source, command);
        } catch (CommandSyntaxException e) {
            throw new GameTestAssertException("/" + command + " failed: " + e.getMessage());
        }
    }

    public static CommandSourceStack console(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack();
    }

    public static ServerPlayer mockPlayer(GameTestHelper helper) {
        return mockPlayer(helper, UUID.randomUUID(), Commands.LEVEL_ALL);
    }

    // a player in the player list like a real one, so Util.getPlayer & selectors find it, but packets sent to it go nowhere
    // it's removed when the test ends, removePlayer takes it offline before then
    public static ServerPlayer mockPlayer(GameTestHelper helper, UUID uuid, int permissionLevel) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = Platform.newPlayer(helper.getLevel(), new GameProfile(uuid, "px-" + uuid.toString().substring(0, 8)), permissionLevel);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        // forge adds its filters before packet_handler when the player joins, and needs to know it's a forge client
        new EmbeddedChannel(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.attr(FML_NETVERSION).set(NetworkConstants.NETVERSION);
                channel.pipeline().addLast("packet_handler", connection);
            }
        });
        connection.setProtocol(ConnectionProtocol.PLAY);
        server.getPlayerList().placeNewPlayer(connection, player);
        // a failed check skips the removePlayer at the end of a test, which left the player standing at spawn
        testInfo(helper).addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {}

            @Override
            public void testPassed(GameTestInfo info) {
                removePlayer(player);
            }

            @Override
            public void testFailed(GameTestInfo info) {
                removePlayer(player);
            }
        });
        return player;
    }

    // private before neoforge, game tests only run in dev so the field keeps its name
    private static GameTestInfo testInfo(GameTestHelper helper) {
        try {
            Field field = GameTestHelper.class.getDeclaredField("testInfo");
            field.setAccessible(true);
            return (GameTestInfo) field.get(helper);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't get the test from its helper", e);
        }
    }

    public static void removePlayer(ServerPlayer player) {
        // it may already be gone, or a test may have brought the same uuid back as a new player
        if (player.server.getPlayerList().getPlayer(player.getUUID()) != player) return;
        // removing them closes what they have open as if they'd died, which drops it all at spawn where they stand
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.containerMenu = player.inventoryMenu;
        player.server.getPlayerList().remove(player);
    }

    public static IKnowledgeProvider knowledge(ServerPlayer player) {
        return Objects.requireNonNull(Util.getKnowledgeProvider(player), "No knowledge provider for " + player.getScoreboardName());
    }

    public static BigInteger emc(ServerPlayer player) {
        return knowledge(player).getEmc();
    }
}
