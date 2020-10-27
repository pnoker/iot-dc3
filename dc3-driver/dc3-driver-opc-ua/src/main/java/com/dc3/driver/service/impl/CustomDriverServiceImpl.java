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
import com.dc3.driver.key.KeyLoader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.dc3.common.sdk.util.DriverUtils.attribute;
import static com.dc3.common.sdk.util.DriverUtils.value;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class CustomDriverServiceImpl implements CustomDriverService {

    @Resource
    private DriverService driverService;
    @Resource
    private DriverContext driverContext;

    private static KeyLoader keyLoader;

    static {
        try {
            //TODO 每次都会生成证书，生产环境可以调整为固定证书
            Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
            Files.createDirectories(securityTempDir);
            if (!Files.exists(securityTempDir)) {
                throw new Exception("unable to create security dir: " + securityTempDir);
            }

            keyLoader = new KeyLoader().load(securityTempDir);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Opc Ua Client Map
     */
    private static Map<Long, OpcUaClient> clientMap = new ConcurrentHashMap<>(16);


    @Override
    public void initial() {
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) throws Exception {
        return readItem(device.getId(), driverInfo, pointInfo);
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) throws Exception {
        writeItem(device.getId(), driverInfo, pointInfo, value);
        return true;
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
     * Get Opc Ua Client
     *
     * @param deviceId   Device Id
     * @param driverInfo Driver Info
     * @return OpcUaClient
     * @throws UaException UaException
     */
    private OpcUaClient getOpcUaClient(Long deviceId, Map<String, AttributeInfo> driverInfo) throws UaException {
        log.debug("Opc Ua Client Info: {}", JSON.toJSONString(driverInfo));
        OpcUaClient opcUaClient = clientMap.get(deviceId);
        if (null == opcUaClient) {
            try {
                opcUaClient = OpcUaClient.create(
                        String.format("opc.tcp://%s:%s%s",
                                attribute(driverInfo, "host"),
                                attribute(driverInfo, "port"),
                                attribute(driverInfo, "path")
                        ),
                        endpoints -> endpoints
                                .stream()
                                .findFirst(),
                        configBuilder -> configBuilder
                                .setApplicationName(LocalizedText.english("DC3 Opc Ua Client"))
                                .setApplicationUri("urn:dc3:opc:ua:client")
                                .setCertificate(keyLoader.getClientCertificate())
                                .setKeyPair(keyLoader.getClientKeyPair())
                                .setIdentityProvider(new AnonymousProvider())
                                .setRequestTimeout(uint(5000))
                                .build()
                );
                clientMap.put(deviceId, opcUaClient);
            } catch (UaException e) {
                clientMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                throw new UaException(e);
            }
        }
        return opcUaClient;
    }

    /**
     * Read Opc Ua Point Value
     *
     * @param deviceId   Device Id
     * @param driverInfo Driver Info
     * @param pointInfo  Point Info
     * @return String Value
     * @throws Exception
     */
    public String readItem(Long deviceId, Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo) {
        CompletableFuture<String> value = new CompletableFuture<>();

        OpcUaClient client;
        try {
            int namespace = attribute(pointInfo, "namespace");
            String tag = attribute(pointInfo, "tag");
            NodeId nodeId = new NodeId(namespace, tag);

            client = getOpcUaClient(deviceId, driverInfo);
            client.connect().get();
            client.readValue(0.0, TimestampsToReturn.Both, nodeId).thenAccept(dataValue -> {
                try {
                    value.complete(dataValue.getValue().getValue().toString());
                } catch (Exception e) {
                    log.error("Opc Ua Point(ns={};s={}) does not exist", namespace, tag);
                }
            });
            return value.get();
        } catch (InterruptedException | ExecutionException | UaException e) {
            log.error("Opc Ua Point Read Error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Write Opc Ua Point Value
     *
     * @param deviceId   Device Id
     * @param driverInfo Driver Info
     * @param pointInfo  Point Info
     * @param values     Value Array
     * @throws UaException          UaException
     * @throws ExecutionException   ExecutionException
     * @throws InterruptedException InterruptedException
     */
    private void writeItem(Long deviceId, Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, AttributeInfo values) {
        OpcUaClient client;
        try {
            int namespace = attribute(pointInfo, "namespace");
            String tag = attribute(pointInfo, "tag"), type = values.getType(), value = values.getValue();
            NodeId nodeId = new NodeId(namespace, tag);

            client = getOpcUaClient(deviceId, driverInfo);
            client.connect().get();

            switch (type.toLowerCase()) {
                case Common.ValueType.INT:
                    int intValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(intValue)));
                    break;
                case Common.ValueType.LONG:
                    long longValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(longValue)));
                    break;
                case Common.ValueType.FLOAT:
                    float floatValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(floatValue)));
                    break;
                case Common.ValueType.DOUBLE:
                    double doubleValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(doubleValue)));
                    break;
                case Common.ValueType.BOOLEAN:
                    boolean booleanValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(booleanValue)));
                    break;
                case Common.ValueType.STRING:
                    client.writeValue(nodeId, new DataValue(new Variant(value)));
                    break;
                default:
                    break;
            }
        } catch (InterruptedException | ExecutionException | UaException e) {
            log.error("Opc Ua Point Write Error: {}", e.getMessage(), e);
        }
    }

}
