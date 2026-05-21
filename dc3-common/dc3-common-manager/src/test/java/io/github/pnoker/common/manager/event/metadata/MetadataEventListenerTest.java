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

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataEventListenerTest {

    @Mock
    private DriverService driverService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MetadataEventListener listener;

    private DriverBO driver;

    @BeforeEach
    void setUp() {
        driver = new DriverBO();
        driver.setId(7L);
        driver.setServiceName("dc3-driver-modbus-tcp");
    }

    @Test
    void deviceEventNotifiesOwningDriverViaRabbit() {
        when(driverService.listByDeviceId(10L)).thenReturn(driver);

        listener.onApplicationEvent(new MetadataEvent(this, 10L, MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_METADATA),
                eq(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + "dc3-driver-modbus-tcp"),
                any(MetadataEventDTO.class));
    }

    @Test
    void pointEventFansOutToEveryAffectedDriver() {
        DriverBO secondary = new DriverBO();
        secondary.setServiceName("dc3-driver-mqtt");
        when(driverService.selectByPointId(20L)).thenReturn(List.of(driver, secondary));

        listener.onApplicationEvent(new MetadataEvent(this, 20L, MetadataTypeEnum.POINT,
                MetadataOperateTypeEnum.ADD));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_METADATA),
                eq(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + "dc3-driver-modbus-tcp"),
                any(MetadataEventDTO.class));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_METADATA),
                eq(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + "dc3-driver-mqtt"),
                any(MetadataEventDTO.class));
        verify(rabbitTemplate, times(2)).convertAndSend(
                any(String.class), any(String.class), any(MetadataEventDTO.class));
    }

    @Test
    void pointEventWithEmptyDriverListEmitsNothing() {
        when(driverService.selectByPointId(20L)).thenReturn(List.of());

        listener.onApplicationEvent(new MetadataEvent(this, 20L, MetadataTypeEnum.POINT,
                MetadataOperateTypeEnum.UPDATE));

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void eventWithTargetServicesBypassesOwnerLookup() {
        listener.onApplicationEvent(new MetadataEvent(this, 10L, MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.DELETE, Set.of("dc3-driver-old")));

        verify(driverService, never()).listByDeviceId(10L);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_METADATA),
                eq(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + "dc3-driver-old"),
                any(MetadataEventDTO.class));
    }

    @Test
    void driverEventNotifiesRegisteredDriverService() {
        when(driverService.getById(7L)).thenReturn(driver);

        listener.onApplicationEvent(new MetadataEvent(this, 7L, MetadataTypeEnum.DRIVER,
                MetadataOperateTypeEnum.UPDATE));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_METADATA),
                eq(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + "dc3-driver-modbus-tcp"),
                any(MetadataEventDTO.class));
    }

    @Test
    void serviceFailureIsSwallowedSilently() {
        when(driverService.listByDeviceId(10L)).thenThrow(new RuntimeException("downstream offline"));

        listener.onApplicationEvent(new MetadataEvent(this, 10L, MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.DELETE));

        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class), any(String.class), any(MetadataEventDTO.class));
    }
}
