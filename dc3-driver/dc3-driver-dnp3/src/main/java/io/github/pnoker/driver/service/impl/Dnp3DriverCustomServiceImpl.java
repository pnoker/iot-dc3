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
package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import io.stepfunc.dnp3.AnalogInput;
import io.stepfunc.dnp3.AnalogOutputStatus;
import io.stepfunc.dnp3.AssociationConfig;
import io.stepfunc.dnp3.AssociationHandler;
import io.stepfunc.dnp3.AssociationId;
import io.stepfunc.dnp3.AssociationInformation;
import io.stepfunc.dnp3.BinaryInput;
import io.stepfunc.dnp3.BinaryOutputStatus;
import io.stepfunc.dnp3.Classes;
import io.stepfunc.dnp3.ClientState;
import io.stepfunc.dnp3.ClientStateListener;
import io.stepfunc.dnp3.CommandMode;
import io.stepfunc.dnp3.CommandSet;
import io.stepfunc.dnp3.ConnectStrategy;
import io.stepfunc.dnp3.ControlCode;
import io.stepfunc.dnp3.Counter;
import io.stepfunc.dnp3.DoubleBitBinaryInput;
import io.stepfunc.dnp3.EndpointList;
import io.stepfunc.dnp3.EventClasses;
import io.stepfunc.dnp3.FunctionCode;
import io.stepfunc.dnp3.Group12Var1;
import io.stepfunc.dnp3.HeaderInfo;
import io.stepfunc.dnp3.LinkErrorMode;
import io.stepfunc.dnp3.MasterChannel;
import io.stepfunc.dnp3.MasterChannelConfig;
import io.stepfunc.dnp3.OpType;
import io.stepfunc.dnp3.ReadHandler;
import io.stepfunc.dnp3.ReadType;
import io.stepfunc.dnp3.Request;
import io.stepfunc.dnp3.ResponseHeader;
import io.stepfunc.dnp3.Runtime;
import io.stepfunc.dnp3.RuntimeConfig;
import io.stepfunc.dnp3.TaskError;
import io.stepfunc.dnp3.TaskType;
import io.stepfunc.dnp3.UtcTimestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joou.UByte;
import org.joou.ULong;
import org.joou.UShort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom driver service implementation for the DNP3 (IEEE 1815) master driver.
 * <p>
 * Maintains one {@link Runtime} + TCP {@link MasterChannel} + association per outstation device.
 * Reads trigger a class 0/1/2/3 integrity poll through a {@link ReadHandler} that caches the latest
 * point values by index; writes issue {@code DIRECT_OPERATE} commands via {@link CommandSet}.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Dnp3DriverCustomServiceImpl implements DriverCustomService {

    private static final String POINT_TYPE_BINARY_INPUT = "BINARY_INPUT";
    private static final String POINT_TYPE_ANALOG_INPUT = "ANALOG_INPUT";
    private static final String POINT_TYPE_COUNTER = "COUNTER";
    private static final String POINT_TYPE_DOUBLE_BIT = "DOUBLE_BIT_BINARY_INPUT";
    private static final String POINT_TYPE_BINARY_OUTPUT = "BINARY_OUTPUT";
    private static final String POINT_TYPE_ANALOG_OUTPUT = "ANALOG_OUTPUT";
    private static final long POLL_TIMEOUT_MILLIS = 5_000L;

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, Dnp3Connection> connectionMap;

    private static void checkRequired(Map<String, AttributeBO> config, String code,
                                      List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code).level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code).build());
        }
    }

    @Override
    public void initial() {
        connectionMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // Class polling is driven on demand by the SDK read schedule.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        Dnp3Connection connection = connectionMap.get(device.getId());
        if (Objects.isNull(connection)) {
            return DeviceHealthState.offline();
        }
        return ClientState.CONNECTED.equals(connection.state().get())
                ? DeviceHealthState.online()
                : DeviceHealthState.offline();
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)
                && (MetadataOperateTypeEnum.DELETE.equals(operateType)
                || MetadataOperateTypeEnum.UPDATE.equals(operateType))) {
            Dnp3Connection removed = connectionMap.remove(metadataEvent.getId());
            if (Objects.nonNull(removed)) {
                removed.close();
                log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                        driverCode, metadataEvent.getId(), operateType);
            }
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        Dnp3Connection connection = getConnection(device.getId(), driverConfig);
        int pointIndex = getRequiredIntConfig(pointConfig, "pointIndex");
        String pointType = getConfigValue(pointConfig, "pointType", POINT_TYPE_BINARY_INPUT);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            CacheReadHandler handler = new CacheReadHandler(connection.cache(), latch);
            connection.channel().readWithHandler(connection.associationId(),
                            Request.classRequest(true, true, true, true), handler)
                    .exceptionally(ex -> {
                        log.warn("DNP3 poll failed, protocol={}, deviceId={}",
                                driverCode, device.getId(), ex);
                        return null;
                    });
            if (!latch.await(POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new ReadPointException("DNP3 poll timed out, protocol={}, deviceId={}",
                        driverCode, device.getId());
            }
            String value = connection.cache()
                    .getOrDefault(pointType, Map.of())
                    .get(pointIndex);
            if (Objects.isNull(value)) {
                throw new ReadPointException("DNP3 point not found, protocol={}, pointType={}, pointIndex={}",
                        driverCode, pointType, pointIndex);
            }
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadPointException("DNP3 read failed, protocol={}, message={}",
                    driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        Dnp3Connection connection = getConnection(device.getId(), driverConfig);
        int pointIndex = getRequiredIntConfig(pointConfig, "pointIndex");
        String pointType = getConfigValue(pointConfig, "pointType", POINT_TYPE_BINARY_INPUT);
        String rawValue = writePointValue.getValue(String.class);
        try {
            CommandSet commandSet = new CommandSet();
            if (POINT_TYPE_BINARY_OUTPUT.equals(pointType)) {
                boolean on = Boolean.parseBoolean(rawValue);
                commandSet.addG12V1U8(UByte.valueOf(pointIndex),
                        Group12Var1.fromCode(ControlCode.fromOpType(on ? OpType.LATCH_ON : OpType.LATCH_OFF)));
            } else if (POINT_TYPE_ANALOG_OUTPUT.equals(pointType)) {
                commandSet.addG41V1U16(UShort.valueOf(pointIndex), Integer.parseInt(rawValue));
            } else {
                throw new WritePointException("DNP3 write unsupported point type, protocol={}, pointType={}",
                        driverCode, pointType);
            }
            connection.channel()
                    .operate(connection.associationId(), CommandMode.DIRECT_OPERATE, commandSet)
                    .toCompletableFuture()
                    .get(POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            return true;
        } catch (WritePointException e) {
            throw e;
        } catch (Exception e) {
            throw new WritePointException("DNP3 write failed, protocol={}, message={}",
                    driverCode, e.getMessage(), e);
        }
    }

    private Dnp3Connection getConnection(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectionMap.computeIfAbsent(deviceId, id -> createConnection(id, driverConfig));
    }

    private Dnp3Connection createConnection(Long deviceId, Map<String, AttributeBO> driverConfig) {
        String host = getRequiredConfig(driverConfig, "host");
        int port = getConfigIntValue(driverConfig, "port", 20_000);
        int masterAddress = getConfigIntValue(driverConfig, "masterAddress", 1);
        int outstationAddress = getConfigIntValue(driverConfig, "outstationAddress", 1);
        try {
            Runtime runtime = new Runtime(new RuntimeConfig());
            MasterChannelConfig channelConfig = new MasterChannelConfig(UShort.valueOf(masterAddress));
            EndpointList endpoints = new EndpointList(host + ":" + port);
            ConnectStrategy connectStrategy = new ConnectStrategy();
            AtomicReference<ClientState> state = new AtomicReference<>(ClientState.DISABLED);
            MasterChannel channel = MasterChannel.createTcpChannel(runtime, LinkErrorMode.CLOSE, channelConfig,
                    endpoints, connectStrategy, new ClientStateListener() {
                        @Override
                        public void onChange(ClientState clientState) {
                            state.set(clientState);
                            log.debug("DNP3 client state changed, protocol={}, deviceId={}, state={}",
                                    driverCode, deviceId, clientState);
                        }
                    });
            Map<String, Map<Integer, String>> cache = new ConcurrentHashMap<>();
            AssociationConfig associationConfig = new AssociationConfig(
                    EventClasses.all(), EventClasses.none(), Classes.all(), EventClasses.none());
            AssociationId associationId = channel.addAssociation(
                    UShort.valueOf(outstationAddress),
                    associationConfig,
                    new CacheReadHandler(cache, null),
                    new CurrentTimeAssociationHandler(),
                    new NoopAssociationInformation());
            log.info("Driver connection established, protocol={}, deviceId={}, host={}:{}",
                    driverCode, deviceId, host, port);
            return new Dnp3Connection(runtime, channel, associationId, cache, state);
        } catch (Exception e) {
            throw new ConnectorException("Failed to create DNP3 channel: {}:{}", host, port, e);
        }
    }

    private String getRequiredConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
    }

    private int getRequiredIntConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(Integer.class);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Integer.class);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "pointIndex", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    /**
     * Holder pairing the DNP3 runtime, channel, association id, and cached point values.
     */
    private static final class Dnp3Connection {

        private final Runtime runtime;
        private final MasterChannel channel;
        private final AssociationId associationId;
        private final Map<String, Map<Integer, String>> cache;
        private final AtomicReference<ClientState> state;

        Dnp3Connection(Runtime runtime, MasterChannel channel, AssociationId associationId,
                       Map<String, Map<Integer, String>> cache, AtomicReference<ClientState> state) {
            this.runtime = runtime;
            this.channel = channel;
            this.associationId = associationId;
            this.cache = cache;
            this.state = state;
        }

        MasterChannel channel() {
            return channel;
        }

        AssociationId associationId() {
            return associationId;
        }

        Map<String, Map<Integer, String>> cache() {
            return cache;
        }

        AtomicReference<ClientState> state() {
            return state;
        }

        void close() {
            try {
                channel.shutdown();
            } catch (Exception e) {
                // best effort; the native runtime owns the channel resources
            }
            runtime.shutdown();
        }
    }

    /**
     * Caches the latest value of each DNP3 point type/index from a poll response, and signals
     * the optional latch when the response fragment ends.
     */
    private static final class CacheReadHandler implements ReadHandler {

        private final Map<String, Map<Integer, String>> cache;
        private final CountDownLatch latch;

        CacheReadHandler(Map<String, Map<Integer, String>> cache, CountDownLatch latch) {
            this.cache = cache;
            this.latch = latch;
        }

        @Override
        public void handleBinaryInput(HeaderInfo info, List<BinaryInput> values) {
            cache(POINT_TYPE_BINARY_INPUT, values, value -> String.valueOf(value.value));
        }

        @Override
        public void handleAnalogInput(HeaderInfo info, List<AnalogInput> values) {
            cache(POINT_TYPE_ANALOG_INPUT, values, value -> String.valueOf(value.value));
        }

        @Override
        public void handleCounter(HeaderInfo info, List<Counter> values) {
            cache(POINT_TYPE_COUNTER, values, value -> String.valueOf(value.value.longValue()));
        }

        @Override
        public void handleDoubleBitBinaryInput(HeaderInfo info, List<DoubleBitBinaryInput> values) {
            cache(POINT_TYPE_DOUBLE_BIT, values, value -> String.valueOf(value.value));
        }

        @Override
        public void handleBinaryOutputStatus(HeaderInfo info, List<BinaryOutputStatus> values) {
            cache(POINT_TYPE_BINARY_OUTPUT, values, value -> String.valueOf(value.value));
        }

        @Override
        public void handleAnalogOutputStatus(HeaderInfo info, List<AnalogOutputStatus> values) {
            cache(POINT_TYPE_ANALOG_OUTPUT, values, value -> String.valueOf(value.value));
        }

        @Override
        public void endFragment(ReadType readType, ResponseHeader header) {
            if (Objects.nonNull(latch)) {
                latch.countDown();
            }
        }

        private <T> void cache(String pointType, List<T> values, java.util.function.Function<T, String> extractor) {
            Map<Integer, String> byIndex = cache.computeIfAbsent(pointType, key -> new ConcurrentHashMap<>());
            for (T value : values) {
                int index = indexOf(value);
                byIndex.put(index, extractor.apply(value));
            }
        }

        private int indexOf(Object value) {
            if (value instanceof BinaryInput v) {
                return v.index.intValue();
            } else if (value instanceof AnalogInput v) {
                return v.index.intValue();
            } else if (value instanceof Counter v) {
                return v.index.intValue();
            } else if (value instanceof DoubleBitBinaryInput v) {
                return v.index.intValue();
            } else if (value instanceof BinaryOutputStatus v) {
                return v.index.intValue();
            } else if (value instanceof AnalogOutputStatus v) {
                return v.index.intValue();
            }
            return -1;
        }
    }

    /**
     * Supplies the current UTC time to the DNP3 stack for time-sync operations.
     */
    private static final class CurrentTimeAssociationHandler implements AssociationHandler {

        @Override
        public UtcTimestamp getCurrentTime() {
            return UtcTimestamp.valid(ULong.valueOf(System.currentTimeMillis()));
        }
    }

    /**
     * No-op association information sink; task lifecycle is surfaced through logs by the stack.
     */
    private static final class NoopAssociationInformation implements AssociationInformation {

        @Override
        public void taskStart(TaskType taskType, FunctionCode functionCode, UByte seq) {
            // no-op
        }

        @Override
        public void taskSuccess(TaskType taskType, FunctionCode functionCode, UByte seq) {
            // no-op
        }

        @Override
        public void taskFail(TaskType taskType, TaskError error) {
            // no-op
        }

        @Override
        public void unsolicitedResponse(boolean isDuplicate, UByte seq) {
            // no-op
        }
    }
}
