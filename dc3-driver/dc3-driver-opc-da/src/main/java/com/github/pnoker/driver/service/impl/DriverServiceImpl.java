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

package com.github.pnoker.driver.service.impl;

import com.github.pnoker.common.constant.Common;
import com.github.pnoker.common.model.Device;
import com.github.pnoker.common.model.Point;
import com.github.pnoker.common.sdk.bean.AttributeInfo;
import com.github.pnoker.common.sdk.bean.DriverContext;
import com.github.pnoker.common.sdk.service.DriverService;
import com.github.pnoker.common.sdk.service.rabbit.PointValueService;
import com.github.pnoker.driver.bean.OpcDaPointVariable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.Server;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Executors;

import static com.github.pnoker.common.sdk.util.DriverUtils.attribute;
import static com.github.pnoker.common.sdk.util.DriverUtils.value;

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
    private volatile Map<Long, Server> serverMap;

    @Override
    public void initial() {
    }

    @Override
    @SneakyThrows
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        Server server = getServer(device.getId(), driverInfo);
        OpcDaPointVariable opcDaPointVariable = getOpcDaPointVariable(pointInfo);
        Group group = server.addGroup(opcDaPointVariable.getGroup());
        Item item = group.addItem(opcDaPointVariable.getTag());
        String value = item.read(false).getValue().getObjectAsString2();
        server.dispose();
        log.debug("read: device:{}, value:{}", device.getId(), value);
        return value;
    }

    @Override
    @SneakyThrows
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {
        Server server = getServer(device.getId(), driverInfo);
        OpcDaPointVariable opcDaPointVariable = getOpcDaPointVariable(pointInfo);
        Group group = server.addGroup(opcDaPointVariable.getGroup());
        Item item = group.addItem(opcDaPointVariable.getTag());
        writeItem(item, value.getType(), value.getValue());
        server.dispose();
        log.debug("write: device:{}, value:{}", device.getId(), value);
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
     * 获取 Opc Da 连接信息
     *
     * @param driverInfo
     * @return
     */
    private ConnectionInformation getConnectionInformation(Map<String, AttributeInfo> driverInfo) {
        String host = attribute(driverInfo, "host");
        String domain = attribute(driverInfo, "domain");
        String username = attribute(driverInfo, "username");
        String password = attribute(driverInfo, "password");
        log.debug("connectInfo: host:{},domain:{},username:{},password:{}", host, domain, username, password);

        ConnectionInformation connectionInformation = new ConnectionInformation();
        connectionInformation.setHost(host);
        connectionInformation.setDomain(domain);
        connectionInformation.setUser(username);
        connectionInformation.setPassword(password);
        connectionInformation.setClsid("F8582CF2-88FB-11D0-B850-00C0F0104305");
        return connectionInformation;
    }

    /**
     * 获取位号变量信息
     *
     * @param pointInfo
     * @return
     */
    private OpcDaPointVariable getOpcDaPointVariable(Map<String, AttributeInfo> pointInfo) {
        String group = attribute(pointInfo, "group");
        String tag = attribute(pointInfo, "tag");
        log.debug("pointVariable: group:{},tag:{}", group, tag);

        OpcDaPointVariable opcDaPointVariable = new OpcDaPointVariable();
        opcDaPointVariable.setGroup(group);
        opcDaPointVariable.setTag(tag);
        return opcDaPointVariable;
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
            case Common.ValueType.BYTE:
                byte vb = value(type, value);
                item.write(new JIVariant(vb, false));
                break;
            case Common.ValueType.INT:
                int vi = value(type, value);
                item.write(new JIVariant(vi, false));
                break;
            case Common.ValueType.DOUBLE:
                double vd = value(type, value);
                item.write(new JIVariant(vd, false));
                break;
            case Common.ValueType.FLOAT:
                float vf = value(type, value);
                item.write(new JIVariant(vf, false));
                break;
            case Common.ValueType.LONG:
                long vl = value(type, value);
                item.write(new JIVariant(vl, false));
                break;
            case Common.ValueType.BOOLEAN:
                boolean vo = value(type, value);
                item.write(new JIVariant(vo, false));
                break;
            default:
                break;
        }
    }

}
