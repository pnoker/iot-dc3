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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.AddFailedException;
import org.openscada.opc.lib.da.DuplicateGroupException;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.Server;
import org.openscada.opc.lib.da.UnknownGroupException;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Custom driver service implementation for the OPC DA driver.
 * <p>
 * Manages OPC DA server connections via DCOM, reads/writes tag values through groups and
 * items with support for multiple data types.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpcDaDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;

    private final DriverSenderService driverSenderService;

    /**
     * Cache of device ID to OPC DA server connections.
     */
    private Map<Long, Server> connectMap;

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=opcDa, metadataType={}, operateType={}, deviceId={}",
                    metadataType, operateType, metadataEvent.getId());

            // Remove stale connection when device is updated or deleted
            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                Server removed = connectMap.remove(metadataEvent.getId());
                log.info("Driver connection invalidated, protocol=opcDa, deviceId={}, operateType={}, removed={}",
                        metadataEvent.getId(), operateType, Objects.nonNull(removed));
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=opcDa, metadataType={}, operateType={}, pointId={}",
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        return new ReadPointValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        Server server = getConnector(device.getId(), driverConfig);
        return writeValue(server, pointConfig, writePointValue);
    }

    /**
     * Get or create an OPC DA server connection for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration (host, clsId, username, password)
     * @return cached or newly connected OPC DA Server
     * @throws ConnectorException if the connection fails
     */
    private Server getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        Server server = connectMap.get(deviceId);
        if (Objects.isNull(server)) {
            String host = driverConfig.get("host").getValue(String.class);
            String clsId = driverConfig.get("clsId").getValue(String.class);
            String user = driverConfig.get("username").getValue(String.class);
            String password = driverConfig.get("password").getValue(String.class);
            log.debug("Driver connection creating, protocol=opcDa, deviceId={}, host={}, clsId={}, usernamePresent={}",
                    deviceId, host, clsId, Objects.nonNull(user));
            ConnectionInformation connectionInformation = new ConnectionInformation(host, clsId, user, password);
            server = new Server(connectionInformation, Executors.newSingleThreadScheduledExecutor());
            try {
                server.connect();
                connectMap.put(deviceId, server);
                log.info("Driver connection established, protocol=opcDa, deviceId={}, host={}, clsId={}", deviceId,
                        host, clsId);
            } catch (AlreadyConnectedException | UnknownHostException | JIException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Driver connection failed, protocol=opcDa, deviceId={}, host={}, clsId={}", deviceId, host,
                        clsId, e);
                throw new ConnectorException("Driver connection failed, protocol=opcDa, deviceId={}, host={}, clsId={}, message={}",
                        deviceId, host, clsId, e.getMessage(), e);
            }
        }
        return server;
    }

    /**
     * Resolve an OPC DA Item from the server, creating the group if needed.
     *
     * @param server      connected OPC DA server
     * @param pointConfig point configuration (group, tag)
     * @return the resolved Item
     */
    public Item getItem(Server server, Map<String, AttributeBO> pointConfig) throws NotConnectedException, JIException,
            UnknownHostException, DuplicateGroupException, AddFailedException {
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
     * Read a tag value from the OPC DA server.
     *
     * @param server      active OPC DA server connection
     * @param pointConfig tag configuration (group, tag)
     * @return the tag value as a string
     * @throws ReadPointException if reading fails (server is disposed on error)
     */
    private String readValue(Server server, Map<String, AttributeBO> pointConfig) {
        try {
            Item item = getItem(server, pointConfig);
            return readItem(item);
        } catch (NotConnectedException | JIException | AddFailedException | DuplicateGroupException
                 | UnknownHostException e) {
            server.dispose();
            log.error("Driver point read failed, protocol=opcDa", e);
            throw new ReadPointException("Driver point read failed, protocol=opcDa, message={}", e.getMessage(), e);
        }
    }

    /**
     * Read and convert an OPC DA Item value to string.
     * <p>
     * Supports VT_I2, VT_I4, VT_I8, VT_R4, VT_R8, VT_BOOL, VT_BSTR; falls back to
     * toString().
     *
     * @param item the OPC DA Item to read
     * @return the value as a string
     * @throws JIException if DCOM communication fails
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
     * Write a value to an OPC DA tag.
     *
     * @param server          active OPC DA server connection
     * @param pointConfig     tag configuration (group, tag)
     * @param writePointValue value to write
     * @return true if the write succeeded
     * @throws WritePointException if writing fails (server is disposed on error)
     */
    private boolean writeValue(Server server, Map<String, AttributeBO> pointConfig, WritePointValue writePointValue) {
        try {
            Item item = getItem(server, pointConfig);
            return writeItem(item, writePointValue);
        } catch (NotConnectedException | AddFailedException | DuplicateGroupException | UnknownHostException
                 | JIException e) {
            server.dispose();
            log.error("Driver point write failed, protocol=opcDa", e);
            throw new WritePointException("Driver point write failed, protocol=opcDa, message={}", e.getMessage(), e);
        }
    }

    /**
     * Write a typed value to an OPC DA Item via JIVariant.
     * <p>
     * Supports SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING.
     *
     * @param item            target OPC DA Item
     * @param writePointValue value and type to write
     * @return true if the server reported success
     * @throws JIException        if DCOM communication fails
     * @throws UnSupportException if the value type is unsupported
     */
    private boolean writeItem(Item item, WritePointValue writePointValue) throws JIException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(writePointValue.getType().getCode());
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + writePointValue.getType());
        }

        int writeResult = 0;
        switch (valueType) {
            case SHORT:
                short shortValue = writePointValue.getValue(Short.class);
                writeResult = item.write(new JIVariant(shortValue, false));
                break;
            case INT:
                int intValue = writePointValue.getValue(Integer.class);
                writeResult = item.write(new JIVariant(intValue, false));
                break;
            case LONG:
                long longValue = writePointValue.getValue(Long.class);
                writeResult = item.write(new JIVariant(longValue, false));
                break;
            case FLOAT:
                float floatValue = writePointValue.getValue(Float.class);
                writeResult = item.write(new JIVariant(floatValue, false));
                break;
            case DOUBLE:
                double doubleValue = writePointValue.getValue(Double.class);
                writeResult = item.write(new JIVariant(doubleValue, false));
                break;
            case BOOLEAN:
                boolean booleanValue = writePointValue.getValue(Boolean.class);
                writeResult = item.write(new JIVariant(booleanValue, false));
                break;
            case STRING:
                writeResult = item.write(new JIVariant(writePointValue.getValue(), false));
                break;
            default:
                break;
        }
        return writeResult > 0;
    }

}
