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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

    @BeforeEach
    void setUp() {
        driverMetadata = new DriverMetadata();
        driverMetadata.setDeviceLeases(Map.of(99L, 1L), System.currentTimeMillis() + 60_000, 1L);
        receiver = new MetadataReceiver(
                pointMetadata, driverMetadata, deviceMetadata, driverClient, metadataEventPublisher);
        lenient().when(deviceMetadata.refreshCache(anyLong())).thenReturn(Mono.empty());
        lenient().when(pointMetadata.refreshCache(anyLong())).thenReturn(Mono.empty());
        lenient().when(driverClient.refreshMetadata(anyLong())).thenReturn(Mono.empty());
    }

    @Test
    void rejectsInvalidEnvelopeAsDeadLetter() {
        StepVerifier.create(receiver.metadataReceive(received(null), ack)).verifyComplete();

        verify(ack).reject(false);
        verifyNoInteractions(metadataEventPublisher);
    }

    @Test
    void deviceRefreshCompletesBeforePublishingLocalEvent() {
        MetadataEventDTO event = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 10L);

        StepVerifier.create(receiver.metadataReceive(received(event), ack)).verifyComplete();

        verify(deviceMetadata).refreshCache(10L);
        assertThat(driverMetadata.getDeviceIds()).containsExactly(99L);
        verify(metadataEventPublisher).publishEvent(any(MetadataEvent.class));
        verifyNoInteractions(ack);
    }

    @Test
    void deviceRefreshFailurePropagatesAndSkipsLocalEvent() {
        MetadataEventDTO event = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 10L);
        when(deviceMetadata.refreshCache(10L)).thenReturn(Mono.error(new IllegalStateException("manager unavailable")));

        StepVerifier.create(receiver.metadataReceive(received(event), ack))
                .expectErrorMessage("manager unavailable")
                .verify();

        verify(metadataEventPublisher, never()).publishEvent(any());
        verifyNoInteractions(ack);
    }

    @Test
    void deviceDeleteRemovesOwnershipBeforePublishingEvent() {
        MetadataEventDTO event = event(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 99L);

        StepVerifier.create(receiver.metadataReceive(received(event), ack)).verifyComplete();

        verify(deviceMetadata).removeCache(99L);
        assertThat(driverMetadata.getDeviceIds()).isEmpty();
        verify(metadataEventPublisher).publishEvent(any(MetadataEvent.class));
    }

    @Test
    void pointRefreshUsesReactiveCacheCompletion() {
        MetadataEventDTO event = event(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 20L);

        StepVerifier.create(receiver.metadataReceive(received(event), ack)).verifyComplete();

        verify(pointMetadata).refreshCache(20L);
        verify(metadataEventPublisher).publishEvent(any(MetadataEvent.class));
    }

    @Test
    void driverRefreshUsesAsyncGrpcCompletion() {
        MetadataEventDTO event = event(MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.UPDATE, 7L);

        StepVerifier.create(receiver.metadataReceive(received(event), ack)).verifyComplete();

        verify(driverClient).refreshMetadata(7L);
        verify(metadataEventPublisher).publishEvent(any(MetadataEvent.class));
    }

    @Test
    void driverDeleteClearsAllCaches() {
        DriverBO driver = new DriverBO();
        driverMetadata.setDriver(driver);
        driverMetadata.setDriverStatus(EntityStatusEnum.ONLINE);
        MetadataEventDTO event = event(MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.DELETE, 7L);

        StepVerifier.create(receiver.metadataReceive(received(event), ack)).verifyComplete();

        verify(deviceMetadata).clearCache();
        verify(pointMetadata).clearCache();
        verify(driverClient, never()).refreshMetadata(7L);
        assertThat(driverMetadata.getDriver()).isNull();
        assertThat(driverMetadata.getDriverStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void commandAndEventMetadataAreForwardedWithoutCacheMutation() {
        StepVerifier.create(receiver.metadataReceive(
                        received(event(MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.UPDATE, 30L)), ack))
                .verifyComplete();
        StepVerifier.create(receiver.metadataReceive(
                        received(event(MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.UPDATE, 40L)), ack))
                .verifyComplete();

        verify(deviceMetadata, never()).refreshCache(anyLong());
        verify(pointMetadata, never()).refreshCache(anyLong());
        verify(metadataEventPublisher, org.mockito.Mockito.times(2)).publishEvent(any(MetadataEvent.class));
    }

    @Test
    void localPublisherFailurePropagates() {
        MetadataEventDTO event = event(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.DELETE, 20L);
        doThrow(new IllegalStateException("local listener failed"))
                .when(metadataEventPublisher)
                .publishEvent(any());

        StepVerifier.create(receiver.metadataReceive(received(event), ack))
                .expectErrorMessage("local listener failed")
                .verify();

        verifyNoInteractions(ack);
    }

    private MqReceived<MetadataEventDTO> received(MetadataEventDTO event) {
        return new MqReceived<>(event, Map.of(), false);
    }

    private MetadataEventDTO event(MetadataTypeEnum type, MetadataOperateTypeEnum operation, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(type);
        event.setOperateType(operation);
        event.setId(id);
        return event;
    }
}
