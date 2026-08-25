/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.driver.receiver.rabbit;

import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.driver.grpc.client.DriverClient;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MetadataReceiverTest {

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private PointMetadata pointMetadata;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    @Mock
    private DriverClient driverClient;

    @Mock
    private Acknowledgment ack;

    private DriverMetadata driverMetadata;
    private MetadataReceiver receiver;

    private static MetadataEventDTO event(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO dto = new MetadataEventDTO();
        dto.setMetadataType(type);
        dto.setOperateType(op);
        dto.setId(id);
        return dto;
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    @BeforeEach
    void setUp() {
        driverMetadata = new DriverMetadata();
        driverMetadata.setDeviceLeases(Map.of(99L, 1L), System.currentTimeMillis() + 60_000, 1L);

        receiver = new MetadataReceiver(pointMetadata, driverMetadata, deviceMetadata, driverClient, metadataEventPublisher);

    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.metadataReceive(new MqReceived<>(null, Map.of(), false), ack);
        verify(ack).reject(false);
        verify(metadataEventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsPayloadWithoutId() throws Exception {
        MetadataEventDTO dto = new MetadataEventDTO();
        dto.setMetadataType(MetadataTypeEnum.DEVICE);
        dto.setOperateType(MetadataOperateTypeEnum.ADD);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(ack).reject(false);
    }

    @Test
    void deviceAddRefreshesCacheWithoutBypassingLeaseOwnership() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 10L);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(deviceMetadata).loadCache(10L);
        assertThat(driverMetadata.getDeviceIds()).containsExactly(99L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(ack).ack();
    }

    @Test
    void deviceUpdateAlsoTriggersLoadCache() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 10L);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(deviceMetadata).loadCache(10L);
        verify(ack).ack();
    }

    @Test
    void deviceDeleteRemovesCacheAndDriverDeviceIds() throws Exception {
        driverMetadata.setDeviceLeases(Map.of(99L, 1L), System.currentTimeMillis() + 60_000, 1L);
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 99L);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(deviceMetadata).removeCache(99L);
        assertThat(driverMetadata.getDeviceIds()).doesNotContain(99L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(ack).ack();
    }

    @Test
    void pointAddTriggersLoadCache() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.ADD, 20L);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(pointMetadata).loadCache(20L);
        verify(ack).ack();
    }

    @Test
    void pointDeleteRemovesCache() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.DELETE, 20L);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(pointMetadata).removeCache(20L);
        verify(ack).ack();
    }

    @Test
    void driverUpdateRefreshesDriverMetadata() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.UPDATE, 7L);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(driverClient).refreshMetadata(7L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(ack).ack();
    }

    @Test
    void driverDeleteClearsAllDriverSideCaches() throws Exception {
        driverMetadata.setDriver(new DriverBO());
        driverMetadata.setDriverStatus(EntityStatusEnum.ONLINE);

        MetadataEventDTO dto = event(MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.DELETE, 7L);
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);

        verify(deviceMetadata).clearCache();
        verify(pointMetadata).clearCache();
        assertThat(driverMetadata.getDeviceIds()).isEmpty();
        assertThat(driverMetadata.getDriver()).isNull();
        assertThat(driverMetadata.getDriverStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
        verify(driverClient, never()).refreshMetadata(7L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(ack).ack();
    }

    @Test
    void commandMetadataEventIsForwardedWithoutCacheMutation() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.UPDATE, 30L);

        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);

        verify(deviceMetadata, never()).loadCache(30L);
        verify(pointMetadata, never()).loadCache(30L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(ack).ack();
    }

    @Test
    void eventMetadataEventIsForwardedWithoutCacheMutation() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.UPDATE, 40L);

        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);

        verify(deviceMetadata, never()).loadCache(40L);
        verify(pointMetadata, never()).loadCache(40L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(ack).ack();
    }

    @Test
    void nacksAndRequeuesOnPublisherFailure() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 10L);
        doThrow(new RuntimeException("downstream offline"))
                .when(metadataEventPublisher).publishEvent(any());
        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);
        verify(ack).reject(true);
    }

    @Test
    void deviceAddNacksAndRequeuesWhenLoadCacheFails() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 10L);
        doThrow(new ServiceException("manager center unreachable"))
                .when(deviceMetadata).loadCache(10L);

        receiver.metadataReceive(new MqReceived<>(dto, Map.of(), false), ack);

        // gRPC failure must surface as nack(requeue) rather than ack — earlier the
        // loader was fire-and-forget and a failure silently dropped the event.
        verify(ack).reject(true);
        // Metadata events never create ownership; only a complete Manager lease
        // snapshot can install a device and fencing token.
        assertThat(driverMetadata.getDeviceIds()).containsExactly(99L);
        verify(metadataEventPublisher, never()).publishEvent(any());
    }
}
