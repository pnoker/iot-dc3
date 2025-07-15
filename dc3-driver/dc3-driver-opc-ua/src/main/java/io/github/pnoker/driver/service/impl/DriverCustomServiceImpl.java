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
import java.util.Objects;
import java.util.concurrent.*;


/**
 * 驱动自定义服务实现类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    DriverMetadata driverMetadata;
    @Resource
    private DriverSenderService driverSenderService;

    private Map<Long, OpcUaClient> connectMap;

    @Override
    public void initial() {
        /*
         * 驱动初始化逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 驱动启动时会自动执行该方法, 您可以在此处执行特定的初始化操作。
         *
         */
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        /*
         * 设备状态上传逻辑
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 设备状态的上传可以根据具体需求灵活实现, 以下是一些常见的实现方式：
         * - 在 `read` 方法中根据读取的数据判断设备状态；
         * - 在自定义的定时任务中定期检查设备状态；
         * - 根据特定的业务逻辑或事件触发设备状态的判断。
         *
         * 最终通过 {@link DriverSenderService#deviceStatusSender(Long, DeviceStatusEnum)} 接口将设备状态提交给 SDK 管理。
         * 设备状态枚举 {@link DeviceStatusEnum} 包含以下状态：
         * - ONLINE: 设备在线
         * - OFFLINE: 设备离线
         * - MAINTAIN: 设备维护中
         * - FAULT: 设备故障
         *
         * 在以下示例中, 所有设备的状态被设置为 {@link DeviceStatusEnum#ONLINE}, 并设置状态的有效期为 25 {@link TimeUnit#SECONDS}。
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * 接收驱动, 设备, 位号元数据的新增, 更新, 删除事件。
         *
         * 元数据类型: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT)
         * 元数据操作类型: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Device metadata event: deviceId: {}, operate: {}", metadataEvent.getId(), operateType);

            // When the device is updated or deleted, remove the corresponding connection handle
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
         * 读取 OPC UA 点位值
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 1. 通过设备ID和驱动配置获取 OPC UA 客户端连接。
         * 2. 使用点位配置读取 OPC UA 节点的值。
         * 3. 将读取到的值封装为 RValue 对象返回。
         */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig));

    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * 写入 OPC UA 点位值
         *
         * 提示: 此处逻辑仅供参考, 请务必结合实际应用场景进行修改。
         * 1. 通过设备ID和驱动配置获取 OPC UA 客户端连接。
         * 2. 使用点位配置和写入值将数据写入 OPC UA 节点。
         * 3. 返回写入操作是否成功。
         */
        OpcUaClient client = getConnector(device.getId(), driverConfig);
        return writeValue(client, pointConfig, wValue);
    }

    /**
     * 获取 OPC UA 客户端连接
     *
     * @param deviceId     设备ID, 用于标识唯一的设备连接
     * @param driverConfig 驱动配置信息, 包含连接 OPC UA 服务器所需的参数
     * @return OpcUaClient 返回与指定设备关联的 OPC UA 客户端实例
     * @throws ConnectorException 如果连接 OPC UA 服务器失败, 抛出此异常
     */
    private OpcUaClient getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        log.debug("OPC UA server connection info: {}", JsonUtil.toJsonString(driverConfig));
        OpcUaClient opcUaClient = connectMap.get(deviceId);
        if (Objects.isNull(opcUaClient)) {
            String host = driverConfig.get("host").getValue(String.class);
            int port = driverConfig.get("port").getValue(Integer.class);
            String path = driverConfig.get("path").getValue(String.class);
            String url = String.format("opc.tcp://%s:%s%s", host, port, path);
            try {
                opcUaClient = OpcUaClient.create(
                        url,
                        endpoints -> endpoints.stream().findFirst(),
                        configBuilder -> configBuilder
                                .setIdentityProvider(new AnonymousProvider()) // 使用匿名身份验证
                                .setRequestTimeout(Unsigned.uint(5000)) // 设置请求超时时间为 5000 毫秒
                                .build()
                );
                connectMap.put(deviceId, opcUaClient);
            } catch (UaException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Failed to connect OPC UA client: {}", e.getMessage(), e);
                throw new ConnectorException(e.getMessage());
            }
        }
        return opcUaClient;
    }

    /**
     * 根据点位配置获取 OPC UA 节点
     *
     * @param pointConfig 点位配置信息, 包含命名空间和标签
     * @return OPC UA 节点标识
     */
    private NodeId getNode(Map<String, AttributeBO> pointConfig) {
        int namespace = pointConfig.get("namespace").getValue(Integer.class);
        String tag = pointConfig.get("tag").getValue(String.class);
        return new NodeId(namespace, tag);
    }

    /**
     * 读取 OPC UA 节点的值
     *
     * @param client      OPC UA 客户端实例
     * @param pointConfig 点位配置信息
     * @return 读取到的节点值
     * @throws ReadPointException 如果读取操作失败, 抛出此异常
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
     * 写入 OPC UA 节点的值
     *
     * @param client      OPC UA 客户端实例
     * @param pointConfig 点位配置信息
     * @param wValue      写入值
     * @return 写入操作是否成功
     * @throws WritePointException 如果写入操作失败, 抛出此异常
     */
    private boolean writeValue(OpcUaClient client, Map<String, AttributeBO> pointConfig, WValue wValue) {
        try {
            NodeId nodeId = getNode(pointConfig);
            client.connect().get();
            return writeNode(client, nodeId, wValue);
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
     * 将值写入 OPC UA 节点
     *
     * @param client OPC UA 客户端实例
     * @param nodeId OPC UA 节点标识
     * @param wValue 待写入的值
     * @return 写入操作是否成功
     * @throws ExecutionException   写入操作失败时抛出
     * @throws InterruptedException 写入操作被中断时抛出
     */
    private boolean writeNode(OpcUaClient client, NodeId nodeId, WValue wValue) throws ExecutionException, InterruptedException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(wValue.getType().getCode());
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + wValue.getType());
        }

        CompletableFuture<StatusCode> status = new CompletableFuture<>();
        switch (valueType) {
            case INT:
                int intValue = wValue.getValue(Integer.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(intValue)));
                break;
            case LONG:
                long longValue = wValue.getValue(Long.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(longValue)));
                break;
            case FLOAT:
                float floatValue = wValue.getValue(Float.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(floatValue)));
                break;
            case DOUBLE:
                double doubleValue = wValue.getValue(Double.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(doubleValue)));
                break;
            case BOOLEAN:
                boolean booleanValue = wValue.getValue(Boolean.class);
                status = client.writeValue(nodeId, new DataValue(new Variant(booleanValue)));
                break;
            case STRING:
                status = client.writeValue(nodeId, new DataValue(new Variant(wValue.getValue())));
                break;
            default:
                break;
        }

        if (Objects.nonNull(status) && Objects.nonNull(status.get())) {
            return status.get().getValue() > 0;
        }
        return false;
    }

}
