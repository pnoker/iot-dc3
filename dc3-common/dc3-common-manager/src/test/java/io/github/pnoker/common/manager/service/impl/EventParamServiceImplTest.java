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

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.dal.EventParamManager;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.entity.builder.EventParamBuilder;
import io.github.pnoker.common.manager.entity.model.EventParamDO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventParamServiceImplTest {

    @Mock
    private EventParamBuilder eventParamBuilder;

    @Mock
    private EventParamManager eventParamManager;

    @Mock
    private EventService eventService;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private EventParamServiceImpl service;

    private EventParamBO bo;
    private EventParamDO doRow;
    private EventBO event;

    @BeforeEach
    void setUp() {
        bo = new EventParamBO();
        bo.setId(1L);
        bo.setParamName("level");
        bo.setParamCode("level");
        bo.setEventId(10L);
        bo.setTenantId(100L);

        doRow = new EventParamDO();
        doRow.setId(1L);
        doRow.setParamName("level");
        doRow.setParamCode("level");
        doRow.setEventId(10L);
        doRow.setTenantId(100L);

        event = new EventBO();
        event.setId(10L);
        event.setProfileId(20L);
        event.setTenantId(100L);
    }

    @Test
    void addPublishesOwningEventUpdate() {
        when(eventService.getById(10L)).thenReturn(event);
        when(eventParamManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(eventParamBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(eventParamManager.save(doRow)).thenReturn(true);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.add(bo);

        assertMetadataEvent(MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.UPDATE, 10L);
    }

    @Test
    void updatePublishesOwningEventUpdate() {
        when(eventParamManager.getById(1L)).thenReturn(doRow);
        when(eventService.getById(10L)).thenReturn(event);
        when(eventParamManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(eventParamBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(eventParamManager.updateById(doRow)).thenReturn(true);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.update(bo);

        assertMetadataEvent(MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.UPDATE, 10L);
    }

    @Test
    void deletePublishesOwningEventUpdate() {
        when(eventParamManager.getById(1L)).thenReturn(doRow);
        when(eventParamManager.removeById(1L)).thenReturn(true);
        when(eventService.getById(10L)).thenReturn(event);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.delete(1L);

        assertMetadataEvent(MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.UPDATE, 10L);
    }

    private void assertMetadataEvent(MetadataTypeEnum metadataType, MetadataOperateTypeEnum operateType, Long id) {
        ArgumentCaptor<MetadataEvent> captor = ArgumentCaptor.forClass(MetadataEvent.class);
        verify(metadataEventPublisher).publishEvent(captor.capture());

        MetadataEvent event = captor.getValue();
        assertThat(event.getMetadataType()).isEqualTo(metadataType);
        assertThat(event.getOperateType()).isEqualTo(operateType);
        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getTargetServices()).isEmpty();
    }

}
