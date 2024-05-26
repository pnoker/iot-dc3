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
 * @author pnoker
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
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        你可以在此处执行一些特定的初始化逻辑, 驱动在启动的时候会自动执行该方法。
        */
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        上传设备状态, 可自行灵活拓展, 不一定非要在schedule()接口中实现, 你可以: 
        - 在read中实现设备状态的判断;
        - 在自定义定时任务中实现设备状态的判断;
        - 根据某种判断机制实现设备状态的判断。

        最后根据 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理, 其中设备状态(StatusEnum):
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        接收驱动, 设备, 位号元数据新增, 更新, 删除都会触发改事件
        提供元数据类型: MetadataTypeEnum(DRIVER, DEVICE, POINT)
        提供元数据操作类型: MetadataOperateTypeEnum(ADD, DELETE, UPDATE)
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Device metadata event: deviceId: {}, operate: {}", metadataEvent.getId(), operateType);

            // 当设备更新或者删除时，移除连接句柄
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
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
         */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
         */
        Server server = getConnector(device.getId(), driverConfig);
        return writeValue(server, pointConfig, wValue);
    }

    /**
     * 获取 Opc Da Server
     *
     * @param deviceId     设备ID
     * @param driverConfig 驱动信息
     * @return Server
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
     * 获取 Opc Da Item
     *
     * @param server      Server
     * @param pointConfig 位号属性配置 Map
     * @return Item
     * @throws NotConnectedException   NotConnectedException
     * @throws JIException             JIException
     * @throws UnknownHostException    UnknownHostException
     * @throws DuplicateGroupException DuplicateGroupException
     * @throws AddFailedException      AddFailedException
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
     * 获取 OpcDa 值
     *
     * @param server      OpcDa Server
     * @param pointConfig 位号信息
     * @return Item Value
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
     * 读取 Opc Da 位号值
     *
     * @param item Opc Item
     * @return R of String Value
     * @throws JIException JIException
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
     * 写入 OpcDa 值
     *
     * @param server      OpcDa Server
     * @param pointConfig 位号信息
     * @param wValue      写入值
     * @return 是否写入
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
     * Write Opc Da Item
     *
     * @param item   OpcDa Item
     * @param wValue 写入值
     * @throws JIException OpcDa JIException
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
