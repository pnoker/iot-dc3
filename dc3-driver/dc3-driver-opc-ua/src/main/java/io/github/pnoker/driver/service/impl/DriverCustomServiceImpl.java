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

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.driver.entity.bean.RWPointValue;
import io.github.pnoker.common.driver.entity.dto.DeviceDTO;
import io.github.pnoker.common.driver.entity.dto.PointDTO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.bo.AttributeBO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;


/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private DriverSenderService driverSenderService;

    private Map<Long, OpcUaClient> connectMap;

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
        - 在read中实现设备状态的判断；
        - 在自定义定时任务中实现设备状态的判断；
        - 根据某种判断机制实现设备状态的判断。

        最后根据 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理, 其中设备状态（StatusEnum）:
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
        deviceMetadata.getAllDevice().forEach(device -> driverSenderService.deviceStatusSender(device.getId(), DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public String read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceDTO device, PointDTO point) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
         */
        OpcUaClient client = getConnector(device.getId(), driverConfig);
        return readValue(client, pointConfig);

    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceDTO device, PointDTO point, RWPointValue value) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
         */
        OpcUaClient client = getConnector(device.getId(), driverConfig);
        return writeValue(client, pointConfig, value);
    }

    /**
     * 获取 Opc Ua Client
     *
     * @param deviceId     设备ID
     * @param driverConfig 驱动信息
     * @return OpcUaClient
     */
    private OpcUaClient getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        log.debug("Opc Ua Server Connection Info {}", JsonUtil.toJsonString(driverConfig));
        OpcUaClient opcUaClient = connectMap.get(deviceId);
        if (ObjectUtil.isNull(opcUaClient)) {
            String host = driverConfig.get("host").getAttributeValue(String.class);
            int port = driverConfig.get("port").getAttributeValue(Integer.class);
            String path = driverConfig.get("path").getAttributeValue(String.class);
            String url = String.format("opc.tcp://%s:%s%s", host, port, path);
            try {
                opcUaClient = OpcUaClient.create(
                        url,
                        endpoints -> endpoints.stream().findFirst(),
                        configBuilder -> configBuilder
                                .setIdentityProvider(new AnonymousProvider())
                                .setRequestTimeout(Unsigned.uint(5000))
                                .build()
                );
                connectMap.put(deviceId, opcUaClient);
            } catch (UaException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Connect opc ua client error: {}", e.getMessage(), e);
                throw new ConnectorException(e.getMessage());
            }
        }
        return opcUaClient;
    }

    /**
     * 获取 Opc Ua Item
     *
     * @param pointConfig 位号信息
     * @return OpcUa Node
     */
    private NodeId getNode(Map<String, AttributeBO> pointConfig) {
        int namespace = pointConfig.get("namespace").getAttributeValue(Integer.class);
        String tag = pointConfig.get("tag").getAttributeValue(String.class);
        return new NodeId(namespace, tag);
    }

    /**
     * 获取 OpcUa 值
     *
     * @param client      OpcUaClient
     * @param pointConfig 位号信息
     * @return Node Value
     */
    private String readValue(OpcUaClient client, Map<String, AttributeBO> pointConfig) {
        try {
            NodeId nodeId = getNode(pointConfig);
            client.connect().get();
            CompletableFuture<String> value = new CompletableFuture<>();
            client.readValue(0.0, TimestampsToReturn.Both, nodeId).thenAccept(dataValue -> value.complete(dataValue.getValue().getValue().toString()));
            return value.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Read opc ua value error: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new ReadPointException(e.getMessage());
        } catch (ExecutionException | TimeoutException e) {
            log.error("Read opc ua value error: {}", e.getMessage(), e);
            throw new ReadPointException(e.getMessage());
        }
    }

    /**
     * 写入 OpcUa 值
     *
     * @param pointConfig 位号信息
     * @param value       写入值
     */
    private boolean writeValue(OpcUaClient client, Map<String, AttributeBO> pointConfig, RWPointValue value) {
        try {
            NodeId nodeId = getNode(pointConfig);

            client.connect().get();
            return writeNode(client, nodeId, value);
        } catch (InterruptedException e) {
            log.error("Write opc ua value error: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new WritePointException(e.getMessage());
        } catch (ExecutionException e) {
            log.error("Write opc ua value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * Write Opc Ua Node
     *
     * @param client OpcUaClient
     * @param nodeId OpcUa Node
     * @param value  写入值
     * @return 是否写入
     * @throws ExecutionException   ExecutionException
     * @throws InterruptedException InterruptedException
     */
    private boolean writeNode(OpcUaClient client, NodeId nodeId, RWPointValue value) throws ExecutionException, InterruptedException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(value.getType().getCode());
        if (ObjectUtil.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + value.getType());
        }

        CompletableFuture<StatusCode> status = new CompletableFuture<>();
        switch (valueType) {
            case INT:
                int intValue = value.getValue(Integer.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(intValue)));
                break;
            case LONG:
                long longValue = value.getValue(Long.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(longValue)));
                break;
            case FLOAT:
                float floatValue = value.getValue(Float.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(floatValue)));
                break;
            case DOUBLE:
                double doubleValue = value.getValue(Double.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(doubleValue)));
                break;
            case BOOLEAN:
                boolean booleanValue = value.getValue(Boolean.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(booleanValue)));
                break;
            case STRING:
                status = client.writeValue(nodeId, new DataValue(new Variant(value.getValue())));
                break;
            default:
                break;
        }

        if (ObjectUtil.isNotNull(status) && ObjectUtil.isNotNull(status.get())) {
            return status.get().getValue() > 0;
        }
        return false;
    }

}
