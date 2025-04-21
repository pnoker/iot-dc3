/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * 驱动自定义服务实现类
 *
 * @author pnoker
 * @version 2025.2.2
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
         * 驱动初始化逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 驱动启动时会自动执行该方法，您可以在此处执行特定的初始化操作。
         *
         */
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        /*
         * 设备状态上传逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 设备状态的上传可以根据具体需求灵活实现，以下是一些常见的实现方式：
         * - 在 `read` 方法中根据读取的数据判断设备状态；
         * - 在自定义的定时任务中定期检查设备状态；
         * - 根据特定的业务逻辑或事件触发设备状态的判断。
         *
         * 最终通过 {@link DriverSenderService#deviceStatusSender(Long, DeviceStatusEnum)} 接口将设备状态提交给 SDK 管理。
         * 设备状态枚举 {@link DeviceStatusEnum} 包含以下状态：
         * - ONLINE: 设备在线
         * - OFFLINE: 设备离线
         * - MAINTAIN: 设备维护中
         * - FAULT: 设备故障
         *
         * 在以下示例中，所有设备的状态被设置为 {@link DeviceStatusEnum#ONLINE}，并设置状态的有效期为 25 {@link TimeUnit#SECONDS}。
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * 接收驱动、设备、位号元数据的新增、更新、删除事件。
         *
         * 元数据类型: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT)
         * 元数据操作类型: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
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
         * 读取位号值逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 1. 通过设备ID和驱动配置获取Opc Da Server连接。
         * 2. 根据位号配置读取对应的位号值。
         * 3. 将读取到的值封装为RValue对象并返回。
         */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * 写入位号值逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 1. 通过设备ID和驱动配置获取Opc Da Server连接。
         * 2. 根据位号配置和写入值，将值写入对应的位号。
         * 3. 返回写入操作是否成功。
         */
        Server server = getConnector(device.getId(), driverConfig);
        return writeValue(server, pointConfig, wValue);
    }

    /**
     * 获取 OPC DA 服务器连接
     * <p>
     * 根据设备ID和驱动配置获取对应的 OPC DA 服务器连接。如果连接不存在，则创建新的连接并缓存。
     *
     * @param deviceId     设备ID，用于标识设备对应的 OPC DA 服务器连接
     * @param driverConfig 驱动配置，包含 OPC DA 服务器的连接信息（如主机地址、CLSID、用户名、密码等）
     * @return Server 返回与设备ID对应的 OPC DA 服务器连接
     * @throws ConnectorException 如果连接 OPC DA 服务器时发生异常，则抛出此异常
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
     * 获取 OPC DA 服务器中的 Item 对象
     * <p>
     * 根据位号配置中的组名和标签名，从指定的 OPC DA 服务器中获取对应的 Item 对象。
     * 如果组不存在，则创建新的组；如果组已存在，则直接使用该组。
     *
     * @param server      已连接的 OPC DA 服务器实例
     * @param pointConfig 位号配置，包含组名和标签名等信息
     * @return Item       返回与位号配置对应的 Item 对象
     * @throws NotConnectedException   如果 OPC DA 服务器未连接，则抛出此异常
     * @throws JIException             如果与 OPC DA 服务器通信时发生错误，则抛出此异常
     * @throws UnknownHostException    如果无法解析 OPC DA 服务器的主机地址，则抛出此异常
     * @throws DuplicateGroupException 如果尝试添加已存在的组，则抛出此异常
     * @throws AddFailedException      如果添加组或 Item 失败，则抛出此异常
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
     * 从 OPC DA 服务器读取位号值
     * <p>
     * 该方法通过给定的 OPC DA 服务器和位号配置，获取对应的 Item 对象，并读取其值。
     * 如果在读取过程中发生异常，将断开服务器连接并抛出 {@link ReadPointException}。
     *
     * @param server      已连接的 OPC DA 服务器实例
     * @param pointConfig 位号配置，包含组名和标签名等信息
     * @return String     返回读取到的位号值
     * @throws ReadPointException 如果读取位号值时发生异常，则抛出此异常
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
     * 读取 OPC DA 位号值
     * <p>
     * 该方法通过给定的 OPC DA Item 对象，读取其值并根据数据类型进行转换。
     * 支持的数据类型包括：短整型 (VT_I2)、整型 (VT_I4)、长整型 (VT_I8)、浮点型 (VT_R4)、双精度浮点型 (VT_R8)、布尔型 (VT_BOOL)、字符串型 (VT_BSTR)。
     * 如果数据类型不在上述范围内，则返回对象的字符串表示。
     *
     * @param item OPC DA Item 对象，包含要读取的位号值
     * @return String 返回读取到的位号值的字符串表示
     * @throws JIException 如果与 OPC DA 服务器通信时发生错误，则抛出此异常
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
     * 向 OPC DA 服务器写入位号值
     * <p>
     * 该方法通过给定的 OPC DA 服务器、位号配置和写入值，获取对应的 Item 对象，并将值写入该 Item。
     * 如果在写入过程中发生异常，将断开服务器连接并抛出 {@link WritePointException}。
     *
     * @param server      已连接的 OPC DA 服务器实例
     * @param pointConfig 位号配置，包含组名和标签名等信息
     * @param wValue      写入值，包含要写入的数据类型和值
     * @return boolean    返回写入操作是否成功
     * @throws WritePointException 如果写入位号值时发生异常，则抛出此异常
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
     * 向 OPC DA Item 写入值
     * <p>
     * 该方法根据写入值的数据类型，将值转换为相应的 JIVariant 对象，并写入到指定的 OPC DA Item 中。
     * 支持的数据类型包括：短整型 (SHORT)、整型 (INT)、长整型 (LONG)、浮点型 (FLOAT)、双精度浮点型 (DOUBLE)、布尔型 (BOOLEAN)、字符串型 (STRING)。
     * 如果数据类型不支持，将抛出 {@link UnSupportException} 异常。
     *
     * @param item   OPC DA Item 对象，表示要写入的目标位号
     * @param wValue 写入值对象，包含要写入的数据类型和值
     * @return boolean 返回写入操作是否成功，成功返回 true，失败返回 false
     * @throws JIException        如果与 OPC DA 服务器通信时发生错误，则抛出此异常
     * @throws UnSupportException 如果写入值的数据类型不支持，则抛出此异常
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
