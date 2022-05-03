package cool.furry.mc.forge.projectexpansion.util;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings("unused")
public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);

    public static @Nullable
    ServerPlayerEntity getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(uuid);
    }

    public static @Nullable
    ServerPlayerEntity getPlayer(@Nullable World world, UUID uuid) {
        return world == null || world.getServer() == null ? null : world.getServer().getPlayerList().getPlayerByUUID(uuid);
    }

    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stackCopy = ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (stackCopy.isDamageable()) stackCopy.setDamage(0);
        return NBTManager.getPersistentInfo(ItemInfo.fromStack(stackCopy)).createStack();
    }

    public static AddKnowledgeResult addKnowledge(PlayerEntity player, IKnowledgeProvider provider, Item rawItem, Item cleanItem) {
        return addKnowledge(player, provider, ItemInfo.fromItem(rawItem), ItemInfo.fromItem(cleanItem));
    }

    public static AddKnowledgeResult addKnowledge(PlayerEntity player, IKnowledgeProvider provider, ItemStack rawStack, ItemStack cleanStack) {
        return addKnowledge(player, provider, ItemInfo.fromStack(rawStack), ItemInfo.fromStack(cleanStack));
    }

    public static AddKnowledgeResult addKnowledge(PlayerEntity player, IKnowledgeProvider provider, ItemInfo rawInfo, ItemInfo cleanInfo) {
        if (cleanInfo.createStack().isEmpty()) return AddKnowledgeResult.FAIL;

        if (!provider.hasKnowledge(cleanInfo)) {
            if (MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, rawInfo, cleanInfo)))
                return AddKnowledgeResult.FAIL;

            provider.addKnowledge(cleanInfo);
            return AddKnowledgeResult.SUCCESS;
        }

        return AddKnowledgeResult.UNKNOWN;
    }

    public static int safeIntValue(BigInteger val) {
        try {
            return val.intValueExact();
        } catch (ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public static long safeLongValue(BigInteger val) {
        try {
            return val.longValueExact();
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    public static int mod(int a, int b) {
        a = a % b;
        return a < 0 ? a + b : a;
    }

    public static final int SLOT_SPACING_X = 18;
    public static final int SLOT_SPACING_Y = 18;

    public static final int HOTBAR_ROWS = 1;
    public static final int HOTBAR_COLUMNS = 9;
    public static final int PLAYER_INVENTORY_ROWS = 3;
    public static final int PLAYER_INVENTORY_COLUMNS = 9;
    public static void addPlayerInventoryToContainer(Function<Slot, Slot> addSlot, IInventory inventory, int hotbarX, int hotbarY, int inventoryX, int inventoryY) {
        addPlayerInventoryToContainer(addSlot, inventory, hotbarX, hotbarY, inventoryX, inventoryY, SLOT_SPACING_X, SLOT_SPACING_Y);
    }
    public static void addPlayerInventoryToContainer(Function<Slot, Slot> addSlot, IInventory inventory, int hotbarX, int hotbarY, int inventoryX, int inventoryY, int slotSpacingX, int slotSpacingY) {
        for (int slotNumber = 0; slotNumber < HOTBAR_COLUMNS; slotNumber++) addSlot.apply(new Slot(inventory, slotNumber, hotbarX + slotSpacingX * slotNumber, hotbarY));
        addInventoryToContainer(addSlot, inventory, inventoryX, inventoryY, PLAYER_INVENTORY_COLUMNS, PLAYER_INVENTORY_ROWS, slotSpacingX, slotSpacingY, HOTBAR_COLUMNS);
    }

    public static void addInventoryToContainer(Function<Slot, Slot> addSlot, IInventory inventory, int inventoryX, int inventoryY, int inventoryColumns, int inventoryRows) {
        addInventoryToContainer(addSlot, inventory, inventoryX, inventoryY, inventoryColumns, inventoryRows, SLOT_SPACING_X, SLOT_SPACING_Y, 0);
    }
    public static void addInventoryToContainer(Function<Slot, Slot> addSlot, IInventory inventory, int inventoryX, int inventoryY, int inventoryColumns, int inventoryRows, int indexOffset) {
        addInventoryToContainer(addSlot, inventory, inventoryX, inventoryY, inventoryColumns, inventoryRows, SLOT_SPACING_X, SLOT_SPACING_Y, 0);
    }
    public static void addInventoryToContainer(Function<Slot, Slot> addSlot, IInventory inventory, int inventoryX, int inventoryY, int inventoryColumns, int inventoryRows, int slotSpacingX, int slotSpacingY) {
        addInventoryToContainer(addSlot, inventory, inventoryX, inventoryY, inventoryColumns, inventoryRows, slotSpacingX, slotSpacingY, 0);
    }
    public static void addInventoryToContainer(Function<Slot, Slot> addSlot, IInventory inventory, int inventoryX, int inventoryY, int inventoryColumns, int inventoryRows, int slotSpacingX, int slotSpacingY, int indexOffset) {
        for (int y = 0; y < inventoryRows; y++) {
            for (int x = 0; x < inventoryColumns; x++) {
                int xpos = inventoryX + x * slotSpacingX;
                int ypos = inventoryY + y * slotSpacingY;
                addSlot.apply(new Slot(inventory, indexOffset + y * inventoryColumns + x,  xpos, ypos));
            }
        }
    }

    public static boolean isMatter(ItemStack itemStack) { return isMatter(itemStack.getItem()); }
    public static boolean isMatter(Item item) {
        return Arrays.stream(Matter.VALUES).anyMatch((m) -> m.getItem() == item);
    }

    public enum AddKnowledgeResult {
        FAIL,
        UNKNOWN,
        SUCCESS,
    }
}
