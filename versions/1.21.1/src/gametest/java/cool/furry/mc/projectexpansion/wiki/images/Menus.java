package cool.furry.mc.projectexpansion.wiki.images;

import appeng.client.gui.me.items.CraftingTermScreen;
import cool.furry.mc.projectexpansion.block.BlockEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Input;
import cool.furry.mc.projectexpansion.gui.GUICondenserMK3Output;
import cool.furry.mc.projectexpansion.gui.GUIEMCRefinery;
import cool.furry.mc.projectexpansion.gui.GUIEmcPipe;
import cool.furry.mc.projectexpansion.gui.GUIPowerFlower;
import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.item.ItemArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.pipe.Distribution;
import cool.furry.mc.projectexpansion.wiki.Menu;
import cool.furry.mc.projectexpansion.wiki.WikiAe2;
import cool.furry.mc.projectexpansion.wiki.WikiEmi;
import cool.furry.mc.projectexpansion.wiki.WikiImage;
import cool.furry.mc.projectexpansion.wiki.WikiPlayer;
import dev.emi.emi.screen.RecipeScreen;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static cool.furry.mc.projectexpansion.wiki.WikiBlocks.clear;
import static cool.furry.mc.projectexpansion.wiki.WikiBlocks.click;
import static cool.furry.mc.projectexpansion.wiki.WikiBlocks.place;
import static cool.furry.mc.projectexpansion.wiki.WikiBlocks.use;

// see WikiImage, listed in WikiImages.DEFINITIONS
public final class Menus {
    private Menus() {}

    /*******************************
     * Arcane Transmutation Tablet *
     *******************************/

    @WikiImage(value = "Arcane-Transmutation-Tablet/menu.png", cutout = true)
    @Menu(screen = GUIArcaneTransmutationTablet.class)
    public static Runnable arcaneTabletMenu(ServerPlayer player, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ARCANE_TRANSMUTATION_TABLET.get()));
        return () -> ((ItemArcaneTransmutationTablet) Items.ARCANE_TRANSMUTATION_TABLET.get()).openContainer(player);
    }

    // the buttons beside the crafting grid
    @WikiImage(value = "Arcane-Transmutation-Tablet/grid-buttons.png", cutout = true)
    @Menu(screen = GUIArcaneTransmutationTablet.class, crop = { -75, 10, 76, 89 })
    public static Runnable arcaneTabletGridButtons(ServerPlayer player, BlockPos pos) {
        return arcaneTabletMenu(player, pos);
    }

    // the clear button turns red with shift held, for a forced clear
    @WikiImage(value = "Arcane-Transmutation-Tablet/force-clear-button.png", cutout = true)
    @Menu(screen = GUIArcaneTransmutationTablet.class, crop = { -75, 10, 76, 89 }, mouse = { -67, 65 }, shift = true)
    public static Runnable arcaneTabletForceClearButton(ServerPlayer player, BlockPos pos) {
        return arcaneTabletMenu(player, pos);
    }

    // oak stairs filled from jei/emi with one plank short, so the last one's a ghost plank, hovered
    @WikiImage(value = "Arcane-Transmutation-Tablet/ghost-ingredient-tooltip.png", cutout = true)
    @Menu(screen = GUIArcaneTransmutationTablet.class, crop = { -75, 10, 372, 96 }, mouse = { -15, 61 })
    public static Runnable arcaneTabletGhostIngredientTooltip(ServerPlayer player, BlockPos pos) {
        WikiPlayer.learn(player, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS));
        WikiPlayer.setEmc(player, 1_000_000);
        player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 5));
        Runnable open = arcaneTabletMenu(player, pos);
        return () -> {
            open.run();
            ((ContainerArcaneTransmutationTablet) player.containerMenu).onRecipeTransfer(stairs(), false, Optional.empty());
        };
    }

    // emi's oak stairs recipe over the tablet with its + hovered, with emc for one plank & none in the inventory, so the rest won't be filled
    @WikiImage("Arcane-Transmutation-Tablet/recipe-transfer-tooltip.png")
    // the recipe & tooltip, from the window's top left since emi's screen isn't a menu
    @Menu(screen = RecipeScreen.class, crop = { 177, 70, 233, 84 })
    public static Runnable arcaneTabletRecipeTransferTooltip(ServerPlayer player, BlockPos pos) {
        WikiPlayer.learn(player, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS));
        WikiPlayer.setEmc(player, 12);
        Runnable open = arcaneTabletMenu(player, pos);
        return () -> {
            open.run();
            WikiEmi.hoverFill(GUIArcaneTransmutationTablet.class, ResourceLocation.withDefaultNamespace("oak_stairs"));
        };
    }

    // like jei & emi hand it over, a list of options for each grid slot, which the tablet sorts so they have to be mutable
    private static List<List<ItemStack>> stairs() {
        List<List<ItemStack>> recipe = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            boolean plank = i == 0 || i == 3 || i == 4 || i >= 6;
            recipe.add(plank ? new ArrayList<>(List.of(new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS))) : new ArrayList<>());
        }
        return recipe;
    }

    /*****************
     * Condenser MK3 *
     *****************/

    // the top side locked to iron, with diamonds still waiting in its input
    @WikiImage(value = "Condenser-MK3/side-menu.png", cutout = true)
    @Menu(screen = GUICondenserMK3Input.class, settleTicks = 30)
    public static Runnable condenserSideMenu(ServerPlayer player, BlockPos pos) {
        BlockEntityCondenserMK3 condenser = condenser(player, pos);
        return () -> condenser.openMenu(player, Direction.UP);
    }

    // full of the iron the top side made
    @WikiImage(value = "Condenser-MK3/output-menu.png", cutout = true)
    @Menu(screen = GUICondenserMK3Output.class, settleTicks = 30)
    public static Runnable condenserOutputMenu(ServerPlayer player, BlockPos pos) {
        BlockEntityCondenserMK3 condenser = condenser(player, pos);
        return () -> condenser.openMenu(player, Direction.DOWN);
    }

    /*************
     * EMC Pipes *
     *************/

    // an improved upgrade, so distribution can be changed
    @WikiImage(value = "EMC-Pipes/settings-menu.png", cutout = true)
    @Menu(screen = GUIEmcPipe.class)
    public static Runnable emcPipeSettingsMenu(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockPos pipePos = pos.east();
        clear(level, pos, pipePos);
        // the pipe first, so it connects to the collector when that's placed
        place(level, pipePos, Blocks.EMC_PIPE.get());
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getCollector()));
        BlockEmcPipe.useWrench(level, pipePos, level.getBlockState(pipePos), Direction.WEST, false);
        BlockEntityEmcPipe pipe = (BlockEntityEmcPipe) Objects.requireNonNull(level.getBlockEntity(pipePos));
        pipe.setUpgradeStack(Direction.WEST, new ItemStack(Items.IMPROVED_PIPE_UPGRADE.get()));
        pipe.setStoredSettings(Direction.WEST, pipe.getStoredSettings(Direction.WEST).withDistribution(Distribution.ROUND_ROBIN));
        return () -> use(player, pipePos, Direction.WEST);
    }

    /****************
     * EMC Refinery *
     ****************/

    // partway through refining raw ianthium
    @WikiImage(value = "EMC-Refinery/menu.png", cutout = true)
    @Menu(screen = GUIEMCRefinery.class, settleTicks = 50)
    public static Runnable emcRefineryMenu(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        clear(level, pos);
        place(level, pos, Blocks.EMC_REFINERY.get());
        BlockEntityEMCRefinery refinery = (BlockEntityEMCRefinery) Objects.requireNonNull(level.getBlockEntity(pos));
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.INPUT_SLOT, new ItemStack(Items.RAW_IANTHIUM.get(), 16));
        ItemStack star = new ItemStack(PEItems.KLEIN_STAR_EIN.get());
        IItemEmcHolder holder = Objects.requireNonNull(star.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY));
        holder.insertEmc(star, holder.getMaximumEmc(star), IEmcStorage.EmcAction.EXECUTE);
        refinery.getItems().setStackInSlot(BlockEntityEMCRefinery.STAR_SLOT, star);
        return () -> use(player, pos, Direction.NORTH);
    }

    /*****************
     * Power Flowers *
     *****************/

    @WikiImage(value = "Power-Flowers/menu.png", cutout = true)
    @Menu(screen = GUIPowerFlower.class)
    public static Runnable powerFlowerMenu(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        clear(level, pos);
        place(level, pos, Objects.requireNonNull(Matter.BASIC.getPowerFlower()));
        ((BlockEntityPowerFlower) Objects.requireNonNull(level.getBlockEntity(pos))).setOwner(player);
        return () -> use(player, pos, Direction.UP);
    }

    /***********
     * Helpers *
     ***********/

    // an ae2 terminal on a network with a transmutation interface, with stone hovered, as many as the int limit since the owner can afford far more
    @WikiImage("Transmutation-Interface/terminal.png")
    @Menu(screen = CraftingTermScreen.class, settleTicks = 40, mouse = { 17, 27 })
    public static Runnable transmutationInterfaceTerminal(ServerPlayer player, BlockPos pos) {
        // a few blocks over so it's clear of the player
        BlockPos at = pos.east(4);
        WikiAe2.interfaceNetwork(player, at);
        return () -> click(player, WikiAe2.terminal(at), Direction.NORTH);
    }

    private static BlockEntityCondenserMK3 condenser(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        clear(level, pos);
        place(level, pos, Blocks.CONDENSER_MK3.get());
        BlockEntityCondenserMK3 condenser = (BlockEntityCondenserMK3) Objects.requireNonNull(level.getBlockEntity(pos));
        condenser.getSidedHandler(Direction.UP).setLockInfoFromPacket(ItemInfo.fromItem(net.minecraft.world.item.Items.IRON_INGOT));
        IItemHandler input = condenser.getAutomationSidedItemHandler(Direction.UP);
        // more than the output holds, so some stay in the input
        for (int i = 0; i < 8; i++) ItemHandlerHelper.insertItem(input, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64), false);
        return condenser;
    }
}
