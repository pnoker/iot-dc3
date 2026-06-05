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
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.driver.key.KeyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Custom driver service implementation for the OPC UA driver.
 * <p>
 * This service provides OPC UA-specific device communication capabilities using the
 * Eclipse Milo OPC UA stack. It manages client connections to OPC UA servers and handles
 * read/write operations to OPC UA nodes.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpcUaDriverCustomServiceImpl implements DriverCustomService {

    /**
     * Cache of device ID to OPC UA client connections.
     */
    private static final long CONNECT_TIMEOUT_SECONDS = 5;
    private static final long REQUEST_TIMEOUT_MS = 5000;
    private static final long READ_TIMEOUT_SECONDS = 1;
    private static final long WRITE_TIMEOUT_SECONDS = 1;
    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;
    private Map<Long, OpcUaClient> connectMap;

    /**
     * KeyLoader for OPC UA client certificate management.
     * Lazy-initialized on first health check to avoid blocking startup.
     */
    private volatile KeyLoader keyLoader;

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
        try {
            keyLoader = new KeyLoader().load(
                    java.nio.file.Path.of(System.getProperty("user.dir"), "dc3", "opc-ua"));
        } catch (Exception e) {
            log.warn("OPC UA KeyLoader initialization failed, falling back to anonymous auth: {}", e.getMessage());
            keyLoader = null;
        }
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        try {
            OpcUaClient client = getConnector(device.getId(), driverConfig);
            if (client != null) {
                // Connect is idempotent — returns immediately when already connected
                client.connect().get(1, TimeUnit.SECONDS);
                return DeviceHealthState.online();
            }
        } catch (Exception e) {
            log.debug("Health check failed for device {}", device.getId(), e);
        }
        return DeviceHealthState.offline();
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=" + driverCode + ", metadataType={}, operateType={}, deviceId={}",
                    metadataType, operateType, metadataEvent.getId());

            // Remove stale connection when device is updated or deleted
            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                OpcUaClient removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    removed.disconnect();
                }
                log.info("Driver connection invalidated, protocol=" + driverCode + ", deviceId={}, operateType={}, removed={}",
                        metadataEvent.getId(), operateType, Objects.nonNull(removed));
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=" + driverCode + ", metadataType={}, operateType={}, pointId={}",
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        OpcUaClient client = getConnector(device.getId(), driverConfig);
        return new ReadPointValue(device, point, readValue(device.getId(), client, pointConfig));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        OpcUaClient client = getConnector(device.getId(), driverConfig);
        return writeValue(device.getId(), client, pointConfig, writePointValue);
    }

    /**
     * Get or create an OPC UA client for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration (host, port, path)
     * @return cached or newly created OpcUaClient
     * @throws ConnectorException if client creation fails
     */
    private OpcUaClient getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String host = driverConfig.get("host").getValue(String.class);
            int port = driverConfig.get("port").getValue(Integer.class);
            String path = driverConfig.get("path").getValue(String.class);
            String url = String.format("opc.tcp://%s:%s%s", host, port, path);
            log.debug("Driver connection creating, protocol=" + driverCode + ", deviceId={}, host={}, port={}, path={}", deviceId,
                    host, port, path);
            try {
                // Prefer certificate-based auth when KeyLoader is available
                KeyLoader loader = this.keyLoader;
                OpcUaClient opcUaClient = OpcUaClient.create(url, endpoints -> endpoints.stream().findFirst(),
                        configBuilder -> {
                            configBuilder
                                    .setRequestTimeout(Unsigned.uint(REQUEST_TIMEOUT_MS))
                                    .setApplicationName(LocalizedText.english("IoT DC3 OPC UA Driver"))
                                    .setApplicationUri("urn:dc3:opc:ua:client");
                            if (loader != null && loader.getClientCertificate() != null
                                    && loader.getClientKeyPair() != null) {
                                configBuilder
                                        .setCertificate(loader.getClientCertificate())
                                        .setKeyPair(loader.getClientKeyPair())
                                        .setIdentityProvider(new AnonymousProvider());
                            } else {
                                configBuilder
                                        .setIdentityProvider(new AnonymousProvider());
                            }
                            return configBuilder.build();
                        });
                log.info("Driver connection created, protocol=" + driverCode + ", deviceId={}, host={}, port={}, path={}", deviceId,
                        host, port, path);
                return opcUaClient;
            } catch (UaException e) {
                log.error("Driver connection failed, protocol=" + driverCode + ", deviceId={}, host={}, port={}, path={}", deviceId,
                        host, port, path, e);
                throw new ConnectorException("Driver connection failed, protocol=" + driverCode + ", deviceId={}, host={}, port={}, path={}, message={}",
                        deviceId, host, port, path, e.getMessage(), e);
            }
        });
    }

    /**
     * Build a NodeId from point configuration (namespace + tag).
     */
    private NodeId getNode(Map<String, AttributeBO> pointConfig) {
        int namespace = pointConfig.get("namespace").getValue(Integer.class);
        String tag = pointConfig.get("tag").getValue(String.class);
        return new NodeId(namespace, tag);
    }

    /**
     * Read a node value from the OPC UA server with a 1-second timeout.
     *
     * @param client      connected OPC UA client
     * @param pointConfig point configuration (namespace, tag)
     * @return the node value as a string
     * @throws ReadPointException if reading fails or times out
     */
    private String readValue(Long deviceId, OpcUaClient client, Map<String, AttributeBO> pointConfig) {
        try {
            NodeId nodeId = getNode(pointConfig);
            client.connect().get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            DataValue dataValue = client.readValue(0.0, TimestampsToReturn.Both, nodeId)
                    .get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (Objects.isNull(dataValue) || Objects.isNull(dataValue.getStatusCode())
                    || !dataValue.getStatusCode().isGood()) {
                invalidateConnector(deviceId, client);
                throw new ReadPointException("Driver point read failed, protocol=" + driverCode + ", statusCode={}",
                        Objects.nonNull(dataValue) ? dataValue.getStatusCode() : null);
            }
            Variant variant = dataValue.getValue();
            if (Objects.isNull(variant) || Objects.isNull(variant.getValue())) {
                invalidateConnector(deviceId, client);
                throw new ReadPointException("Driver point read failed, protocol=" + driverCode + ", value is null");
            }
            return String.valueOf(variant.getValue());
        } catch (InterruptedException e) {
            log.error("Driver point read interrupted, protocol={}", driverCode, e);
            Thread.currentThread().interrupt();
            invalidateConnector(deviceId, client);
            throw new ReadPointException("Driver point read interrupted, protocol=" + driverCode + ", message={}", e.getMessage(),
                    e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Driver point read failed, protocol={}", driverCode, e);
            invalidateConnector(deviceId, client);
            throw new ReadPointException("Driver point read failed, protocol=" + driverCode + ", message={}", e.getMessage(), e);
        }
    }

    /**
     * Write a value to an OPC UA node.
     *
     * @param client          connected OPC UA client
     * @param pointConfig     point configuration (namespace, tag)
     * @param writePointValue value to write
     * @return true if the write succeeded
     * @throws WritePointException if writing fails
     */
    private boolean writeValue(Long deviceId, OpcUaClient client, Map<String, AttributeBO> pointConfig, WritePointValue writePointValue) {
        try {
            NodeId nodeId = getNode(pointConfig);
            client.connect().get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return writeNode(client, nodeId, writePointValue);
        } catch (InterruptedException e) {
            log.error("Driver point write interrupted, protocol={}", driverCode, e);
            Thread.currentThread().interrupt();
            invalidateConnector(deviceId, client);
            throw new WritePointException("Driver point write interrupted, protocol=" + driverCode + ", message={}", e.getMessage(),
                    e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Driver point write failed, protocol={}", driverCode, e);
            invalidateConnector(deviceId, client);
            throw new WritePointException("Driver point write failed, protocol=" + driverCode + ", message={}", e.getMessage(), e);
        }
    }

    /**
     * Write a typed value to an OPC UA node via DataValue/Variant.
     * <p>
     * Supports INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING.
     *
     * @param client          connected OPC UA client
     * @param nodeId          target node identifier
     * @param writePointValue value and type to write
     * @return true if the server reported a good status
     */
    private boolean writeNode(OpcUaClient client, NodeId nodeId, WritePointValue writePointValue)
            throws ExecutionException, InterruptedException, TimeoutException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(writePointValue.getType().getCode());
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + writePointValue.getType());
        }

        java.util.concurrent.CompletableFuture<StatusCode> status = null;
        switch (valueType) {
            case INT:
                int intValue = writePointValue.getValue(Integer.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(intValue)));
                break;
            case LONG:
                long longValue = writePointValue.getValue(Long.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(longValue)));
                break;
            case FLOAT:
                float floatValue = writePointValue.getValue(Float.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(floatValue)));
                break;
            case DOUBLE:
                double doubleValue = writePointValue.getValue(Double.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(doubleValue)));
                break;
            case BOOLEAN:
                boolean booleanValue = writePointValue.getValue(Boolean.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(booleanValue)));
                break;
            case STRING:
                status = client.writeValue(nodeId, new DataValue(new Variant(writePointValue.getValue())));
                break;
            default:
                break;
        }

        if (Objects.nonNull(status)) {
            StatusCode statusCode = status.get(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return Objects.nonNull(statusCode) && statusCode.isGood();
        }
        return false;
    }

    private void invalidateConnector(Long deviceId, OpcUaClient client) {
        connectMap.remove(deviceId, client);
        try {
            client.disconnect();
        } catch (Exception e) {
            log.warn("Driver connection disconnect failed, protocol=" + driverCode + ", deviceId={}", deviceId, e);
        }
    }

}
