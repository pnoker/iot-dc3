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
package io.github.pnoker.common.manager.event.metadata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.mq.sender.MessageSender;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class MetadataEventListenerTest {
    @Mock
    private ReactiveDriverService driverService;

    @Mock
    private MessageSender messageSender;

    @InjectMocks
    private MetadataEventListener listener;

    private DriverBO driver;

    @BeforeEach
    void setUp() {
        driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(1L);
        driver.setServiceName("dc3-driver-modbus-tcp");
    }

    @Test
    void deviceEventNotifiesOwningDriverWithTenant() {
        when(driverService.getByDeviceId(1L, 10L)).thenReturn(Mono.just(driver));
        listener.onApplicationEvent(
                new MetadataEvent(this, 1L, 10L, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE));
        verify(messageSender)
                .send(argThat(m -> m.getTopic() == MqTopic.METADATA
                        && "dc3-driver-modbus-tcp".equals(m.getPartitionKey())
                        && ((io.github.pnoker.common.entity.dto.MetadataEventDTO) m.getPayload())
                                .getTenantId()
                                .equals(1L)));
    }

    @Test
    void pointEventFansOutToEveryAffectedDriver() {
        DriverBO secondary = new DriverBO();
        secondary.setTenantId(1L);
        secondary.setServiceName("dc3-driver-mqtt");
        when(driverService.listByPointId(1L, 20L)).thenReturn(Flux.just(driver, secondary));
        listener.onApplicationEvent(
                new MetadataEvent(this, 1L, 20L, MetadataTypeEnum.POINT, MetadataOperateTypeEnum.ADD));
        verify(messageSender, times(2)).send(any());
    }

    @Test
    void eventWithoutTenantIsDropped() {
        listener.onApplicationEvent(
                new MetadataEvent(this, 10L, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE));
        verifyNoInteractions(driverService, messageSender);
    }

    @Test
    void eventWithTargetServicesBypassesOwnerLookup() {
        listener.onApplicationEvent(new MetadataEvent(
                this, 1L, 10L, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, Set.of("dc3-driver-old")));
        verify(driverService, never()).getByDeviceId(any(), any());
        verify(messageSender).send(argThat(m -> "dc3-driver-old".equals(m.getPartitionKey())));
    }

    @Test
    void driverEventNotifiesRegisteredDriverService() {
        when(driverService.getById(1L, 7L)).thenReturn(Mono.just(driver));
        listener.onApplicationEvent(
                new MetadataEvent(this, 1L, 7L, MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.UPDATE));
        verify(messageSender).send(any());
    }

    @Test
    void serviceFailureDoesNotPublish() {
        when(driverService.getByDeviceId(1L, 10L)).thenReturn(Mono.error(new RuntimeException("downstream offline")));
        listener.onApplicationEvent(
                new MetadataEvent(this, 1L, 10L, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE));
        verify(messageSender, never()).send(any());
    }
}
