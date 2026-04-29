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

import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;


/**
 * Custom driver service implementation for the OPC UA driver.
 * <p>
 * This service provides OPC UA-specific device communication capabilities using the
 * Eclipse Milo OPC UA stack. It manages client connections to OPC UA servers and
 * handles read/write operations to OPC UA nodes.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    DriverMetadata driverMetadata;
    @Resource
    private DriverSenderService driverSenderService;

    /**
     * Cache of device ID to OPC UA client connections.
     */
    private Map<Long, OpcUaClient> connectMap;

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Device metadata event: deviceId: {}, operate: {}", metadataEvent.getId(), operateType);

            // Remove stale connection when device is updated or deleted
            if (MetadataOperateTypeEnum.DELETE.equals(operateType) || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                connectMap.remove(metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Point metadata event: pointId: {}, operate: {}", metadataEvent.getId(), operateType);
        }
    }

    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        OpcUaClient client = getConnector(device.getId(), driverConfig);
        return writeValue(client, pointConfig, wValue);
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
        log.debug("OPC UA server connection info: {}", JsonUtil.toJsonString(driverConfig));
        OpcUaClient opcUaClient = connectMap.get(deviceId);
        if (Objects.isNull(opcUaClient)) {
            String host = driverConfig.get("host").getValue(String.class);
            int port = driverConfig.get("port").getValue(Integer.class);
            String path = driverConfig.get("path").getValue(String.class);
            String url = String.format("opc.tcp://%s:%s%s", host, port, path);
            try {
                opcUaClient = OpcUaClient.create(
                        url,
                        endpoints -> endpoints.stream().findFirst(),
                        configBuilder -> configBuilder
                                // Use anonymous authentication
                                .setIdentityProvider(new AnonymousProvider())
                                // Set request timeout to 5000 ms
                                .setRequestTimeout(Unsigned.uint(5000))
                                .build()
                );
                connectMap.put(deviceId, opcUaClient);
            } catch (UaException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Failed to connect OPC UA client: {}", e.getMessage(), e);
                throw new ConnectorException(e.getMessage());
            }
        }
        return opcUaClient;
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
    private String readValue(OpcUaClient client, Map<String, AttributeBO> pointConfig) {
        try {
            NodeId nodeId = getNode(pointConfig);
            client.connect().get();
            CompletableFuture<String> value = new CompletableFuture<>();
            client.readValue(0.0, TimestampsToReturn.Both, nodeId).thenAccept(dataValue -> value.complete(dataValue.getValue().getValue().toString()));
            return value.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Read opc ua value error: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new ReadPointException(e.getMessage());
        } catch (ExecutionException | TimeoutException e) {
            log.error("Read opc ua value error: {}", e.getMessage(), e);
            throw new ReadPointException(e.getMessage());
        }
    }

    /**
     * Write a value to an OPC UA node.
     *
     * @param client      connected OPC UA client
     * @param pointConfig point configuration (namespace, tag)
     * @param wValue      value to write
     * @return true if the write succeeded
     * @throws WritePointException if writing fails
     */
    private boolean writeValue(OpcUaClient client, Map<String, AttributeBO> pointConfig, WValue wValue) {
        try {
            NodeId nodeId = getNode(pointConfig);
            client.connect().get();
            return writeNode(client, nodeId, wValue);
        } catch (InterruptedException e) {
            log.error("Write opc ua value error: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new WritePointException(e.getMessage());
        } catch (ExecutionException e) {
            log.error("Write opc ua value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * Write a typed value to an OPC UA node via DataValue/Variant.
     * <p>Supports INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING.
     *
     * @param client connected OPC UA client
     * @param nodeId target node identifier
     * @param wValue value and type to write
     * @return true if the server reported a good status
     */
    private boolean writeNode(OpcUaClient client, NodeId nodeId, WValue wValue) throws ExecutionException, InterruptedException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(wValue.getType().getCode());
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + wValue.getType());
        }

        CompletableFuture<StatusCode> status = new CompletableFuture<>();
        switch (valueType) {
            case INT:
                int intValue = wValue.getValue(Integer.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(intValue)));
                break;
            case LONG:
                long longValue = wValue.getValue(Long.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(longValue)));
                break;
            case FLOAT:
                float floatValue = wValue.getValue(Float.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(floatValue)));
                break;
            case DOUBLE:
                double doubleValue = wValue.getValue(Double.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(doubleValue)));
                break;
            case BOOLEAN:
                boolean booleanValue = wValue.getValue(Boolean.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(booleanValue)));
                break;
            case STRING:
                status = client.writeValue(nodeId, new DataValue(new Variant(wValue.getValue())));
                break;
            default:
                break;
        }

        if (Objects.nonNull(status) && Objects.nonNull(status.get())) {
            return status.get().getValue() > 0;
        }
        return false;
    }

}
