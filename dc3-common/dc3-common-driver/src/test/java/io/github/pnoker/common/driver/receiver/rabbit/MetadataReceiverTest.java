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

import com.rabbitmq.client.Channel;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
    private Channel channel;

    private DriverMetadata driverMetadata;
    private MetadataReceiver receiver;
    private Message message;

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
        driverMetadata.setDeviceIds(new HashSet<>(Set.of(99L)));

        receiver = new MetadataReceiver(pointMetadata, driverMetadata, deviceMetadata, driverClient, metadataEventPublisher);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message(new byte[0], props);
    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.metadataReceive(channel, message, null);
        verify(channel).basicReject(eq(7L), eq(false));
        verify(metadataEventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsPayloadWithoutId() throws Exception {
        MetadataEventDTO dto = new MetadataEventDTO();
        dto.setMetadataType(MetadataTypeEnum.DEVICE);
        dto.setOperateType(MetadataOperateTypeEnum.ADD);
        receiver.metadataReceive(channel, message, dto);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void deviceAddTriggersLoadCacheAndAddsToDriverDeviceIds() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 10L);
        receiver.metadataReceive(channel, message, dto);
        verify(deviceMetadata).loadCache(10L);
        assertThat(driverMetadata.getDeviceIds()).contains(10L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void deviceUpdateAlsoTriggersLoadCache() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 10L);
        receiver.metadataReceive(channel, message, dto);
        verify(deviceMetadata).loadCache(10L);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void deviceDeleteRemovesCacheAndDriverDeviceIds() throws Exception {
        driverMetadata.getDeviceIds().add(99L);
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 99L);
        receiver.metadataReceive(channel, message, dto);
        verify(deviceMetadata).removeCache(99L);
        assertThat(driverMetadata.getDeviceIds()).doesNotContain(99L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void pointAddTriggersLoadCache() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.ADD, 20L);
        receiver.metadataReceive(channel, message, dto);
        verify(pointMetadata).loadCache(20L);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void pointDeleteRemovesCache() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.DELETE, 20L);
        receiver.metadataReceive(channel, message, dto);
        verify(pointMetadata).removeCache(20L);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void driverUpdateRefreshesDriverMetadata() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.UPDATE, 7L);
        receiver.metadataReceive(channel, message, dto);
        verify(driverClient).refreshMetadata(7L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void driverDeleteClearsAllDriverSideCaches() throws Exception {
        driverMetadata.setDriver(new DriverBO());
        driverMetadata.setDriverStatus(EntityStatusEnum.ONLINE);

        MetadataEventDTO dto = event(MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.DELETE, 7L);
        receiver.metadataReceive(channel, message, dto);

        verify(deviceMetadata).clearCache();
        verify(pointMetadata).clearCache();
        assertThat(driverMetadata.getDeviceIds()).isEmpty();
        assertThat(driverMetadata.getDriver()).isNull();
        assertThat(driverMetadata.getDriverStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
        verify(driverClient, never()).refreshMetadata(7L);
        verify(metadataEventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MetadataEvent.class));
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void nacksAndRequeuesOnPublisherFailure() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 10L);
        doThrow(new RuntimeException("downstream offline"))
                .when(metadataEventPublisher).publishEvent(any());
        receiver.metadataReceive(channel, message, dto);
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
    }

    @Test
    void deviceAddNacksAndRequeuesWhenLoadCacheFails() throws Exception {
        MetadataEventDTO dto = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 10L);
        doThrow(new ServiceException("manager center unreachable"))
                .when(deviceMetadata).loadCache(10L);

        receiver.metadataReceive(channel, message, dto);

        // gRPC failure must surface as nack(requeue) rather than ack — earlier the
        // loader was fire-and-forget and a failure silently dropped the event.
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
        // deviceId is added before loadCache so that a Quartz scan racing with the
        // refresh sees a consistent view; on failure the id stays so the requeued
        // event can retry, and a confirmed-null upstream will clean it via postLoad.
        assertThat(driverMetadata.getDeviceIds()).contains(10L);
        verify(metadataEventPublisher, never()).publishEvent(any());
    }
}
