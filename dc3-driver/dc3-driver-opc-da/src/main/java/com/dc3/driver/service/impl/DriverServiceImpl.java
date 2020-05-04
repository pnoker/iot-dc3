/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

import com.dc3.common.constant.Common;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.common.sdk.service.rabbit.PointValueService;
import com.dc3.driver.bean.OpcDaPointVariable;
import lombok.SneakyThrows;
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
public class DriverServiceImpl implements DriverService {
    @Resource
    private PointValueService pointValueService;
    @Resource
    private DriverContext driverContext;

    /**
     * Opc Da Server Map
     */
    private volatile Map<Long, Server> serverMap = new HashMap<>(64);

    @Override
    public void initial() {
    }

    @Override
    @SneakyThrows
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        OpcDaPointVariable opcDaPointVariable = getOpcDaPointVariable(pointInfo);

        Server server = getServer(device.getId(), driverInfo);
        Group group = getGroup(server, opcDaPointVariable.getGroup());
        Item item = group.addItem(opcDaPointVariable.getTag());

        String value = readItem(item);
        log.debug("read: device:{}, value:{}", device.getId(), value);
        server.dispose();
        return value;
    }

    @Override
    @SneakyThrows
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {
        OpcDaPointVariable opcDaPointVariable = getOpcDaPointVariable(pointInfo);

        Server server = getServer(device.getId(), driverInfo);
        Group group = getGroup(server, opcDaPointVariable.getGroup());
        Item item = group.addItem(opcDaPointVariable.getTag());

        writeItem(item, value.getType(), value.getValue());
        log.debug("write: device:{}, value:{}", device.getId(), value);
        server.dispose();
        return false;
    }

    @Override
    public void schedule() {

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
            server = new Server(
                    getConnectionInformation(driverInfo),
                    Executors.newSingleThreadScheduledExecutor());
        }
        try {
            server.connect();
        } catch (AlreadyConnectedException e) {
        }
        serverMap.put(deviceId, server);
        return server;
    }

    /**
     * 获取 Opc Da 分组
     *
     * @param server
     * @param groupName
     * @return
     * @throws UnknownGroupException
     * @throws NotConnectedException
     * @throws JIException
     * @throws UnknownHostException
     * @throws DuplicateGroupException
     */
    public Group getGroup(Server server, String groupName) throws NotConnectedException, JIException, UnknownHostException, DuplicateGroupException {
        Group group;
        try {
            group = server.findGroup(groupName);
        } catch (UnknownGroupException e) {
            group = server.addGroup(groupName);
        }

        return group;
    }

    /**
     * 获取 Opc Da 连接信息
     *
     * @param driverInfo
     * @return
     */
    private ConnectionInformation getConnectionInformation(Map<String, AttributeInfo> driverInfo) {
        log.debug("OpcDa server connection information {}", driverInfo);
        return new ConnectionInformation(attribute(driverInfo, "host"), attribute(driverInfo, "clsId"), attribute(driverInfo, "username"), attribute(driverInfo, "password"));
    }

    /**
     * 获取 Opc Da 位号变量信息
     *
     * @param pointInfo
     * @return
     */
    private OpcDaPointVariable getOpcDaPointVariable(Map<String, AttributeInfo> pointInfo) {
        log.debug("OpcDa point information {}", pointInfo);
        return new OpcDaPointVariable(attribute(pointInfo, "group"), attribute(pointInfo, "tag"));
    }

    /**
     * 读取 Opc Da 数据
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
     * 向 Opc Da 写数据
     *
     * @param item
     * @param type
     * @param value
     * @throws JIException
     */
    private void writeItem(Item item, String type, String value) throws JIException {
        switch (type.toLowerCase()) {
            case Common.ValueType.INT:
                int vi = value(type, value);
                item.write(new JIVariant(vi, false));
                break;
            case Common.ValueType.LONG:
                long vl = value(type, value);
                item.write(new JIVariant(vl, false));
                break;
            case Common.ValueType.FLOAT:
                float vf = value(type, value);
                item.write(new JIVariant(vf, false));
                break;
            case Common.ValueType.DOUBLE:
                double vd = value(type, value);
                item.write(new JIVariant(vd, false));
                break;
            case Common.ValueType.BOOLEAN:
                boolean vo = value(type, value);
                item.write(new JIVariant(vo, false));
                break;
            case Common.ValueType.STRING:
                item.write(new JIVariant(value, false));
                break;
            default:
                break;
        }
    }

}
