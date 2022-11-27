/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.driver.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.bean.driver.AttributeInfo;
import io.github.pnoker.common.enums.PointValueTypeEnum;
import io.github.pnoker.common.enums.StatusEnum;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.sdk.bean.driver.DriverContext;
import io.github.pnoker.common.sdk.service.DriverCustomService;
import io.github.pnoker.common.sdk.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.github.pnoker.common.sdk.utils.DriverUtil.attribute;
import static io.github.pnoker.common.sdk.utils.DriverUtil.value;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

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
    private DriverService driverService;

    private Map<String, OpcUaClient> clientMap;

    @Override
    public void initial() {
        clientMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) throws Exception {
        int namespace = attribute(pointInfo, "namespace");
        String tag = attribute(pointInfo, "tag");

        NodeId nodeId = new NodeId(namespace, tag);
        CompletableFuture<String> value = new CompletableFuture<>();
        OpcUaClient client = getOpcUaClient(device.getId(), driverInfo);

        client.connect().get();
        client.readValue(0.0, TimestampsToReturn.Both, nodeId).thenAccept(dataValue -> {
            try {
                value.complete(dataValue.getValue().getValue().toString());
            } catch (Exception e) {
                log.error("accept point(ns={};s={}) value error: {}", namespace, tag, e.getMessage());
            }
        });
        String rawValue = value.get(1, TimeUnit.SECONDS);
        log.debug("read point(ns={};s={}) value: {}", namespace, tag, rawValue);
        return rawValue;

    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) throws Exception {
        writeItem(device.getId(), driverInfo, pointInfo, value);
        return true;
    }

    @Override
    public void schedule() {
        driverContext.getDriverMetadata().getDeviceMap().keySet().forEach(id -> driverService.deviceStatusSender(id, StatusEnum.ONLINE));
    }

    /**
     * Get Opc Ua Client
     *
     * @param deviceId   Device ID
     * @param driverInfo Driver Info
     * @return OpcUaClient
     */
    private OpcUaClient getOpcUaClient(String deviceId, Map<String, AttributeInfo> driverInfo) {
        OpcUaClient opcUaClient = clientMap.get(deviceId);
        if (null == opcUaClient) {
            try {
                opcUaClient = OpcUaClient.create(
                        String.format("opc.tcp://%s:%s%s",
                                attribute(driverInfo, "host"),
                                attribute(driverInfo, "port"),
                                attribute(driverInfo, "path")
                        ),
                        endpoints -> endpoints.stream().findFirst(),
                        configBuilder -> configBuilder
                                .setIdentityProvider(new AnonymousProvider())
                                .setRequestTimeout(uint(5000))
                                .build()
                );
                clientMap.put(deviceId, opcUaClient);
            } catch (UaException e) {
                log.error("get opc ua client error: {}", e.getMessage());
                clientMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
            }
        }
        return clientMap.get(deviceId);
    }

    /**
     * Write Opc Ua Point Value
     *
     * @param deviceId   Device ID
     * @param driverInfo Driver Info
     * @param pointInfo  Point Info
     * @param values     Value Array
     */
    private void writeItem(String deviceId, Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, AttributeInfo values) {
        OpcUaClient client;
        try {
            int namespace = attribute(pointInfo, "namespace");
            String tag = attribute(pointInfo, "tag");
            String type = values.getType();
            String value = values.getValue();

            PointValueTypeEnum valueType = PointValueTypeEnum.of(type);
            if (ObjectUtil.isNull(valueType)) {
                throw new IllegalArgumentException("Unsupported type of " + type);
            }

            NodeId nodeId = new NodeId(namespace, tag);
            client = getOpcUaClient(deviceId, driverInfo);
            client.connect().get();

            switch (valueType) {
                case INT:
                    int intValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(intValue)));
                    break;
                case LONG:
                    long longValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(longValue)));
                    break;
                case FLOAT:
                    float floatValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(floatValue)));
                    break;
                case DOUBLE:
                    double doubleValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(doubleValue)));
                    break;
                case BOOLEAN:
                    boolean booleanValue = value(type, value);
                    client.writeValue(nodeId, new DataValue(new Variant(booleanValue)));
                    break;
                case STRING:
                    client.writeValue(nodeId, new DataValue(new Variant(value)));
                    break;
                default:
                    break;
            }
        } catch (InterruptedException e) {
            log.error("Opc Ua Point Write Error: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Opc Ua Point Write Error: {}", e.getMessage());
        }
    }

}
