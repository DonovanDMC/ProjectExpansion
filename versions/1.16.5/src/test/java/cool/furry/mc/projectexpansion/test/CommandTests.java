package cool.furry.mc.projectexpansion.test;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTest;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestClass;
import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestHelper;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.commands.CommandReloadEMC;
import cool.furry.mc.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

import static cool.furry.mc.projectexpansion.test.TestUtil.absolute;
import static cool.furry.mc.projectexpansion.test.TestUtil.blockEntity;
import static cool.furry.mc.projectexpansion.test.TestUtil.check;
import static cool.furry.mc.projectexpansion.test.TestUtil.console;
import static cool.furry.mc.projectexpansion.test.TestUtil.emcValue;
import static cool.furry.mc.projectexpansion.test.TestUtil.knowledge;
import static cool.furry.mc.projectexpansion.test.TestUtil.mockPlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.removePlayer;
import static cool.furry.mc.projectexpansion.test.TestUtil.run;
import static cool.furry.mc.projectexpansion.test.TestUtil.runOrFail;
import static cool.furry.mc.projectexpansion.test.TestUtil.setBlock;

@IntegrationTestClass("empty")
public class CommandTests {
    private static void checkEmc(ServerPlayerEntity player, long expected) {
        BigInteger emc = knowledge(player).getEmc();
        check(emc.equals(BigInteger.valueOf(expected)), "Expected " + player.getScoreboardName() + " to have " + expected + " EMC, they have " + emc);
    }

    @IntegrationTest("empty")
    public void reloadEmc(IntegrationTestHelper helper) {
        check(runOrFail(console(helper), "px reloademc") > 0, "/px reloademc didn't succeed");
    }

    @IntegrationTest("empty")
    public void emcCommandTargetsPlayer(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        String name = player.getScoreboardName();
        CommandSource console = console(helper);
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
    }

    @IntegrationTest("empty")
    public void emcCommandRunByPlayer(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper, UUID.randomUUID(), true);
        try {
            check(runOrFail(player.createCommandSourceStack(), "px emc set @s 42") == 1, "/px emc set @s didn't succeed");
            checkEmc(player, 42);
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void opCommandsNeedPermission(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        CommandSource source = player.createCommandSourceStack();
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
    }

    // the table is worked out from each fuel's EMC
    @IntegrationTest("empty")
    public void dumpFuelMapRuns(IntegrationTestHelper helper) {
        check(runOrFail(console(helper), "px dumpfuelmap") == 1, "/px dumpfuelmap didn't succeed");
    }

    @IntegrationTest("empty")
    public void knowledgeCommandTargetsPlayer(IntegrationTestHelper helper) {
        ServerPlayerEntity player = mockPlayer(helper);
        String name = player.getScoreboardName();
        CommandSource console = console(helper);
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
    }

    @IntegrationTest("empty")
    public void setOwnerCommand(IntegrationTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        setBlock(helper, pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        BlockPos world = absolute(helper, pos);
        ServerPlayerEntity player = mockPlayer(helper);
        try {
            check(runOrFail(console(helper), String.format("px setowner %d %d %d %s", world.getX(), world.getY(), world.getZ(), player.getScoreboardName())) == 1, "/px setowner didn't succeed");
            BlockEntityPowerFlower flower = blockEntity(helper, pos, BlockEntityPowerFlower.class);
            check(flower.owner.equals(player.getUUID()), "The owner is " + flower.owner + ", not " + player.getUUID());
            check(flower.ownerName.equals(player.getScoreboardName()), "The owner's name is " + flower.ownerName + ", not " + player.getScoreboardName());
            BlockPos empty = absolute(helper, new BlockPos(2, 1, 1));
            check(runOrFail(console(helper), String.format("px setowner %d %d %d %s", empty.getX(), empty.getY(), empty.getZ(), player.getScoreboardName())) == 0, "/px setowner succeeded on air");
        } finally {
            removePlayer(player);
        }
    }

    @IntegrationTest("empty")
    public void emcCommandsReloadImmediately(IntegrationTestHelper helper) {
        long original = emcValue(Items.DEAD_BUSH);
        CommandSource console = console(helper);
        CommandReloadEMC.forceReloadAfterCommands = true;
        try {
            check(runOrFail(console, "projecte setemc 12345 minecraft:dead_bush") == 1, "/projecte setemc didn't succeed");
            check(emcValue(Items.DEAD_BUSH) == 12345, "A dead bush is worth " + emcValue(Items.DEAD_BUSH) + " right after /projecte setemc, not 12345");
        } finally {
            runOrFail(console, "projecte resetemc minecraft:dead_bush");
            CommandReloadEMC.forceReloadAfterCommands = false;
        }
        check(emcValue(Items.DEAD_BUSH) == original, "A dead bush is worth " + emcValue(Items.DEAD_BUSH) + " after /projecte resetemc, not " + original);
    }
}
