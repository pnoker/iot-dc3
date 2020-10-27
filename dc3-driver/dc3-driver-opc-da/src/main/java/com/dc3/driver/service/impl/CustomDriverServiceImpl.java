/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.service.impl;

import com.alibaba.fastjson.JSON;
import com.dc3.common.constant.Common;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.CustomDriverService;
import com.dc3.common.sdk.service.DriverService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static com.dc3.common.sdk.util.DriverUtils.attribute;
import static com.dc3.common.sdk.util.DriverUtils.value;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class CustomDriverServiceImpl implements CustomDriverService {

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;

    /**
     * Opc Da Server Map
     */
    private volatile Map<Long, Server> serverMap = new HashMap<>(64);

    @Override
    public void initial() {
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) throws Exception {
        log.debug("Opc Da Read, device: {}, point: {}", JSON.toJSONString(device), JSON.toJSONString(point));
        Server server = getServer(device.getId(), driverInfo);
        try {
            Item item = getItem(server, pointInfo);
            return readItem(item);
        } finally {
            server.dispose();
        }
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) throws Exception {
        log.debug("Opc Da Write, device: {}, value: {}", JSON.toJSONString(device), JSON.toJSONString(value));
        Server server = getServer(device.getId(), driverInfo);
        try {
            Item item = getItem(server, pointInfo);
            writeItem(item, value.getType(), value.getValue());
            return true;
        } finally {
            server.dispose();
        }
    }

    @Override
    public void schedule() {

        /*
        TODO:设备状态
        上传设备状态，可自行灵活拓展，不一定非要在schedule()接口中实现，也可以在read中实现设备状态的设置；
        你可以通过某种判断机制确定设备的状态，然后通过driverService.deviceStatusSender(deviceId,DeviceStatus)接口将设备状态交给SDK管理。

        设备状态（DeviceStatus）如下：
        ONLINE:在线
        OFFLINE:离线
        MAINTAIN:维护
        FAULT:故障
         */
        driverContext.getDeviceMap().keySet().forEach(id -> driverService.deviceStatusSender(id, Common.Device.Status.ONLINE));
    }

    /**
     * 获取 Opc Da Server
     *
     * @param deviceId
     * @param driverInfo
     * @return
     * @throws JIException
     * @throws UnknownHostException
     */
    private Server getServer(Long deviceId, Map<String, AttributeInfo> driverInfo) throws JIException, UnknownHostException {
        Server server = serverMap.get(deviceId);
        if (null == server) {
            ConnectionInformation connectionInformation = new ConnectionInformation(attribute(driverInfo, "host"), attribute(driverInfo, "clsId"), attribute(driverInfo, "username"), attribute(driverInfo, "password"));
            log.debug("Opc Da Server Connection Info {}", JSON.toJSONString(connectionInformation));
            server = new Server(connectionInformation, Executors.newSingleThreadScheduledExecutor());
        }
        try {
            server.connect();
        } catch (AlreadyConnectedException ignored) {
        }
        serverMap.put(deviceId, server);
        return server;
    }

    /**
     * 获取 Opc Da Item
     *
     * @param server
     * @param pointInfo
     * @return
     * @throws UnknownGroupException
     * @throws NotConnectedException
     * @throws JIException
     * @throws UnknownHostException
     * @throws DuplicateGroupException
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
     * 读取 Opc Da 位号值
     *
     * @param item
     * @return
     * @throws JIException
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
     * 修改 Opc Da 位号值
     *
     * @param item
     * @param type
     * @param value
     * @throws JIException
     */
    private void writeItem(Item item, String type, String value) throws JIException {
        switch (type.toLowerCase()) {
            case Common.ValueType.INT:
                int intValue = value(type, value);
                item.write(new JIVariant(intValue, false));
                break;
            case Common.ValueType.LONG:
                long longValue = value(type, value);
                item.write(new JIVariant(longValue, false));
                break;
            case Common.ValueType.FLOAT:
                float floatValue = value(type, value);
                item.write(new JIVariant(floatValue, false));
                break;
            case Common.ValueType.DOUBLE:
                double doubleValue = value(type, value);
                item.write(new JIVariant(doubleValue, false));
                break;
            case Common.ValueType.BOOLEAN:
                boolean booleanValue = value(type, value);
                item.write(new JIVariant(booleanValue, false));
                break;
            case Common.ValueType.STRING:
                item.write(new JIVariant(value, false));
                break;
            default:
                break;
        }
    }

}
