package com.alcatrazescapee.mcjunitlib.framework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tags.ITag;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayerFactory;

/**
 * @see IntegrationTest
 * @see IntegrationTestClass
 */
public class IntegrationTestHelper
{
    private static final Logger LOGGER = LogManager.getLogger("IntegrationTests");

    private final ServerWorld world;
    private final IntegrationTestRunner test;
    private final BlockPos origin;
    private final MutableBoundingBox boundingBox;

    private final List<Supplier<String>> assertions;
    private final List<ScheduledAction> scheduledActions;

    private int lastScheduledAction; // The last scheduled action - time out ticks are added onto this value
    private boolean failFast; // If conditions will never be set to true
    private long startedAt;

    public IntegrationTestHelper(ServerWorld world, IntegrationTestRunner test, BlockPos origin, BlockPos size)
    {
        this.world = world;
        this.test = test;
        this.origin = origin;
        this.boundingBox = new MutableBoundingBox(BlockPos.ZERO, size.offset(1, 1, 1));

        this.assertions = new ArrayList<>();
        this.scheduledActions = new ArrayList<>();
        this.failFast = false;
    }

    public void destroyBlock(BlockPos pos)
    {
        destroyBlock(pos, false);
    }

    public void destroyBlock(BlockPos pos, boolean dropBlock)
    {
        relativePos(pos).ifPresent(actualPos -> world.destroyBlock(actualPos, dropBlock));
    }

    public void placeBlock(BlockPos pos, Direction direction, Block block)
    {
        Item item = block.asItem();
        if (item instanceof BlockItem)
        {
            useItem(pos, direction, item);
        }
        else
        {
            fail("Tried to place a block which was not a BlockItem");
        }
    }

    public void placeBlock(BlockPos pos, Direction direction, BlockItem blockItem)
    {
        useItem(pos, direction, new ItemStack(blockItem), Vector3d.ZERO);
    }

    public void placeBlock(BlockPos pos, Direction direction, BlockItem blockItem, Vector3d hitVec)
    {
        useItem(pos, direction, new ItemStack(blockItem), hitVec);
    }

    public void useItem(BlockPos pos, Direction direction, Item item)
    {
        useItem(pos, direction, new ItemStack(item));
    }

    public void useItem(BlockPos pos, Direction direction, ItemStack stack)
    {
        useItem(pos, direction, stack, Vector3d.ZERO);
    }

    public void useItem(BlockPos pos, Direction direction, ItemStack stack, Vector3d hitVec)
    {
        relativePos(pos).ifPresent(actualPos -> {
            PlayerEntity player = FakePlayerFactory.getMinecraft(world); // This is required because forge NPEs in place block
            BlockRayTraceResult rayTrace = new BlockRayTraceResult(hitVec, direction, actualPos, false);
            ItemUseContext context = new ItemUseContext(world, player, Hand.MAIN_HAND, stack, rayTrace) {};
            stack.useOn(context);
        });
    }

    public void setBlockState(BlockPos pos, BlockState state)
    {
        relativePos(pos).ifPresent(actualPos -> world.setBlockAndUpdate(actualPos, state));
    }

    public void pushButton(BlockPos pos)
    {
        relativePos(pos).ifPresent(actualPos -> {
            BlockState state = world.getBlockState(actualPos);
            if (state.getBlock() instanceof AbstractButtonBlock)
            {
                ((AbstractButtonBlock) state.getBlock()).press(state, world, actualPos);
            }
        });
    }

    public void pullLever(BlockPos pos)
    {
        relativePos(pos).ifPresent(actualPos -> {
            BlockState state = world.getBlockState(actualPos);
            if (state.getBlock() instanceof LeverBlock)
            {
                ((LeverBlock) state.getBlock()).pull(state, world, actualPos);
            }
        });
    }

    /**
     * Execute an action after a certain number of ticks have elapsed
     *
     * @param ticks  The number of ticks to wait
     * @param action The action to execute
     * @return A helper can be used to schedule subsequent actions
     */
    public ScheduleHelper runAfter(int ticks, Runnable action)
    {
        scheduledActions.add(new ScheduledAction(ticks, action));
        lastScheduledAction = Math.max(lastScheduledAction, ticks);
        return new ScheduleHelper(ticks);
    }

    /**
     * Creates a scheduler for running subsequent actions
     *
     * @return A scheduler.
     */
    public ScheduleHelper scheduler()
    {
        return new ScheduleHelper(0);
    }

    public BlockState getBlockState(BlockPos pos)
    {
        return relativePos(pos).map(world::getBlockState).orElseGet(Blocks.AIR::defaultBlockState);
    }

    public FluidState getFluidState(BlockPos pos)
    {
        return relativePos(pos).map(world::getFluidState).orElseGet(Fluids.EMPTY::defaultFluidState);
    }

    @Nullable
    public TileEntity getTileEntity(BlockPos pos)
    {
        return relativePos(pos).map(world::getBlockEntity).orElse(null);
    }

    public void assertAirAt(BlockPos pos, String message)
    {
        assertBlockAt(pos, Blocks.AIR, message);
    }

    public void assertBlockAt(BlockPos pos, Block block, String message)
    {
        assertBlockAt(pos, stateIn -> stateIn.is(block), message);
    }

    public void assertBlockAt(BlockPos pos, ITag<Block> tag, String message)
    {
        assertBlockAt(pos, stateIn -> stateIn.is(tag), message);
    }

    public void assertBlockAt(BlockPos pos, BlockState state, String message)
    {
        assertBlockAt(pos, stateIn -> state == stateIn, message);
    }

    public void assertBlockAt(BlockPos pos, Predicate<BlockState> condition, String message)
    {
        relativePos(pos).ifPresent(actualPos -> assertTrue(() -> condition.test(world.getBlockState(actualPos)), message));
    }

    public void assertFluidAt(BlockPos pos, Fluid fluid, String message)
    {
        assertFluidAt(pos, stateIn -> stateIn.getType() == fluid, message);
    }

    public void assertFluidAt(BlockPos pos, FluidState fluidState, String message)
    {
        assertFluidAt(pos, stateIn -> stateIn == fluidState, message);
    }

    public void assertFluidAt(BlockPos pos, ITag<Fluid> tag, String message)
    {
        assertFluidAt(pos, stateIn -> stateIn.is(tag), message);
    }

    public void assertFluidAt(BlockPos pos, Predicate<FluidState> condition, String message)
    {
        relativePos(pos).ifPresent(actualPos -> assertTrue(() -> condition.test(world.getFluidState(actualPos)), message));
    }

    public void assertTileEntityAt(BlockPos pos, Class<? extends TileEntity> teClazz, String message)
    {
        assertTileEntityAt(pos, te -> true, teClazz, message);
    }

    public void assertTileEntityAt(BlockPos pos, TileEntityType<?> type, String message)
    {
        assertTileEntityAt(pos, te -> te.getType() == type, message);
    }

    public void assertTileEntityAt(BlockPos pos, Predicate<TileEntity> condition, String message)
    {
        relativePos(pos).ifPresent(actualPos -> assertThat(() -> {
            TileEntity te = world.getBlockEntity(pos);
            if (te != null)
            {
                return condition.test(te) ? null : message;
            }
            else
            {
                return "There was no tile entity at " + pos;
            }
        }));
    }

    @SuppressWarnings("unchecked")
    public <T extends TileEntity> void assertTileEntityAt(BlockPos pos, Predicate<T> condition, Class<T> teClass, String message)
    {
        relativePos(pos).ifPresent(actualPos -> assertThat(() -> {
            TileEntity te = world.getBlockEntity(actualPos);
            if (te == null)
            {
                return "There was no tile entity at " + pos;
            }
            if (teClass.isInstance(te))
            {
                return condition.test((T) te) ? null : message;
            }
            else
            {
                return "Tile entity at " + pos + "is not an instance of " + teClass.getName();
            }
        }));
    }

    /**
     * @param condition A condition describing if the test passes (true) or fails (false)
     * @param message   An error message for if the test fails
     */
    public void assertTrue(BooleanSupplier condition, String message)
    {
        assertions.add(() -> condition.getAsBoolean() ? null : message);
    }

    /**
     * If the supplier returns a non null string, it is assumed to be an error message and the condition has failed.
     * If the supplier returns null, the condition has passed
     *
     * @param optionalErrorIfFail A condition
     */
    public void assertThat(Supplier<String> optionalErrorIfFail)
    {
        assertions.add(optionalErrorIfFail);
    }

    public void fail(String message)
    {
        assertions.add(() -> message);
        failFast = true;
    }

    /**
     * Gets the in-world position, based on a zero centered position within the test structure
     * Anything that references the world directly should call this to handle any positions.
     * It is considered a failure to reference a position outside of the test structure.
     *
     * @param pos A position, where the origin is the origin of the test structure.
     * @return The corresponding world position, or empty if the position was outside of the test structure.
     */
    public Optional<BlockPos> relativePos(BlockPos pos)
    {
        if (boundingBox.isInside(pos))
        {
            return Optional.of(origin.offset(pos));
        }
        fail("Tried to access the position " + pos + " which was not inside the test area!");
        return Optional.empty();
    }

    /**
     * Gets the world.
     *
     * WARNING: Be careful when using this with any positions directly.
     * Make sure to call {@link IntegrationTestHelper#relativePos(BlockPos)} for any positions interacting with the world.
     *
     * @return The world
     */
    public ServerWorld getWorld()
    {
        return world;
    }

    Optional<TestResult> tick(int currentTick)
    {
        if (!scheduledActions.isEmpty())
        {
            // If actions are remaining, execute them
            final Iterator<ScheduledAction> iterator = scheduledActions.iterator();
            while (iterator.hasNext())
            {
                ScheduledAction action = iterator.next();
                if (action.ticks <= currentTick)
                {
                    iterator.remove();
                    try
                    {
                        action.action.run();
                    }
                    catch (Throwable t)
                    {
                        fail(describe(t));
                    }
                }
            }
        }
        else if (currentTick % test.getRefreshTicks() == 0)
        {
            // No remaining scheduled actions, so update conditions every refresh interval

            // Refresh conditions
            final List<String> failures = new ArrayList<>();
            for (Supplier<String> assertion : assertions)
            {
                String error;
                try
                {
                    error = assertion.get();
                }
                catch (Throwable t)
                {
                    // Counts as not passing yet, as what it checks may still be on its way, so it isn't logged each time
                    error = message(t);
                }
                if (error != null)
                {
                    failures.add(error);
                }
            }

            final int timeoutTicks = lastScheduledAction + test.getTimeoutTicks();

            if (failures.isEmpty())
            {
                // No fails or scheduled actions - Test passed!
                return TestResult.success();
            }
            if (failFast)
            {
                // Fail fast - used for invalid test configurations or direct calls to unconditional failures
                return TestResult.fail(failures);
            }
            if (timeoutTicks != -1 && currentTick >= timeoutTicks)
            {
                // Test failed due to time out
                failures.add(test.getName() + " Failed after time out at " + timeoutTicks + " ticks.");
                return TestResult.fail(failures);
            }
        }
        return Optional.empty();
    }

    BlockPos getOrigin()
    {
        return origin;
    }

    IntegrationTestRunner getTest()
    {
        return test;
    }

    long getRunTimeMillis()
    {
        return startedAt == 0 ? 0 : (System.nanoTime() - startedAt) / 1_000_000;
    }

    void run()
    {
        startedAt = System.nanoTime();
        try
        {
            test.getTestAction().accept(this);
        }
        catch (Throwable t)
        {
            fail(describe(t));
        }
    }

    // Something thrown from a test, a scheduled action or an assertion fails the test with its message, instead of crashing the server
    String describe(Throwable t)
    {
        LOGGER.debug("Error in test {}", test.getName(), t);
        return message(t);
    }

    private static String message(Throwable t)
    {
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

    public final class ScheduleHelper
    {
        final int currentTicks;

        ScheduleHelper(int currentTicks)
        {
            this.currentTicks = currentTicks;
        }

        /**
         * Execute a subsequent action
         *
         * @param extraTicks An additional number of ticks to wait
         * @param action     The action to execute
         * @return A helper for scheduling subsequent actions
         */
        public ScheduleHelper thenRun(int extraTicks, Runnable action)
        {
            return runAfter(currentTicks + extraTicks, action);
        }

        /**
         * Wait an amount of ticks
         *
         * @return A helper for scheduling subsequent actions
         */
        public ScheduleHelper thenWait(int extraTicks)
        {
            return runAfter(currentTicks + extraTicks, () -> {});
        }
    }

    static final class ScheduledAction
    {
        final int ticks;
        private final Runnable action;

        ScheduledAction(int ticks, Runnable action)
        {
            this.ticks = ticks;
            this.action = action;
        }
    }
}
