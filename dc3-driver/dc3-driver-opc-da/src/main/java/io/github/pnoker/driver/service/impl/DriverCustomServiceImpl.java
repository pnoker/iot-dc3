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
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.*;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Drive custom service implementation classes
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
     * Opc Da Server Map
     */
    private Map<Long, Server> connectMap;

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

    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        /*
         * Read point value logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * 1. Obtain the Opc Da Server connection via device ID and driver configuration.
         * 2. Read the corresponding point value according to the point configuration.
         * 3. Wrap the read value into an RValue object and return it.
         */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * Write point value logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * 1. Obtain the Opc Da Server connection via device ID and driver configuration.
         * 2. According to the point configuration and write value, write the value to the corresponding point.
         * 3. Return whether the write operation is successful.
         */
        Server server = getConnector(device.getId(), driverConfig);
        return writeValue(server, pointConfig, wValue);
    }

    /**
     * Get OPC DA Server Connection
     * <p>
     * Obtain the corresponding OPC DA server connection based on the device ID and driver configuration.
     * If the connection does not exist, create a new connection and cache it.
     *
     * @param deviceId     Device ID, used to identify the OPC DA server connection corresponding to the device
     * @param driverConfig Driver configuration, including connection information of the OPC DA server
     *                     (such as host address, CLSID, username, password, etc.)
     * @return Server      Returns the OPC DA server connection corresponding to the device ID
     * @throws ConnectorException If an exception occurs when connecting to the OPC DA server, this exception is thrown
     */
    private Server getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        log.debug("Opc Da Server Connection Info {}", JsonUtil.toJsonString(driverConfig));
        Server server = connectMap.get(deviceId);
        if (Objects.isNull(server)) {
            String host = driverConfig.get("host").getValue(String.class);
            String clsId = driverConfig.get("clsId").getValue(String.class);
            String user = driverConfig.get("username").getValue(String.class);
            String password = driverConfig.get("password").getValue(String.class);
            ConnectionInformation connectionInformation = new ConnectionInformation(host, clsId, user, password);
            server = new Server(connectionInformation, Executors.newSingleThreadScheduledExecutor());
            try {
                server.connect();
                connectMap.put(deviceId, server);
            } catch (AlreadyConnectedException | UnknownHostException | JIException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Connect opc da server error: {}", e.getMessage(), e);
                throw new ConnectorException(e.getMessage());
            }
        }
        return server;
    }

    /**
     * Get the Item object from the OPC DA server
     * <p>
     * According to the group name and tag name in the point configuration, obtain the corresponding Item object
     * from the specified OPC DA server. If the group does not exist, a new group will be created;
     * if the group already exists, it will be used directly.
     *
     * @param server      Connected OPC DA server instance
     * @param pointConfig Point configuration, including group name and tag name, etc.
     * @return Item       Returns the Item object corresponding to the point configuration
     * @throws NotConnectedException   If the OPC DA server is not connected, this exception will be thrown
     * @throws JIException             If an error occurs when communicating with the OPC DA server, this exception will be thrown
     * @throws UnknownHostException    If the host address of the OPC DA server cannot be resolved, this exception will be thrown
     * @throws DuplicateGroupException If trying to add an existing group, this exception will be thrown
     * @throws AddFailedException      If adding a group or Item fails, this exception will be thrown
     */
    public Item getItem(Server server, Map<String, AttributeBO> pointConfig) throws NotConnectedException, JIException, UnknownHostException, DuplicateGroupException, AddFailedException {
        Group group;
        String groupName = pointConfig.get("group").getValue(String.class);
        try {
            group = server.findGroup(groupName);
        } catch (UnknownGroupException e) {
            group = server.addGroup(groupName);
        }
        return group.addItem(pointConfig.get("tag").getValue(String.class));
    }

    /**
     * Read tag value from OPC DA server
     * <p>
     * This method obtains the corresponding Item object through the given OPC DA server and tag configuration,
     * and reads its value.
     * If an exception occurs during the reading process, the server connection will be disconnected and {@link ReadPointException} will be thrown.
     *
     * @param server      Connected OPC DA server instance
     * @param pointConfig Tag configuration, including group name and tag name, etc.
     * @return String     Returns the read tag value
     * @throws ReadPointException If an exception occurs when reading the tag value, this exception will be thrown
     */
    private String readValue(Server server, Map<String, AttributeBO> pointConfig) {
        try {
            Item item = getItem(server, pointConfig);
            return readItem(item);
        } catch (NotConnectedException | JIException | AddFailedException | DuplicateGroupException |
                 UnknownHostException e) {
            server.dispose();
            log.error("Read opc da value error: {}", e.getMessage(), e);
            throw new ReadPointException(e.getMessage());
        }
    }

    /**
     * Read OPC DA tag value
     * <p>
     * This method reads the value from the given OPC DA Item object and converts it according to its data type.
     * Supported data types: short (VT_I2), int (VT_I4), long (VT_I8), float (VT_R4), double (VT_R8), boolean (VT_BOOL), string (VT_BSTR).
     * If the data type is not in the above list, the string representation of the object is returned.
     *
     * @param item OPC DA Item object containing the tag value to be read
     * @return String string representation of the read tag value
     * @throws JIException thrown when communication with the OPC DA server fails
     */
    public String readItem(Item item) throws JIException {
        JIVariant jiVariant = item.read(false).getValue();
        switch (jiVariant.getType()) {
            case JIVariant.VT_I2:
                short shortValue = jiVariant.getObjectAsShort();
                return String.valueOf(shortValue);
            case JIVariant.VT_I4:
                int intValue = jiVariant.getObjectAsInt();
                return String.valueOf(intValue);
            case JIVariant.VT_I8:
                long longValue = jiVariant.getObjectAsLong();
                return String.valueOf(longValue);
            case JIVariant.VT_R4:
                float floatValue = jiVariant.getObjectAsFloat();
                return String.valueOf(floatValue);
            case JIVariant.VT_R8:
                double doubleValue = jiVariant.getObjectAsDouble();
                return String.valueOf(doubleValue);
            case JIVariant.VT_BOOL:
                boolean boolValue = jiVariant.getObjectAsBoolean();
                return String.valueOf(boolValue);
            case JIVariant.VT_BSTR:
                return jiVariant.getObjectAsString2();
            default:
                return jiVariant.getObject().toString();
        }
    }

    /**
     * Write value to OPC DA Server
     * <p>
     * This method obtains the corresponding Item object through the given OPC DA server, point configuration and write value,
     * and writes the value into the Item.
     * If an exception occurs during the writing process, the server connection will be disconnected and {@link WritePointException} will be thrown.
     *
     * @param server      Connected OPC DA server instance
     * @param pointConfig Point configuration, including group name, tag name, etc.
     * @param wValue      Write value, including data type and value to be written
     * @return boolean    Returns whether the write operation is successful
     * @throws WritePointException If an exception occurs when writing the point value, this exception will be thrown
     */
    private boolean writeValue(Server server, Map<String, AttributeBO> pointConfig, WValue wValue) {
        try {
            Item item = getItem(server, pointConfig);
            return writeItem(item, wValue);
        } catch (NotConnectedException | AddFailedException | DuplicateGroupException | UnknownHostException |
                 JIException e) {
            server.dispose();
            log.error("Write opc da value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * Write value to OPC DA Item
     * <p>
     * According to the data type of the write value, convert it to the corresponding JIVariant object and write it into the specified OPC DA Item.
     * Supported data types: SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING.
     * If the data type is not supported, an {@link UnSupportException} will be thrown.
     *
     * @param item   OPC DA Item object, representing the target point to be written
     * @param wValue write value object, containing the data type and value to be written
     * @return boolean returns whether the write operation is successful, true for success, false for failure
     * @throws JIException        if an error occurs while communicating with the OPC DA server, this exception will be thrown
     * @throws UnSupportException if the data type of the write value is not supported, this exception will be thrown
     */
    private boolean writeItem(Item item, WValue wValue) throws JIException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(wValue.getType().getCode());
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + wValue.getType());
        }

        int writeResult = 0;
        switch (valueType) {
            case SHORT:
                short shortValue = wValue.getValue(Short.class);
                writeResult = item.write(new JIVariant(shortValue, false));
                break;
            case INT:
                int intValue = wValue.getValue(Integer.class);
                writeResult = item.write(new JIVariant(intValue, false));
                break;
            case LONG:
                long longValue = wValue.getValue(Long.class);
                writeResult = item.write(new JIVariant(longValue, false));
                break;
            case FLOAT:
                float floatValue = wValue.getValue(Float.class);
                writeResult = item.write(new JIVariant(floatValue, false));
                break;
            case DOUBLE:
                double doubleValue = wValue.getValue(Double.class);
                writeResult = item.write(new JIVariant(doubleValue, false));
                break;
            case BOOLEAN:
                boolean booleanValue = wValue.getValue(Boolean.class);
                writeResult = item.write(new JIVariant(booleanValue, false));
                break;
            case STRING:
                writeResult = item.write(new JIVariant(wValue.getValue(), false));
                break;
            default:
                break;
        }
        return writeResult > 0;
    }

}
