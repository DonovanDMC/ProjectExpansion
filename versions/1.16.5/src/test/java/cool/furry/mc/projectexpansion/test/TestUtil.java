package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.projectexpansion.util.Util;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.FMLNetworkConstants;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TestUtil {
    // FMLNetworkConstants.FML_NETVERSION isn't public, valueOf gives back the same key
    private static final AttributeKey<String> FML_NETVERSION = AttributeKey.valueOf("fml:netversion");

    public static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void setBlock(IntegrationTestHelper helper, BlockPos pos, Block block) {
        helper.setBlockState(pos, block.defaultBlockState());
    }

    // mcjunitlib's relativePos turns a position in the test into one in the world, despite the name
    public static BlockPos absolute(IntegrationTestHelper helper, BlockPos pos) {
        return helper.relativePos(pos).orElseThrow(() -> new AssertionError(pos + " is outside the test"));
    }

    public static <T extends TileEntity> T blockEntity(IntegrationTestHelper helper, BlockPos pos, Class<T> type) {
        TileEntity blockEntity = helper.getTileEntity(pos);
        if (!type.isInstance(blockEntity)) throw new AssertionError(String.format("Expected a %s at %s, found %s", type.getSimpleName(), pos, blockEntity));
        return type.cast(blockEntity);
    }

    // passes once the check stops throwing, which is retried until the test times out
    public static void waitUntil(IntegrationTestHelper helper, Runnable check) {
        helper.assertThat(() -> {
            check.run();
            return null;
        });
    }

    // like waitUntil, but each step is only tried once the one before it has passed, so a step can also just do something
    public static void steps(IntegrationTestHelper helper, Runnable... steps) {
        AtomicInteger next = new AtomicInteger();
        waitUntil(helper, () -> {
            while (next.get() < steps.length) {
                steps[next.get()].run();
                next.incrementAndGet();
            }
        });
    }

    public static long emcValue(Item item) {
        return ProjectEAPI.getEMCProxy().getValue(item);
    }

    public static CommandSource console(IntegrationTestHelper helper) {
        return helper.getWorld().getServer().createCommandSourceStack();
    }

    public static int run(CommandSource source, String command) throws CommandSyntaxException {
        return source.getServer().getCommands().getDispatcher().execute(command, source.withSuppressedOutput());
    }

    // for commands that should parse, a failure to parse fails the test
    public static int runOrFail(CommandSource source, String command) {
        try {
            return run(source, command);
        } catch (CommandSyntaxException e) {
            throw new AssertionError("/" + command + " failed: " + e.getMessage());
        }
    }

    public static ServerPlayerEntity mockPlayer(IntegrationTestHelper helper) {
        return mockPlayer(helper, UUID.randomUUID(), false);
    }

    // a player in the player list like a real one, so Util.getPlayer finds it, but packets sent to it go nowhere
    // remove it with removePlayer when done, otherwise it stays online for the rest of the run
    // permissions come from the op list before 1.18, so an op player is added to it
    public static ServerPlayerEntity mockPlayer(IntegrationTestHelper helper, UUID uuid, boolean op) {
        ServerWorld level = helper.getWorld();
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(uuid, "px-" + uuid.toString().substring(0, 8));
        if (op) server.getPlayerList().op(profile);
        ServerPlayerEntity player = new ServerPlayerEntity(server, level, profile, new PlayerInteractionManager(level));
        NetworkManager connection = new NetworkManager(PacketDirection.SERVERBOUND);
        // forge adds its filters before packet_handler when the player joins, and needs to know it's a forge client
        new EmbeddedChannel(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.attr(FML_NETVERSION).set(FMLNetworkConstants.NETVERSION);
                channel.pipeline().addLast("packet_handler", connection);
            }
        });
        connection.setProtocol(ProtocolType.PLAY);
        server.getPlayerList().placeNewPlayer(connection, player);
        return player;
    }

    public static void removePlayer(ServerPlayerEntity player) {
        player.server.getPlayerList().deop(player.getGameProfile());
        player.server.getPlayerList().remove(player);
    }

    public static IKnowledgeProvider knowledge(ServerPlayerEntity player) {
        return Objects.requireNonNull(Util.getKnowledgeProvider(player), "No knowledge provider for " + player.getScoreboardName());
    }

    public static BigInteger emc(ServerPlayerEntity player) {
        return knowledge(player).getEmc();
    }
}
