package com.alcatrazescapee.mcjunitlib.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tileentity.LecternTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;

import com.alcatrazescapee.mcjunitlib.framework.mod.ForgeEventHandler;
import org.objectweb.asm.Type;

/**
 * Main handler for integration tests
 *
 * The rest will be handled for you.
 */
public enum IntegrationTestManager
{
    INSTANCE;

    private static final Type INTEGRATION_TEST = Type.getType(IntegrationTest.class);
    private static final Type INTEGRATION_TEST_FACTORY = Type.getType(IntegrationTestFactory.class);

    private static final Logger LOGGER = LogManager.getLogger("IntegrationTests");
    private static final Level UNIT_TEST = Level.forName("UNITTEST", 50);

    private static String bootstrapModId;

    /**
     * @param modId The mod id in question
     * @deprecated Use the `targetModId` environment variable instead, set from the runServerTest run config.
     */
    @Deprecated
    public static void setup(String modId)
    {
        bootstrapModId = modId;
        LOGGER.warn("IntegrationTestManager#setup(String) is deprecated and will be removed at a later date. Use the `targetModId` environment variable instead");
    }

    public static void setup()
    {
        final String targetModId = Optional.ofNullable(System.getenv("targetModId")).orElse(bootstrapModId);

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        ModList.get().getAllScanData().stream()
            .map(ModFileScanData::getAnnotations)
            .flatMap(Collection::stream)
            .flatMap(annotation -> createIntegrationTests(targetModId, annotation))
            .forEach(IntegrationTestManager.INSTANCE::add);
    }

    private static Stream<IntegrationTestRunner> createIntegrationTests(String modId, ModFileScanData.AnnotationData annotation)
    {
        if (annotation.getAnnotationType().equals(INTEGRATION_TEST))
        {
            final IntegrationTestRunner test = createIntegrationTest(modId, annotation);
            if (test != null)
            {
                return Stream.of(test);
            }
        }
        else if (annotation.getAnnotationType().equals(INTEGRATION_TEST_FACTORY))
        {
            return createIntegrationTestStream(modId, annotation);
        }
        return Stream.empty();
    }

    @Nullable
    private static IntegrationTestRunner createIntegrationTest(String modId, ModFileScanData.AnnotationData annotation)
    {
        final String targetClass = annotation.getClassType().getClassName();
        final String targetName = annotation.getMemberName();
        final String targetDescriptor = "(Lcom/alcatrazescapee/mcjunitlib/framework/IntegrationTestHelper;)V";

        if (!targetName.endsWith(targetDescriptor))
        {
            LOGGER.error("Unable to resolve integration test at {}.{} (Invalid Method Signature - Must take a parameter of type IntegrationTestHelper and return void)", targetClass, targetName);
            return null;
        }

        final String targetMethodName = targetName.substring(0, targetName.length() - targetDescriptor.length());

        try
        {
            final Class<?> clazz = Class.forName(targetClass);
            final Method method = clazz.getDeclaredMethod(targetMethodName, IntegrationTestHelper.class);

            method.setAccessible(true);

            final Object instance = ((method.getModifiers() & Modifier.STATIC) == Modifier.STATIC) ? null : clazz.newInstance();
            final IntegrationTest typedAnnotation = method.getDeclaredAnnotation(IntegrationTest.class);
            final String className = testClassName(clazz);
            final String testName = testMethodName(typedAnnotation.value(), method.getName());
            final String testMethodName = clazz.getSimpleName() + '.' + method.getName();
            final ResourceLocation templateName = new ResourceLocation(modId, (className + '/' + testName).toLowerCase());

            return new IntegrationTestRunner(clazz, helper -> {
                try
                {
                    method.invoke(instance, helper);
                }
                catch (InvocationTargetException e)
                {
                    // Thrown by the test itself, so it fails with its own message
                    helper.fail(helper.describe(e.getCause()));
                }
                catch (IllegalAccessException e)
                {
                    LOGGER.warn("Unable to resolve integration test at {} (Cannot Invoke Method - {})", testName, e.getMessage());
                    LOGGER.debug("Error", e);
                    helper.fail("Reflection Error: " + e.getMessage());
                }
            }, testMethodName, templateName, typedAnnotation.refreshTicks(), typedAnnotation.timeoutTicks());
        }
        catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException e)
        {
            LOGGER.error("Unable to resolve integration test at {}.{} (Unknown Exception - {})", targetClass, targetName, e.getMessage());
            LOGGER.debug("Error", e);
        }
        return null;
    }

    private static Stream<IntegrationTestRunner> createIntegrationTestStream(String modId, ModFileScanData.AnnotationData annotation)
    {
        final String targetClass = annotation.getClassType().getClassName();
        final String targetName = annotation.getMemberName();
        final String targetDescriptor = "()Ljava/util/stream/Stream;";

        if (!targetName.endsWith(targetDescriptor))
        {
            LOGGER.error("Unable to resolve integration test at {}.{} (Invalid Method Signature - Must take no parameters and return a Stream of DynamicIntegrationTest instances)", targetClass, targetName);
            return Stream.empty();
        }

        final String targetMethodName = targetName.substring(0, targetName.length() - targetDescriptor.length());

        try
        {
            final Class<?> clazz = Class.forName(targetClass);
            final Method method = clazz.getDeclaredMethod(targetMethodName);

            method.setAccessible(true);

            final Object instance = ((method.getModifiers() & Modifier.STATIC) == Modifier.STATIC) ? null : clazz.newInstance();
            final IntegrationTestFactory typedAnnotation = method.getDeclaredAnnotation(IntegrationTestFactory.class);
            final String className = testClassName(clazz);
            final String testName = testMethodName(typedAnnotation.value(), method.getName());
            final String testMethodName = clazz.getSimpleName() + '.' + method.getName();
            final ResourceLocation templateName = new ResourceLocation(modId, (className + '/' + testName).toLowerCase());

            try
            {
                return ((Stream<?>) method.invoke(instance))
                    .map(obj -> {
                        if (obj instanceof DynamicIntegrationTest)
                        {
                            final DynamicIntegrationTest dynamic = (DynamicIntegrationTest) obj;
                            return new IntegrationTestRunner(clazz, dynamic.getTestAction(), testMethodName + '/' + dynamic.getName(), templateName, typedAnnotation.refreshTicks(), typedAnnotation.timeoutTicks());
                        }
                        LOGGER.error("Unable to resolve dynamic integration test at {}.{} (Stream element was not a DynamicIntegrationTest)", targetClass, targetName);
                        return null;
                    })
                    .filter(Objects::nonNull);
            }
            catch (InvocationTargetException e)
            {
                LOGGER.error("Unable to resolve dynamic integration test at {}.{} (Could not invoke factory method - {})", targetClass, targetName, e.getMessage());
                LOGGER.debug("Error", e);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException e)
        {
            LOGGER.error("Unable to resolve dynamic integration test at {}.{} (Unknown Exception - {})", targetClass, targetName, e.getMessage());
            LOGGER.debug("Error", e);
        }
        return Stream.empty();
    }

    private static String testClassName(Class<?> clazz)
    {
        final IntegrationTestClass annotation = clazz.getDeclaredAnnotation(IntegrationTestClass.class);
        if (annotation != null && !"".equals(annotation.value()))
        {
            return annotation.value();
        }
        return clazz.getSimpleName();
    }

    private static String testMethodName(String value, String fallback)
    {
        return "".equals(value) ? fallback : value;
    }

    private final HashMap<String, List<IntegrationTestRunner>> sortedTests;
    private final List<IntegrationTestRunner> allTests;
    private final List<IntegrationTestHelper> activeTests;

    private int passedTests, failedTests;
    private int currentTick;
    private Status status;

    IntegrationTestManager()
    {
        this.allTests = new ArrayList<>();
        this.sortedTests = new HashMap<>();
        this.activeTests = new ArrayList<>();
        this.passedTests = 0;
        this.failedTests = 0;
        this.status = Status.WAITING;
    }

    public boolean isComplete()
    {
        return status == Status.FINISHED;
    }

    public boolean hasFailedTests()
    {
        return failedTests > 0;
    }

    public boolean verifyAllTests(ServerWorld world, BiConsumer<String, Boolean> logger)
    {
        if (status == Status.WAITING)
        {
            final TemplateManager manager = world.getStructureManager();
            boolean allPassed = true;
            for (IntegrationTestRunner test : allTests)
            {
                if (manager.get(test.getTemplateName()) == null)
                {
                    logger.accept("Test '" + test.getName() + "' failed verification: No template '" + test.getTemplateName() + "' found.", false);
                    allPassed = false;
                }
            }
            if (allPassed)
            {
                status = Status.VERIFIED;
                logger.accept("Verified all tests.", true);
                return true;
            }
            logger.accept("One more more tests failed verification.", false);
            return false;
        }
        return true;
    }

    public void setupAllTests(ServerWorld world, BiConsumer<String, Boolean> logger)
    {
        if (status == Status.VERIFIED || status == Status.FINISHED || status == Status.SETUP)
        {
            status = Status.SETUP;

            passedTests = failedTests = 0;
            activeTests.clear();
            currentTick = 0;

            int testFloorY = 3;

            final TemplateManager manager = world.getStructureManager();
            final BlockPos.Mutable cursor = new BlockPos.Mutable(0, testFloorY, 0);
            final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
            final Random random = new Random();
            final PlacementSettings settings = new PlacementSettings().setRandom(random);

            int maxZSize = 0;

            for (Map.Entry<String, List<IntegrationTestRunner>> entry : sortedTests.entrySet())
            {
                for (IntegrationTestRunner test : entry.getValue())
                {
                    final Template template = manager.getOrCreate(test.getTemplateName());
                    final BlockPos size = template.getSize();
                    final BlockPos testBoxOrigin = cursor.immutable();
                    final BlockPos testTemplateOrigin = testBoxOrigin.offset(1, 1, 1);

                    // Clear the test area
                    for (int x = testBoxOrigin.getX(); x <= testBoxOrigin.getX() + size.getX() + 1; x++)
                    {
                        for (int z = testBoxOrigin.getZ(); z <= testBoxOrigin.getZ() + size.getZ() + 1; z++)
                        {
                            mutablePos.set(x, testFloorY, z);

                            // Build a floor with a fancy construction-tape border
                            if (x == testBoxOrigin.getX() || x == testBoxOrigin.getX() + size.getX() + 1 || z == testBoxOrigin.getZ() || z == testBoxOrigin.getZ() + size.getZ() + 1)
                            {
                                // Border
                                world.setBlockAndUpdate(mutablePos, ((x + z) & 1) == 0 ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState());
                            }
                            else
                            {
                                world.setBlockAndUpdate(mutablePos, Blocks.GRAY_CONCRETE.defaultBlockState());
                            }

                            // Clear the area of the test
                            for (int y = testTemplateOrigin.getY(); y <= testTemplateOrigin.getY() + size.getY() + 1; y++)
                            {
                                mutablePos.set(x, y, z);
                                world.setBlockAndUpdate(mutablePos, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }

                    // Build the indicator beacon
                    for (int x = testBoxOrigin.getX() - 1; x <= testBoxOrigin.getX() + 1; x++)
                    {
                        for (int z = testBoxOrigin.getZ() - 1; z <= testBoxOrigin.getZ() + 1; z++)
                        {
                            mutablePos.set(x, testFloorY - 2, z);
                            world.setBlockAndUpdate(mutablePos, Blocks.IRON_BLOCK.defaultBlockState());
                        }
                    }
                    world.setBlockAndUpdate(mutablePos.setWithOffset(testBoxOrigin, Direction.DOWN), Blocks.BEACON.defaultBlockState());
                    world.setBlockAndUpdate(mutablePos.set(testBoxOrigin), Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState());

                    // Add the lectern with log book
                    world.setBlockAndUpdate(mutablePos.setWithOffset(testBoxOrigin, -1, 1, -1), Blocks.LECTERN.defaultBlockState());
                    ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
                    editLogBook(book, test.getName(), "Setup", Collections.emptyList());
                    LecternBlock.tryPlaceBook(world, mutablePos, world.getBlockState(mutablePos), book);

                    // Build the test itself
                    template.placeInWorld(world, testTemplateOrigin, settings, random);

                    // Move the cursor
                    cursor.move(Direction.EAST, size.getX() + 2 + 3); // +x
                    maxZSize = Math.max(maxZSize, size.getZ());

                    // Begin test
                    final IntegrationTestHelper helper = new IntegrationTestHelper(world, test, testTemplateOrigin, size);
                    activeTests.add(helper);
                }

                // Move the cursor to the next row
                cursor.setX(0);
                cursor.move(Direction.SOUTH, maxZSize + 2 + 3); // +z
            }
            logger.accept("Setup Finished!", true);
        }
        else
        {
            logger.accept("Setup not possible - tests may still be running.", false);
        }
    }

    public void runAllTests(ServerWorld world, BiConsumer<String, Boolean> logger)
    {
        if (status == Status.SETUP)
        {
            if (activeTests.isEmpty())
            {
                logger.accept("No tests found.", true);
                status = Status.FINISHED;
            }
            else
            {
                for (IntegrationTestHelper activeTest : activeTests)
                {
                    // Run tests and setup conditions
                    activeTest.run();

                    // Update the log book
                    TileEntity te = world.getBlockEntity(activeTest.getOrigin().offset(-2, 0, -2));
                    if (te instanceof LecternTileEntity)
                    {
                        editLogBook(((LecternTileEntity) te).getBook(), activeTest.getTest().getName(), "Running", Collections.emptyList());
                    }
                }
                status = Status.RUNNING;
                logger.accept("Running Tests...", true);
            }
        }
        else
        {
            logger.accept("Cannot run tests now! Current status = " + status.name().toLowerCase(), false);
        }
    }

    public void tick(ServerWorld world)
    {
        if (!activeTests.isEmpty() && status == Status.RUNNING)
        {
            currentTick++;
            Iterator<IntegrationTestHelper> iterator = activeTests.iterator();
            while (iterator.hasNext())
            {
                IntegrationTestHelper helper = iterator.next();
                helper.tick(currentTick).ifPresent(result -> {
                    BlockState glass;
                    if (result.isSuccess())
                    {
                        glass = Blocks.GREEN_STAINED_GLASS.defaultBlockState();
                        passedTests++;
                    }
                    else
                    {
                        glass = Blocks.RED_STAINED_GLASS.defaultBlockState();
                        failedTests++;

                        // Send failure messages!
                        if (!result.getErrors().isEmpty())
                        {
                            LOGGER.log(UNIT_TEST, "Test Failed {}", helper.getTest().getName());
                            for (String error : result.getErrors())
                            {
                                LOGGER.log(UNIT_TEST, error);
                            }
                        }
                    }

                    // Update the beacon state
                    world.setBlockAndUpdate(helper.getOrigin().offset(-1, -1, -1), glass);

                    // Update the log book
                    TileEntity te = world.getBlockEntity(helper.getOrigin().offset(-2, 0, -2));
                    if (te instanceof LecternTileEntity && ((LecternTileEntity) te).hasBook())
                    {
                        String status = result.isSuccess() ? "Pass" : "Fail";
                        editLogBook(((LecternTileEntity) te).getBook(), helper.getTest().getName(), status, result.getErrors());
                    }

                    iterator.remove();
                });
            }

            if (activeTests.isEmpty())
            {
                int totalTests = passedTests + failedTests;
                LOGGER.log(UNIT_TEST, "Integration Testing Complete!");
                LOGGER.log(UNIT_TEST, "Passed: {} / {} ({} %)", passedTests, totalTests, String.format("%.1f", 100f * passedTests / totalTests));
                LOGGER.log(UNIT_TEST, "Failed: {} / {} ({} %)", failedTests, totalTests, String.format("%.1f", 100f * failedTests / totalTests));

                status = Status.FINISHED;
            }
        }
    }

    void add(IntegrationTestRunner test)
    {
        allTests.add(test);
        sortedTests.computeIfAbsent(test.getClassName(), key -> new ArrayList<>()).add(test);
    }

    private void editLogBook(ItemStack stack, String testName, String status, List<String> errors)
    {
        CompoundNBT bookNbt = new CompoundNBT();
        ListNBT pagesNbt = new ListNBT();
        StringBuilder builder = new StringBuilder().append("Test:\n").append(testName).append("\nStatus: ").append(status);
        if (!errors.isEmpty())
        {
            builder.append("\nErrors:");
            for (String error : errors)
            {
                builder.append('\n').append(error);
            }
        }
        while (builder.length() > 200)
        {
            pagesNbt.add(StringNBT.valueOf(builder.substring(0, 200) + "..."));
            builder.delete(0, 200);
        }
        pagesNbt.add(StringNBT.valueOf(builder.toString()));
        bookNbt.put("pages", pagesNbt);
        stack.setTag(bookNbt);
    }

    private enum Status
    {
        WAITING,
        VERIFIED,
        SETUP,
        RUNNING,
        FINISHED
    }
}
