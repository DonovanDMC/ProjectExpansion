package cool.furry.mc.neoforge.projectexpansion.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

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
    // remove it with removePlayer when done, otherwise it stays online for the rest of the run
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
        return player;
    }

    public static void removePlayer(ServerPlayer player) {
        player.server.getPlayerList().remove(player);
    }
}
