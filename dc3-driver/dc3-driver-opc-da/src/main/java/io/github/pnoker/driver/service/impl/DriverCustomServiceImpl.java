/*
 * Copyright 2016-present the original author or authors.
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

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.entity.driver.AttributeInfo;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.driver.sdk.DriverContext;
import io.github.pnoker.driver.sdk.service.DriverCustomService;
import io.github.pnoker.driver.sdk.service.DriverSenderService;
import io.github.pnoker.driver.sdk.utils.DriverUtil;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static io.github.pnoker.driver.sdk.utils.DriverUtil.attribute;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverSenderService driverSenderService;

    /**
     * Opc Da Server Map
     */
    private Map<String, Server> connectMap;

    @Override
    public void initial() {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
        !!!
        你可以在此处执行一些特定的初始化逻辑，驱动在启动的时候会自动执行该方法。
        */
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
        !!!
        上传设备状态，可自行灵活拓展，不一定非要在schedule()接口中实现，你可以：
        - 在read中实现设备状态的判断；
        - 在自定义定时任务中实现设备状态的判断；
        - 通过某种判断机制实现设备状态的判断。

        最后通过 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理，其中设备状态（StatusEnum）：
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
        driverContext.getDriverMetadata().getDeviceMap().keySet().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE));
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
         */
        Server server = getConnector(device.getId(), driverInfo);
        return readValue(server, pointInfo);
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
         */
        Server server = getConnector(device.getId(), driverInfo);
        return writeValue(server, pointInfo, value);
    }

    /**
     * 获取 Opc Da Server
     *
     * @param deviceId   设备ID
     * @param driverInfo 驱动信息
     * @return Server
     */
    private Server getConnector(String deviceId, Map<String, AttributeInfo> driverInfo) {
        log.debug("Opc Da Server Connection Info {}", JsonUtil.toJsonString(driverInfo));
        Server server = connectMap.get(deviceId);
        if (ObjectUtil.isNull(server)) {
            String host = attribute(driverInfo, "host");
            String clsId = attribute(driverInfo, "clsId");
            String user = attribute(driverInfo, "username");
            String password = attribute(driverInfo, "password");
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
     * @param server    Server
     * @param pointInfo Point Attribute Config Map
     * @return Item
     * @throws NotConnectedException   NotConnectedException
     * @throws JIException             JIException
     * @throws UnknownHostException    UnknownHostException
     * @throws DuplicateGroupException DuplicateGroupException
     * @throws AddFailedException      AddFailedException
     */
    public Item getItem(Server server, Map<String, AttributeInfo> pointInfo) throws NotConnectedException, JIException, UnknownHostException, DuplicateGroupException, AddFailedException {
        Group group;
        String groupName = attribute(pointInfo, "group");
        try {
            group = server.findGroup(groupName);
        } catch (UnknownGroupException e) {
            group = server.addGroup(groupName);
        }
        return group.addItem(attribute(pointInfo, "tag"));
    }

    /**
     * 获取 OpcDa 值
     *
     * @param server    OpcDa Server
     * @param pointInfo 位号信息
     * @return Item Value
     */
    private String readValue(Server server, Map<String, AttributeInfo> pointInfo) {
        try {
            Item item = getItem(server, pointInfo);
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
     * @return String Value
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
     * @param server    OpcDa Server
     * @param pointInfo 位号信息
     * @param value     写入值
     * @return 是否写入
     */
    private boolean writeValue(Server server, Map<String, AttributeInfo> pointInfo, AttributeInfo value) {
        try {
            Item item = getItem(server, pointInfo);
            return writeItem(item, value);
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
     * @param item  OpcDa Item
     * @param value 写入值
     * @throws JIException OpcDa JIException
     */
    private boolean writeItem(Item item, AttributeInfo value) throws JIException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(value.getType().getCode());
        if (ObjectUtil.isNull(valueType)) {
            throw new IllegalArgumentException("Unsupported type of " + value.getType());
        }

        int writeResult = 0;
        switch (valueType) {
            case SHORT:
                short shortValue = DriverUtil.value(value.getType().getCode(), value.getValue());
                writeResult = item.write(new JIVariant(shortValue, false));
                break;
            case INT:
                int intValue = DriverUtil.value(value.getType().getCode(), value.getValue());
                writeResult = item.write(new JIVariant(intValue, false));
                break;
            case LONG:
                long longValue = DriverUtil.value(value.getType().getCode(), value.getValue());
                writeResult = item.write(new JIVariant(longValue, false));
                break;
            case FLOAT:
                float floatValue = DriverUtil.value(value.getType().getCode(), value.getValue());
                writeResult = item.write(new JIVariant(floatValue, false));
                break;
            case DOUBLE:
                double doubleValue = DriverUtil.value(value.getType().getCode(), value.getValue());
                writeResult = item.write(new JIVariant(doubleValue, false));
                break;
            case BOOLEAN:
                boolean booleanValue = DriverUtil.value(value.getType().getCode(), value.getValue());
                writeResult = item.write(new JIVariant(booleanValue, false));
                break;
            case STRING:
                writeResult = item.write(new JIVariant(value, false));
                break;
            default:
                break;
        }
        return writeResult > 0;
    }

}
