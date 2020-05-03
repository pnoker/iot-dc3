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
        Server server = getServer(device.getId(), driverInfo);
        OpcDaPointVariable opcDaPointVariable = getOpcDaPointVariable(pointInfo);
        Group group = server.addGroup(opcDaPointVariable.getGroup());
        Item item = group.addItem(opcDaPointVariable.getTag());
        String value = item.read(false).getValue().getObject().toString();
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
        String clsId = attribute(driverInfo, "clsId");
        String username = attribute(driverInfo, "username");
        String password = attribute(driverInfo, "password");
        log.debug("connectInfo: host:{},proId:{},username:{},password:{}", host, clsId, username, password);

        ConnectionInformation connectionInformation = new ConnectionInformation();
        connectionInformation.setHost(host);
        connectionInformation.setClsid(clsId);
        connectionInformation.setUser(username);
        connectionInformation.setPassword(password);
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


    /*public static void main(String[] args) {
        // create connection information
        final ConnectionInformation ci = new ConnectionInformation();
        ci.setHost("localhost");
        ci.setUser("pnoke");
        ci.setPassword("abcd4455563");
        //ci.setProgId("Matrikon.OPC.Simulation.1");
       ci.setClsid("F8582CF2-88FB-11D0-B850-00C0F0104305"); // if ProgId is not working, try it using the Clsid instead
        // create a new server
        final Server server = new Server(ci, Executors.newSingleThreadScheduledExecutor());

        try {
            // connect to server
            server.connect();

            Group group = server.addGroup("dc3");
            Item item = group.addItem("Random.Int4");
            int value = item.read(false).getValue().getObjectAsInt();
            log.info(String.valueOf(value));
        } catch (final JIException e) {
            System.out.println(String.format("%08X: %s", e.getErrorCode(), server.getErrorMessage(e.getErrorCode())));
            e.printStackTrace();
        } catch (AlreadyConnectedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (AddFailedException e) {
            e.printStackTrace();
        } catch (NotConnectedException e) {
            e.printStackTrace();
        } catch (DuplicateGroupException e) {
            e.printStackTrace();
        }
    }*/

}
