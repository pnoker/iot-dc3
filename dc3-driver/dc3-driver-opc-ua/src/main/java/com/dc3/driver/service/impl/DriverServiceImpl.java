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

import com.alibaba.fastjson.JSON;
import com.dc3.common.constant.Common;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.driver.key.KeyLoader;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.dc3.common.sdk.util.DriverUtils.attribute;
import static com.dc3.common.sdk.util.DriverUtils.value;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {
    /**
     * Opc Ua Client Map
     */
    private volatile Map<Long, OpcUaClient> clientMap = new HashMap<>(64);
    private static KeyLoader keyLoader;

    static {
        try {
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

    @Override
    public void initial() {
    }

    @Override
    @SneakyThrows
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        return readItem(device.getId(), driverInfo, pointInfo);
    }

    @Override
    @SneakyThrows
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {
        writeItem(device.getId(), driverInfo, pointInfo, value);
        return true;
    }

    @Override
    public void schedule() {

    }

    /**
     * 获取 Opc Ua Client
     *
     * @param driverInfo
     * @return
     * @throws Exception
     */
    private OpcUaClient getOpcUaClient(Long deviceId, Map<String, AttributeInfo> driverInfo) throws Exception {
        log.debug("Opc Ua Connection Info {}", JSON.toJSONString(driverInfo));
        OpcUaClient opcUaClient = clientMap.get(deviceId);
        if (null == opcUaClient) {
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
        }
        return opcUaClient;
    }

    /**
     * 获取 Opc Ua 位号值
     *
     * @param deviceId
     * @param driverInfo
     * @param pointInfo
     * @return
     * @throws Exception
     */
    public String readItem(Long deviceId, Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo) throws Exception {
        OpcUaClient client = getOpcUaClient(deviceId, driverInfo);
        client.connect().get();
        int namespace = attribute(pointInfo, "namespace");
        String tag = attribute(pointInfo, "tag");

        NodeId nodeId = new NodeId(namespace, tag);
        DataValue dataValue = client.readValue(0.0, TimestampsToReturn.Both, nodeId).get();
        return dataValue.getValue().getValue().toString();
    }

    /**
     * 修改 Opc Ua 位号值
     */
    private void writeItem(Long deviceId, Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, AttributeInfo values) throws Exception {
        OpcUaClient client = getOpcUaClient(deviceId, driverInfo);
        client.connect().get();

        int namespace = attribute(pointInfo, "namespace");
        String tag = attribute(pointInfo, "tag"), type = values.getType(), value = values.getValue();
        NodeId nodeId = new NodeId(namespace, tag);

        switch (type.toLowerCase()) {
            case Common.ValueType.INT:
                int intValue = value(type, value);
                client.writeValue(nodeId, new DataValue(new Variant(intValue), null, null));
                break;
            case Common.ValueType.LONG:
                long longValue = value(type, value);
                client.writeValue(nodeId, new DataValue(new Variant(longValue), null, null));
                break;
            case Common.ValueType.FLOAT:
                float floatValue = value(type, value);
                client.writeValue(nodeId, new DataValue(new Variant(floatValue), null, null));
                break;
            case Common.ValueType.DOUBLE:
                double doubleValue = value(type, value);
                client.writeValue(nodeId, new DataValue(new Variant(doubleValue), null, null));
                break;
            case Common.ValueType.BOOLEAN:
                boolean booleanValue = value(type, value);
                client.writeValue(nodeId, new DataValue(new Variant(booleanValue), null, null));
                break;
            case Common.ValueType.STRING:
                client.writeValue(nodeId, new DataValue(new Variant(value), null, null));
                break;
            default:
                break;
        }
    }

}
