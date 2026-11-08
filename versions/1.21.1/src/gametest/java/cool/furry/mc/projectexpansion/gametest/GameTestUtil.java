package cool.furry.mc.projectexpansion.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.projectexpansion.util.Util;
import io.netty.channel.embedded.EmbeddedChannel;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public class GameTestUtil {
    // an empty 8x4x8 area, see resources/data/projectexpansion/structure/empty.nbt
    public static final String EMPTY = "empty";

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
    // the same as GameTestHelper.makeMockServerPlayerInLevel, but with a set uuid & permission level, and able to receive mod packets
    public static ServerPlayer mockPlayer(GameTestHelper helper, UUID uuid, int permissionLevel) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(uuid, "px-" + uuid.toString().substring(0, 8)), false);
        // the game test server gives ops a permission level of 0, so it's overridden instead
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            protected int getPermissionLevel() {
                return permissionLevel;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // otherwise sending a mod's payload throws, as no channels were negotiated
        NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // a failed check skips the removePlayer at the end of a test, which left the player standing at spawn
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {}

            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                removePlayer(player);
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                removePlayer(player);
            }

            @Override
            public void testAddedForRerun(GameTestInfo info, GameTestInfo rerun, GameTestRunner runner) {}
        });
        return player;
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

    // what a packet's handler sees as having sent it, anything other than the player fails loudly
    public static IPayloadContext payloadContext(ServerPlayer player) {
        return (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class<?>[] { IPayloadContext.class }, (proxy, method, args) -> {
            if (method.getName().equals("player")) return player;
            throw new UnsupportedOperationException(method.getName());
        });
    }
}
