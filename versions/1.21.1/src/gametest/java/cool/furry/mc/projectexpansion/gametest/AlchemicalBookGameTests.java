package cool.furry.mc.projectexpansion.gametest;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketCreateTeleportLocation;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketTeleportToLocation;
import cool.furry.mc.projectexpansion.registries.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;

import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.EMPTY;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.check;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.emc;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.knowledge;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.payloadContext;
import static cool.furry.mc.projectexpansion.gametest.GameTestUtil.removePlayer;

// the alchemical book's packets, which the client sends when its gui is used
@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class AlchemicalBookGameTests {
    // teleporting is free in creative, which the game test level defaults to
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    // a basic book without an owner keeps its locations on itself
    private static ItemStack book(ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BASIC_ALCHEMICAL_BOOK.get()));
        return player.getMainHandItem();
    }

    private static IAlchemicalBookLocationsProvider locations(ItemStack book) {
        try {
            return CapabilityAlchemicalBookLocations.from(book);
        } catch (CapabilityAlchemicalBookLocations.BookError e) {
            throw new GameTestAssertException("Couldn't get the book's locations: " + e.getComponent().getString());
        }
    }

    private static void addLocation(ItemStack book, String name, ResourceKey<Level> dimension, BlockPos pos) {
        try {
            locations(book).addLocation(name, GlobalPos.of(dimension, pos));
        } catch (CapabilityAlchemicalBookLocations.BookError e) {
            throw new GameTestAssertException("Couldn't add " + name + " to the book: " + e.getComponent().getString());
        }
    }

    // the packets used to say which player they were for, so anyone could edit the book in someone else's hand
    @GameTest(template = EMPTY)
    public static void bookChangesOnlyComeFromItsHolder(GameTestHelper helper) {
        ServerPlayer holder = mockPlayer(helper), other = mockPlayer(helper);
        try {
            ItemStack book = book(holder);
            new PacketCreateTeleportLocation("spoofed", InteractionHand.MAIN_HAND).handle(payloadContext(other));
            check(locations(book).getLocation("spoofed") == null, "Someone else added a location to the book in another player's hand");
        } finally {
            removePlayer(holder);
            removePlayer(other);
        }
        helper.succeed();
    }

    // the same, but teleporting the holder & spending their emc
    @GameTest(template = EMPTY, batch = "emc")
    public static void bookTeleportsOnlyItsHolder(GameTestHelper helper) {
        ServerPlayer holder = survivalPlayer(helper), other = mockPlayer(helper);
        try {
            ItemStack book = book(holder);
            BlockPos start = holder.blockPosition();
            addLocation(book, "away", holder.level().dimension(), start.offset(20, 0, 0));
            BigInteger emc = BigInteger.valueOf(1_000_000_000L);
            knowledge(holder).setEmc(emc);
            new PacketTeleportToLocation("away", InteractionHand.MAIN_HAND).handle(payloadContext(other));
            check(holder.blockPosition().equals(start), "Someone else teleported the book's holder to " + holder.blockPosition());
            check(emc(holder).equals(emc), "Someone else spent " + emc.subtract(emc(holder)) + " of the book holder's EMC");
        } finally {
            removePlayer(holder);
            removePlayer(other);
        }
        helper.succeed();
    }

    // teleports a book can't do, like a basic one to another dimension, used to be paid for anyway
    @GameTest(template = EMPTY, batch = "emc")
    public static void bookRefusedTeleportIsFree(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        try {
            ItemStack book = book(player);
            check(!((ItemAlchemicalBook) book.getItem()).getTier().isAcrossDimensions(), "A basic book can go across dimensions, so it can't be used to test this");
            ResourceKey<Level> elsewhere = player.level().dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
            // somewhere other than right here, so it has a cost
            addLocation(book, "elsewhere", elsewhere, player.blockPosition().offset(50, 0, 0));
            BigInteger emc = BigInteger.valueOf(1_000_000_000L);
            knowledge(player).setEmc(emc);
            new PacketTeleportToLocation("elsewhere", InteractionHand.MAIN_HAND).handle(payloadContext(player));
            check(player.level().dimension() != elsewhere, "The basic book teleported to another dimension");
            check(emc(player).equals(emc), "A teleport that didn't happen cost " + emc.subtract(emc(player)) + " EMC");
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    // crossing dimensions is measured with each one's coordinate scale like a portal, plus a set amount on top
    // it used to use the raw coordinates, so it was free or expensive depending on where you stood
    @GameTest(template = EMPTY)
    public static void bookCostScalesAcrossDimensions(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        check(overworld.dimension() == Level.OVERWORLD, "The test level isn't the overworld");
        ItemStack master = new ItemStack(cool.furry.mc.projectexpansion.registries.Items.MASTER_ALCHEMICAL_BOOK.get());
        ItemStack arcane = new ItemStack(cool.furry.mc.projectexpansion.registries.Items.ARCANE_ALCHEMICAL_BOOK.get());
        float ratio = ItemAlchemicalBook.Tier.MASTER.distanceRatio();
        int surcharge = Config.server.alchemicalBookDimensionCost.get();
        CapabilityAlchemicalBookLocations.TeleportLocation nether = CapabilityAlchemicalBookLocations.TeleportLocation.from("nether", GlobalPos.of(Level.NETHER, new BlockPos(100, 64, 0)), 0);
        // 800 blocks into the overworld is 100 into the nether, so it's the same place
        int same = nether.getCost(master, overworld, new BlockPos(800, 64, 0));
        check(same == surcharge, "Going to the same place in the nether cost " + same + " instead of just the " + surcharge + " for changing dimension");
        // raw coordinates line up, but through a portal it's 700 blocks
        int far = nether.getCost(master, overworld, new BlockPos(100, 64, 0));
        int expected = (int) Math.ceil(700 * ratio + surcharge);
        check(far == expected, "Going 700 blocks into the nether cost " + far + " instead of " + expected);
        int free = nether.getCost(arcane, overworld, new BlockPos(100, 64, 0));
        check(free == 0, "The arcane book cost " + free + " to change dimension");
        helper.succeed();
    }
}
