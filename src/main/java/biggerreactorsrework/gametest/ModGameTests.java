package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers every {@link GameTest} method of the test classes as a test function and an instance running it in its structure. */
public final class ModGameTests {
    private static final List<Class<?>> CLASSES = withOptional(PortModelGameTests.class, AuditRegressionGameTests.class, BatteryAppearanceGameTests.class, BatteryChargingGameTests.class, BatteryRedstoneGameTests.class, CalculatorGameTests.class, CarryOnGameTests.class, ContentGameTests.class, CoolingCalibrationGameTests.class, CoolingCircuitComparisonGameTests.class, CoolingGameTests.class, CoolingIntegrationGameTests.class, EmptyCoolantTankGameTests.class, GuideGameTests.class, HeatExchangerCoolingGameTests.class, HotCoolantYieldGameTests.class, IndustrialProcessorGameTests.class, LifecycleGameTests.class, MachineAudioGameTests.class, MachineLightingGameTests.class, MultiblockGameTests.class, OperationGameTests.class, OutputBackpressureGameTests.class, OutputBottleneckStudyGameTests.class, ProcessingGameTests.class, ReactorCoolantReassemblyGameTests.class, ResourcePortAppearanceGameTests.class, SensorAppearanceGameTests.class, StabilizationGameTests.class, TurbineReloadGameTests.class);
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, BiggerReactors.modid);

    private ModGameTests() {
    }

    /** The OpenComputers tests exist only when that integration is turned on in gradle.properties. */
    private static List<Class<?>> withOptional(Class<?>... classes) {
        if (Boolean.getBoolean("biggerreactorsrework.testScripting")) return List.of(PackConfigurationGameTests.class);
        var all = new java.util.ArrayList<>(List.of(classes));
        all.add(CCTweakedGameTests.class);
        all.add(TurbineRotorPlacementGameTests.class);
        all.add(PackConfigurationGameTests.class);
        all.add(AdvancementGameTests.class);
        try {
            all.add(Class.forName("biggerreactorsrework.gametest.OpenComputersGameTests"));
        } catch (ClassNotFoundException absent) {
            // integration inactive
        }
        return List.copyOf(all);
    }

    public static void register(IEventBus modBus) {
        for (Class<?> type : CLASSES) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GameTest.class)) {
                    FUNCTIONS.register(id(method).getPath(), () -> helper -> invoke(method, helper));
                }
            }
        }
        FUNCTIONS.register(modBus);
        modBus.addListener(ModGameTests::onRegisterGameTests);
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        var environments = new java.util.HashMap<String, Holder<TestEnvironmentDefinition<?>>>();
        for (Class<?> type : CLASSES) {
            for (Method method : type.getDeclaredMethods()) {
                GameTest test = method.getAnnotation(GameTest.class);
                if (test == null) {
                    continue;
                }
                String batch = test.batch().isEmpty() ? "default" : test.batch();
                Holder<TestEnvironmentDefinition<?>> environment = environments.computeIfAbsent(batch, name -> event.registerEnvironment(id("batch/" + name)));
                Identifier id = id(method);
                TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD,
                        id(test.template()), test.timeoutTicks(), test.setupTicks(), test.required(),
                        Rotation.NONE, false, 1, 1, test.skyAccess(), 0);
                event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
            }
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BiggerReactors.modid, path);
    }

    private static Identifier id(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("GameTest " + method + " must be static");
        }
        return id(snake(method.getDeclaringClass().getSimpleName()) + "/" + snake(method.getName()));
    }

    private static String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
