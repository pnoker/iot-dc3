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

    private Map<Long, OpcUaClient> connectMap;

    /**
     * Initializes the OPC UA driver.
     * <p>
     * This method is called when the driver starts. It initializes the connection map
     * used to manage OPC UA client instances. Override this method to implement
     * custom initialization logic.
     * </p>
     */
    @Override
    public void initial() {
        /*
         * Driver initialization logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * This method is automatically executed when the driver starts, and you can perform specific initialization operations here.
         *
         */
        connectMap = new ConcurrentHashMap<>(16);
    }

    /**
     * Scheduled task to report device status.
     * <p>
     * This method is called periodically to update device status. By default,
     * all devices are reported as ONLINE with a 25-second validity period.
     * Override this method to implement custom status reporting logic.
     * </p>
     */
    @Override
    public void schedule() {
        /*
         * Device status upload logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * Device status upload can be flexibly implemented based on specific requirements. Here are some common approaches:
         * - Determine device status based on read data in the `read` method;
         * - Periodically check device status in a custom scheduled task;
         * - Trigger device status judgment based on specific business logic or events.
         *
         * Finally, submit the device status to the SDK management through the {@link DriverSenderService#deviceStatusSender(Long, DeviceStatusEnum)} interface.
         * The device status enumeration {@link DeviceStatusEnum} includes the following states:
         * - ONLINE: Device online
         * - OFFLINE: Device offline
         * - MAINTAIN: Device under maintenance
         * - FAULT: Device fault
         *
         * In the following example, all devices are set to {@link DeviceStatusEnum#ONLINE}, with a status validity period of 25 {@link TimeUnit#SECONDS}.
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    /**
     * Handles metadata change events for drivers, devices, and points.
     * <p>
     * This method is called when metadata is created, updated, or deleted.
     * For device update/delete events, it removes the cached OPC UA client connection
     * to force reconnection with updated configuration.
     * </p>
     *
     * @param metadataEvent the metadata event containing type, operation, and ID information
     */
    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * Receive metadata events for driver, device, and point creation, update, and deletion.
         *
         * Metadata type: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT)
         * Metadata operation type: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Device metadata event: deviceId: {}, operate: {}", metadataEvent.getId(), operateType);

            // When the device is updated or deleted, remove the corresponding connection handle
            if (MetadataOperateTypeEnum.DELETE.equals(operateType) || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                connectMap.remove(metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Point metadata event: pointId: {}, operate: {}", metadataEvent.getId(), operateType);
        }
    }

    /**
     * Reads data from an OPC UA device point.
     * <p>
     * This method obtains or creates an OPC UA client connection, then reads the
     * value from the specified OPC UA node. The node is identified by namespace
     * and tag from point configuration.
     * </p>
     *
     * @param driverConfig driver configuration attributes (host, port, path)
     * @param pointConfig point configuration attributes (namespace, tag)
     * @param device the device to read from
     * @param point the point to read
     * @return the read value wrapped in an RValue object
     */
    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        /*
         * Read OPC UA point value
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * 1. Obtain the OPC UA client connection through device ID and driver configuration.
         * 2. Read the OPC UA node value using point configuration.
         * 3. Encapsulate the read value as an RValue object and return it.
         */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig));

    }

    /**
     * Writes data to an OPC UA device point.
     * <p>
     * This method obtains or creates an OPC UA client connection, then writes the
     * value to the specified OPC UA node. The data type is automatically determined
     * from the write value's type flag.
     * </p>
     *
     * @param driverConfig driver configuration attributes (host, port, path)
     * @param pointConfig point configuration attributes (namespace, tag)
     * @param device the device to write to
     * @param point the point to write
     * @param wValue the value containing the data to write
     * @return true if the write operation succeeded, false otherwise
     */
    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * Write OPC UA point value
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * 1. Obtain the OPC UA client connection through device ID and driver configuration.
         * 2. Write data to the OPC UA node using point configuration and write value.
         * 3. Return whether the write operation is successful.
         */
        OpcUaClient client = getConnector(device.getId(), driverConfig);
        return writeValue(client, pointConfig, wValue);
    }

    /**
     * Get OPC UA client connection
     *
     * @param deviceId     Device ID, used to identify a unique device connection
     * @param driverConfig Driver configuration info, contains parameters required to connect to the OPC UA server
     * @return OpcUaClient Returns the OPC UA client instance associated with the specified device
     * @throws ConnectorException Thrown if connecting to the OPC UA server fails
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
     * Get OPC UA node from point configuration
     *
     * @param pointConfig point configuration, contains namespace and tag
     * @return OPC UA node identifier
     */
    private NodeId getNode(Map<String, AttributeBO> pointConfig) {
        int namespace = pointConfig.get("namespace").getValue(Integer.class);
        String tag = pointConfig.get("tag").getValue(String.class);
        return new NodeId(namespace, tag);
    }

    /**
     * Read the value of an OPC UA node
     *
     * @param client      OPC UA client instance
     * @param pointConfig Point configuration info
     * @return The read node value
     * @throws ReadPointException Thrown if the read operation fails
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
     * Write the value to an OPC UA node
     *
     * @param client      OPC UA client instance
     * @param pointConfig point configuration info
     * @param wValue      value to write
     * @return whether the write operation is successful
     * @throws WritePointException thrown if the write operation fails
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
     * Write value to OPC UA node
     *
     * @param client OPC UA client instance
     * @param nodeId OPC UA node identifier
     * @param wValue value to be written
     * @return whether the write operation is successful
     * @throws ExecutionException   thrown when the write operation fails
     * @throws InterruptedException thrown when the write operation is interrupted
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
