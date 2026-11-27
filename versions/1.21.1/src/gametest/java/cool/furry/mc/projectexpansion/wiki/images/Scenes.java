package cool.furry.mc.projectexpansion.wiki.images;

import com.google.common.collect.Lists;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.projectexpansion.block.BlockEMCRefinery;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.util.Fuel;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import cool.furry.mc.projectexpansion.wiki.Scene;
import cool.furry.mc.projectexpansion.wiki.WikiAe2;
import cool.furry.mc.projectexpansion.wiki.WikiImage;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cool.furry.mc.projectexpansion.wiki.WikiBlocks.*;
import static cool.furry.mc.projectexpansion.wiki.WikiPlayer.*;

// see WikiImage, listed in WikiImages.DEFINITIONS
public final class Scenes {
    private Scenes() {}

    // the condenser open & closed are framed the same, room for the open lid, so they line up side by side
    // turned towards its front, where the latch is, which faces west
    private static final float CONDENSER_YAW = -65, CONDENSER_PITCH = 35;
    // the lid's hinged at the back of the top, so open it stands up to about here & sticks out a little behind, to the east
    private static final double CONDENSER_OPEN_HEIGHT = 1.5, CONDENSER_OPEN_DEPTH = 1.3;

    /******************************
     * Advanced Alchemical Chests *
     ******************************/

    @WikiImage(value = "Advanced-Alchemical-Chest/blue-chest.png")
    @Scene(yaw = 0, pitch = 20, fill = 0.4)
    public static void blueAdvancedAlchemicalChest(ServerLevel level, BlockPos pos) {
        place(level, pos, Objects.requireNonNull(AdvancedAlchemicalChest.getBlock(DyeColor.BLUE)));
    }

    // shift right clicking a blue chest with a red bag, which turns it red and says so above the hotbar
    @WikiImage("Advanced-Alchemical-Chest/setting-color-with-bag.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.4, hud = true)
    public static Runnable advancedChestSettingColorWithBag(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, AdvancedAlchemicalChest.getBlock(DyeColor.BLUE));
        ((BlockEntityAdvancedAlchemicalChest) Objects.requireNonNull(level.getBlockEntity(pos))).setOwner(player);
        player.getInventory().setItem(0, new ItemStack(AdvancedAlchemicalChest.getBlock(DyeColor.BLUE)));
        player.getInventory().setItem(1, new ItemStack(PEItems.getBag(DyeColor.RED)));
        player.getInventory().selected = 1;
        return () -> {
            // the chest checks for crouching, which is only for the click so the camera doesn't move
            player.setPose(Pose.CROUCHING);
            click(player, pos, Direction.NORTH);
            player.setPose(Pose.STANDING);
        };
    }

    @WikiImage("Advanced-Alchemical-Chest/all-colors.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 1.2)
    public static void allAdvancedAlchemicalChestColors(ServerLevel level, BlockPos pos) {
        place(level, pos, net.minecraft.world.level.block.Blocks.STONE);
        List<BlockAdvancedAlchemicalChest> all = Arrays.stream(AdvancedAlchemicalChest.getBlocks()).sorted((a, b) -> b.getColor().getId() - a.getColor().getId()).toList();
        int middle = (int) Math.floor(all.size() / 4f);
        List<List<BlockAdvancedAlchemicalChest>> chests = Lists.partition(all, all.size() / 2);
        for (List<BlockAdvancedAlchemicalChest> set : chests) {
            BlockPos base = chests.indexOf(set) == 0 ? pos.relative(Direction.UP, 1).relative(Direction.SOUTH, 1) : pos;
            for (BlockAdvancedAlchemicalChest block : set) {
                place(level, base.offset(set.indexOf(block) - middle, 0, 0), block);
            }
        }
    }

    /*************************
     * Alchemical Collection *
     *************************/

    // breaking diamond ore with an alchemical collection pickaxe, the diamond going straight to emc instead of dropping
    // caught just after, while the block's pieces are still flying
    @WikiImage("Alchemical-Collection/mining-diamond-ore.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.4, actionTicks = 2, hud = true)
    public static Runnable alchemicalCollectionMining(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, net.minecraft.world.level.block.Blocks.DIAMOND_ORE);
        ItemStack pickaxe = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        pickaxe.enchant(level.registryAccess().holderOrThrow(cool.furry.mc.projectexpansion.registries.Enchantments.ALCHEMICAL_COLLECTION), 1);
        pickaxe.set(PEDataComponentTypes.ACTIVE.get(), true);
        player.getInventory().setItem(0, pickaxe);
        learn(player, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        return () -> {
            // nothing drops in creative, and the hud shows health & hunger like the old screenshot, but still flying so the camera stays put
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            // the break effect normally isn't sent to whoever broke it, as their client already shows it, but this player's client never broke anything
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(level.getBlockState(pos)));
            player.gameMode.destroyBlock(pos);
            // the ore drops experience too, which isn't part of what it's showing
            level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(2)).forEach(Entity::discard);
        };
    }

    /*******************************
     * Arcane Transmutation Tablet *
     *******************************/

    @WikiImage(value = "Arcane-Transmutation-Tablet/table-placed.png", cutout = true)
    @Scene
    public static void arcaneTable(ServerLevel level, BlockPos pos) {
        place(level, pos, Blocks.ARCANE_TRANSMUTATION_TABLE.get());
    }

    /**************
     * Collectors *
     **************/

    @WikiImage(value = "Collectors/all-tiers.png", cutout = true)
    @Scene
    public static void collectorsAllTiers(ServerLevel level, BlockPos pos) {
        rows(level, pos, everyTier(Matter::getCollector), 4);
    }

    /***************
     * Compact Sun *
     ***************/

    @WikiImage(value = "Compact-Sun/block.png")
    @Scene(time = 18000)
    public static void compactSunBlock(ServerLevel level, BlockPos pos) {
        place(level, pos, Objects.requireNonNull(Blocks.COMPACT_SUN.get()));
    }

    /*****************
     * Condenser MK3 *
     *****************/

    @WikiImage(value = "Condenser-MK3/block.png", cutout = true)
    @Scene(yaw = CONDENSER_YAW, pitch = CONDENSER_PITCH, frame = { 0, 0, 0, CONDENSER_OPEN_DEPTH, CONDENSER_OPEN_HEIGHT, 1 })
    public static void condenser(ServerLevel level, BlockPos pos) {
        place(level, pos, faceHorizontal(Blocks.CONDENSER_MK3.get(), Direction.WEST));
    }

    // with its lid open, like when someone's using it
    @WikiImage(value = "Condenser-MK3/block-open.png", cutout = true)
    @Scene(yaw = CONDENSER_YAW, pitch = CONDENSER_PITCH, frame = { 0, 0, 0, CONDENSER_OPEN_DEPTH, CONDENSER_OPEN_HEIGHT, 1 })
    public static Runnable condenserOpen(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, faceHorizontal(Blocks.CONDENSER_MK3.get(), Direction.WEST));
        // the event opening a menu sends, without anyone having it open, which would close it again when it's rechecked
        // sent once the client has the block, or it'd be lost
        return () -> level.blockEvent(pos, Blocks.CONDENSER_MK3.get(), 1, 1);
    }

    /*************
     * EMC Links *
     *************/

    @WikiImage(value = "EMC-Link/all-tiers.png", cutout = true)
    @Scene
    public static void emcLinkAllTiers(ServerLevel level, BlockPos pos) {
        rows(level, pos, everyTier(Matter::getEMCLink), 4);
    }

    @WikiImage(value = "EMC-Link/importing-emc.png", cutout = true)
    @Scene
    public static void emcLinkImportingEMC(ServerLevel level, BlockPos pos) {
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        List<Vec3i> surround = List.of(new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1));
        for (Vec3i offset : surround) {
            place(level, pos.offset(offset), Objects.requireNonNull(Matter.BASIC.getCollector()));
        }
    }

    @WikiImage(value = "EMC-Link/extracting-items.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static void emcLinkExtractingItems(ServerLevel level, BlockPos pos) {
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        place(level, pos.below(), hopper(Direction.WEST));
        place(level, pos.below().west(), net.minecraft.world.level.block.Blocks.CHEST);
    }

    @WikiImage(value = "EMC-Link/importing-items.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static void emcLinkImportingItems(ServerLevel level, BlockPos pos) {
        place(level, pos.below().east(), Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        place(level, pos.below(), hopper(Direction.EAST));
        place(level, pos, net.minecraft.world.level.block.Blocks.CHEST);
    }

    // setting an emc link to a water bucket, with a pipe pulling the water out into a tank
    // framed around the link so the crosshair's on it, turned a little so the pipe's end shows, and long enough after for some water to get into the tank
    @WikiImage(value = "EMC-Link/exporting-fluid.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static Runnable emcLinkExportingFluid(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        BlockEntityEMCLink link = Objects.requireNonNull(WorldHelper.getBlockEntity(BlockEntityEMCLink.class, level, pos));
        link.setOwner(player);
        link.setItem(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        place(level, pos.below(), block("pipez:fluid_pipe"));
        place(level, pos.below().west(), block("fluidtank:tank_wood"));
        extract(level, pos.below(), Direction.UP);
        // exactly enough emc for the same amount of water every time, it'd otherwise depend on how long it's been running
        setEmc(player, fluidCost(Matter.BASIC, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET), EXPORTED_FLUID));
        return () -> {};
    }

    @WikiImage("EMC-Link/not-enough-emc.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.4, hud = true)
    public static Runnable emcLinkNotEnoughEMC(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        BlockEntityEMCLink link = Objects.requireNonNull(WorldHelper.getBlockEntity(BlockEntityEMCLink.class, level, pos));
        link.setOwner(player);
        link.setItem(new ItemStack(net.minecraft.world.level.block.Blocks.STONE));
        return () -> click(player, pos, Direction.NORTH);
    }

    // taking stone out of an emc link with an empty hand, caught while it pops into the hotbar so it's clear where it went
    @WikiImage("EMC-Link/taking-export-item.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.4, hud = true, pop = 0)
    public static Runnable emcLinkTakingExport(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        BlockEntityEMCLink link = Objects.requireNonNull(WorldHelper.getBlockEntity(BlockEntityEMCLink.class, level, pos));
        link.setOwner(player);
        ItemStack stone = new ItemStack(net.minecraft.world.level.block.Blocks.STONE);
        link.setItem(stone);
        // the owner has to know it, and has exactly enough emc for 2, so the same amount comes out every time
        learn(player, stone);
        setEmc(player, IEMCProxy.INSTANCE.getValue(stone) * 2);
        return () -> click(player, pos, Direction.NORTH);
    }

    // shift right clicking an emc link with a philosopher's stone, turning its nbt filter off, which puts a yellow band around it
    @WikiImage("EMC-Link/nbt-filter-disabled.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.9, hud = true)
    public static Runnable emcLinkNbtFilterDisabled(ServerPlayer player, BlockPos pos) {
        return toggleNbtFilter(player, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()), true);
    }

    // and turning it back on
    @WikiImage("EMC-Link/nbt-filter-enabled.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.9, hud = true)
    public static Runnable emcLinkNbtFilterEnabled(ServerPlayer player, BlockPos pos) {
        return toggleNbtFilter(player, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()), false);
    }

    @WikiImage("EMC-Link/setting-export-item.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.4, hud = true)
    public static Runnable emcLinkSetExport(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        BlockEntityEMCLink link = Objects.requireNonNull(WorldHelper.getBlockEntity(BlockEntityEMCLink.class, level, pos));
        link.setOwner(player);
        player.getInventory().setItem(0, new ItemStack(net.minecraft.world.level.block.Blocks.STONE));
        return () -> click(player, pos, Direction.NORTH);
    }

    // shift right clicking an emc link with an empty hand, clearing the export
    @WikiImage("EMC-Link/clearing-export-item.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.4, hud = true)
    public static Runnable emcLinkClearExport(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getEMCLink()));
        BlockEntityEMCLink link = Objects.requireNonNull(WorldHelper.getBlockEntity(BlockEntityEMCLink.class, level, pos));
        link.setOwner(player);
        link.setItem(new ItemStack(net.minecraft.world.level.block.Blocks.STONE));
        return () -> {
            // the chest checks for crouching, which is only for the click so the camera doesn't move
            player.setPose(Pose.CROUCHING);
            click(player, pos, Direction.NORTH);
            player.setPose(Pose.STANDING);
        };
    }

    /*************
     * EMC Pipes *
     *************/

    // a collector sending emc down a line of pipes to a condenser
    @WikiImage(value = "EMC-Pipes/pipe-network.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static void pipeNetwork(ServerLevel level, BlockPos pos) {
        for (int i = 1; i <= 4; i++) place(level, pos.east(i), Blocks.EMC_PIPE.get());
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getCollector()));
        place(level, pos.east(5), Blocks.CONDENSER_MK3.get());
        BlockEmcPipe.useWrench(level, pos.east(), level.getBlockState(pos.east()), Direction.NORTH, false);
    }

    // holding a wrench, which shows the bigger hitbox each side has, looking at a pipe pulling from a collector
    @WikiImage("EMC-Pipes/extracting-side.png")
    @Scene(view = Scene.View.FRONT, pitch = 35, fill = 0.35, frame = { -1, 0, 0, 0, 1, 1 }, hud = true)
    public static Runnable emcPipeExtractingSide(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        // the pipes first, so they connect to the collector when it's placed
        for (int i = 1; i <= 3; i++) {
            place(level, pos.east(i), Blocks.EMC_PIPE.get());
            place(level, pos.west(i), Blocks.EMC_PIPE.get());
        }
        place(level, pos, Objects.requireNonNull(Matter.GREEN.getCollector()));
        // west shows on the right from the front, like the old screenshot
        BlockEmcPipe.useWrench(level, pos.west(), level.getBlockState(pos.west()), Direction.EAST, false);
        player.getInventory().setItem(0, new ItemStack(Items.WRENCH.get()));
        player.getInventory().setItem(1, new ItemStack(Blocks.EMC_PIPE.get()));
        return () -> {};
    }

    /****************
     * EMC Refinery *
     ****************/

    // lit up while refining, with a collector beside it giving it emc
    @WikiImage(value = "EMC-Refinery/block.png", cutout = true)
    @Scene
    public static void emcRefinery(ServerLevel level, BlockPos pos) {
        place(level, pos, Blocks.EMC_REFINERY.get().defaultBlockState().setValue(BlockEMCRefinery.LIT, true));
        BlockEntityEMCRefinery refinery = (BlockEntityEMCRefinery) Objects.requireNonNull(level.getBlockEntity(pos));
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get(), 64));
        place(level, pos.south(), Objects.requireNonNull(Matter.FINAL.getCollector()));
    }

    /***************
     * EMC Storage *
     ***************/

    @WikiImage(value = "EMC-Storage/all-tiers.png", cutout = true)
    @Scene
    public static void emcStorageAllTiers(ServerLevel level, BlockPos pos) {
        rows(level, pos, Arrays.stream(EmcStorageTier.VALUES).map(tier -> (Block) Blocks.EMC_STORAGE.get(tier).get()).toList(), 5);
    }

    // wrenching an emc storage's south side, which goes from input to output
    @WikiImage("EMC-Storage/side-modes.png")
    @Scene(yaw = 160, pitch = 25, fill = 0.5, hud = true)
    public static Runnable emcStorageSideModes(ServerPlayer player, BlockPos pos) {
        place(player.serverLevel(), pos, Blocks.EMC_STORAGE.get(EmcStorageTier.BASIC).get());
        player.getInventory().setItem(0, new ItemStack(Items.WRENCH.get()));
        return () -> click(player, pos, Direction.SOUTH);
    }

    /**************************
     * Knowledge Sharing Book *
     **************************/

    // shift using a knowledge sharing book, storing the player's knowledge in it
    // there's nothing to look at, so it's framed on the empty sky
    @WikiImage("Knowledge-Sharing-Book/storing-knowledge.png")
    @Scene(view = Scene.View.FRONT, pitch = -15, frame = { 0, 0, 0, 1, 1, 1 }, hud = true)
    public static Runnable knowledgeBookStoring(ServerPlayer player, BlockPos pos) {
        player.getInventory().setItem(0, new ItemStack(Items.KNOWLEDGE_SHARING_BOOK.get()));
        return () -> {
            // the book checks for crouching, which is only for the use so the camera doesn't move
            player.setPose(Pose.CROUCHING);
            useItem(player);
            player.setPose(Pose.STANDING);
        };
    }

    // using someone else's book, learning what they know that the player doesn't
    @WikiImage("Knowledge-Sharing-Book/gaining-knowledge.png")
    @Scene(view = Scene.View.FRONT, pitch = -15, frame = { 0, 0, 0, 1, 1, 1 }, hud = true)
    public static Runnable knowledgeBookGaining(ServerPlayer player, BlockPos pos) {
        ItemStack[] known = sharedKnowledge();
        player.getInventory().setItem(0, knowledgeBook(player, known));
        forget(player, known);
        return () -> useItem(player);
    }

    // and when the player already knows all of it
    @WikiImage("Knowledge-Sharing-Book/no-new-knowledge.png")
    @Scene(view = Scene.View.FRONT, pitch = -15, frame = { 0, 0, 0, 1, 1, 1 }, hud = true)
    public static Runnable knowledgeBookNoNewKnowledge(ServerPlayer player, BlockPos pos) {
        ItemStack[] known = sharedKnowledge();
        player.getInventory().setItem(0, knowledgeBook(player, known));
        learn(player, known);
        return () -> useItem(player);
    }

    /*****************
     * Matter & Fuel *
     *****************/

    // matter blocks are only in the registry, there's nothing on Matter to get them from
    @WikiImage(value = "Matter-and-Fuel/matter-blocks.png", cutout = true)
    @Scene
    public static void matterBlocks(ServerLevel level, BlockPos pos) {
        rows(level, pos, everyTier(matter -> BuiltInRegistries.BLOCK.getOptional(Main.rl(matter.name().toLowerCase(Locale.ROOT) + "_matter_block")).orElse(null)), 4);
    }

    @WikiImage(value = "Matter-and-Fuel/fuel-blocks.png", cutout = true)
    @Scene
    public static void fuelBlocks(ServerLevel level, BlockPos pos) {
        rows(level, pos, Arrays.stream(Fuel.VALUES).map(Fuel::getBlock).filter(Objects::nonNull).toList(), 4);
    }

    // matter & fuel in item frames, on barriers so there's nothing behind them
    @WikiImage(value = "Matter-and-Fuel/matter-tiers.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static void matterTiers(ServerLevel level, BlockPos pos) {
        itemFrames(level, pos, Arrays.stream(Matter.VALUES).filter(matter -> matter.hasItem).map(matter -> new ItemStack(Objects.requireNonNull(matter.getMatter()))).toList(), 4);
    }

    @WikiImage(value = "Matter-and-Fuel/fuel-tiers.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static void fuelTiers(ServerLevel level, BlockPos pos) {
        itemFrames(level, pos, Arrays.stream(Fuel.VALUES).map(Fuel::getItem).filter(Objects::nonNull).map(ItemStack::new).toList(), 4);
    }

    @WikiImage(value = "Matter-and-Fuel/projecte-matter.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static void projecteMatter(ServerLevel level, BlockPos pos) {
        itemFrames(level, pos, List.of(new ItemStack(PEItems.DARK_MATTER.asItem()), new ItemStack(PEItems.RED_MATTER.asItem())), 2);
    }

    @WikiImage(value = "Matter-and-Fuel/projecte-fuel.png", cutout = true)
    @Scene(view = Scene.View.FRONT)
    public static void projecteFuel(ServerLevel level, BlockPos pos) {
        itemFrames(level, pos, Fuel.COMMON_ITEMS.stream().map(fuel -> new ItemStack(fuel.existingItem.get())).toList(), 3);
    }

    /*****************
     * Power Flowers *
     *****************/

    @WikiImage(value = "Power-Flowers/all-tiers.png", cutout = true)
    @Scene
    public static void powerFlowersAllTiers(ServerLevel level, BlockPos pos) {
        rows(level, pos, everyTier(Matter::getPowerFlower), 4);
    }

    /**********
     * Relays *
     **********/

    @WikiImage(value = "Relays/all-tiers.png", cutout = true)
    @Scene
    public static void relaysAllTiers(ServerLevel level, BlockPos pos) {
        rows(level, pos, everyTier(Matter::getRelay), 4);
    }

    /***************************
     * Transmutation Interface *
     ***************************/

    // the same as the emc link's
    @WikiImage("Transmutation-Interface/nbt-filter-disabled.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.9, hud = true)
    public static Runnable transmutationInterfaceNbtFilterDisabled(ServerPlayer player, BlockPos pos) {
        return toggleNbtFilter(player, pos, Blocks.TRANSMUTATION_INTERFACE.get(), true);
    }

    @WikiImage("Transmutation-Interface/nbt-filter-enabled.png")
    @Scene(view = Scene.View.FRONT, pitch = 20, fill = 0.9, hud = true)
    public static Runnable transmutationInterfaceNbtFilterEnabled(ServerPlayer player, BlockPos pos) {
        return toggleNbtFilter(player, pos, Blocks.TRANSMUTATION_INTERFACE.get(), false);
    }

    // a transmutation interface with an ae2 storage bus on it, so the network sees everything the owner can afford
    @WikiImage("Transmutation-Interface/connected-to-storage.png")
    @Scene(view = Scene.View.FRONT, yaw = 25, pitch = 15, fill = 1.3)
    public static Runnable transmutationInterfaceConnectedToStorage(ServerPlayer player, BlockPos pos) {
        WikiAe2.interfaceNetwork(player, pos);
        return () -> {};
    }

    /***********
     * Helpers *
     ***********/

    // how much the exporting fluid scene's tank gets, which a basic link & pipe are done with well before it's saved
    private static final int EXPORTED_FLUID = 1500;

    // what an emc link takes for an amount of a bucket's fluid, worked out the same way the link does
    private static long fluidCost(Matter matter, ItemStack bucket, int amount) {
        long full = IEMCProxy.INSTANCE.getValue(bucket), empty = IEMCProxy.INSTANCE.getValue(net.minecraft.world.item.Items.BUCKET);
        double perBucket = Math.max(0D, (full - ((empty * matter.getFluidEfficiencyPercentage()) / 100F)) / 1000D);
        if (perBucket == 0) throw new IllegalStateException(bucket + " is free from an emc link, so there's nothing to limit how much it gives");
        return (long) Math.ceil(perBucket * amount);
    }

    // an nbt filterable block starting with its filter as given, and the click that toggles it with a philosopher's stone
    // the stone finds the block by looking from the player's eyes, so the scene has to be close enough to reach & looking at the block
    private static Runnable toggleNbtFilter(ServerPlayer player, BlockPos pos, Block block, boolean filter) {
        ServerLevel level = player.serverLevel();
        place(level, pos, block.defaultBlockState().setValue(BlockEntityNBTFilterable.FILTER, filter));
        ((BlockEntityNBTFilterable) Objects.requireNonNull(level.getBlockEntity(pos))).setOwner(player);
        player.getInventory().setItem(0, new ItemStack(PEItems.PHILOSOPHERS_STONE.asItem()));
        return () -> click(player, pos, Direction.NORTH);
    }

    // what the knowledge sharing book scenes' owner knows, the same as the old screenshot
    private static ItemStack[] sharedKnowledge() {
        return new ItemStack[] {
                new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG), new ItemStack(net.minecraft.world.item.Items.GOLDEN_PICKAXE),
                new ItemStack(Objects.requireNonNull(Matter.GREEN.getRelayItem())), new ItemStack(net.minecraft.world.item.Items.GRASS_BLOCK)
        };
    }

    // a knowledge sharing book stored by someone else who knows exactly these, since using your own book doesn't do anything
    private static ItemStack knowledgeBook(ServerPlayer player, ItemStack... known) {
        ServerPlayer owner = otherPlayer(player.serverLevel(), "Alex");
        forget(owner, known);
        learn(owner, known);
        ItemStack book = new ItemStack(Items.KNOWLEDGE_SHARING_BOOK.get());
        book.set(DataComponentTypes.OWNER, new DataComponentTypes.OwnerData(owner.getUUID(), owner.getName().getString()));
        return book;
    }

    // items in frames facing the camera from the front, in rows that read left to right, on barriers that don't show
    private static void itemFrames(ServerLevel level, BlockPos pos, List<ItemStack> stacks, int columns) {
        int rows = (stacks.size() + columns - 1) / columns;
        for (int i = 0; i < stacks.size(); i++) {
            BlockPos back = pos.west(i % columns).above(rows - 1 - i / columns);
            place(level, back, net.minecraft.world.level.block.Blocks.BARRIER);
            ItemFrame frame = new ItemFrame(level, back.north(), Direction.NORTH);
            frame.setItem(stacks.get(i).copy(), false);
            level.addFreshEntity(frame);
        }
    }

    // from other mods, which are only there at runtime
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new IllegalStateException(id + " isn't installed"));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new IllegalStateException(id + " isn't installed"));
    }

    // a Pipez pipe pulling from a side, through reflection since Pipez is only there at runtime
    private static void extract(ServerLevel level, BlockPos pipe, Direction side) {
        // pipes only have a block entity once their has_data is turned on, which Pipez does when a side's first set up
        BlockState state = level.getBlockState(pipe);
        BooleanProperty hasData = (BooleanProperty) state.getProperties().stream().filter(property -> property.getName().equals("has_data")).findFirst()
                .orElseThrow(() -> new IllegalStateException(state + " isn't a Pipez pipe"));
        level.setBlockAndUpdate(pipe, state.setValue(hasData, true));
        BlockEntity blockEntity = Objects.requireNonNull(level.getBlockEntity(pipe), "There's no pipe at " + pipe);
        try {
            blockEntity.getClass().getMethod("setExtracting", Direction.class, boolean.class).invoke(blockEntity, side, true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't make the pipe extract", e);
        }
        // pipes only work out what they connect to when placed by a player or a neighbor changes, so it's told one did
        BlockState placed = level.getBlockState(pipe);
        placed.handleNeighborChanged(level, pipe, placed.getBlock(), pipe.relative(side), false);
    }

    // the tiers that have one
    private static List<Block> everyTier(Function<Matter, ? extends Block> block) {
        return Arrays.stream(Matter.VALUES).map(block).filter(Objects::nonNull).map(Block.class::cast).toList();
    }

    private static BlockState hopper(Direction dir) {
        BlockState state = net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState();
        return state.setValue(BlockStateProperties.FACING_HOPPER, dir);
    }
}
