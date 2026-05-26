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
import io.github.pnoker.common.manager.dal.EventManager;
import io.github.pnoker.common.manager.dal.EventParamManager;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.EventBuilder;
import io.github.pnoker.common.manager.entity.model.EventDO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.mapper.EventMapper;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventBuilder eventBuilder;

    @Mock
    private EventManager eventManager;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EventParamManager eventParamManager;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    @Mock
    private ProfileService profileService;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private EventServiceImpl service;

    private EventBO bo;
    private EventDO doRow;
    private ProfileBO profile;

    @BeforeEach
    void setUp() {
        bo = new EventBO();
        bo.setId(1L);
        bo.setEventName("Overheat");
        bo.setEventCode("overheat");
        bo.setProfileId(5L);
        bo.setTenantId(100L);

        doRow = new EventDO();
        doRow.setId(1L);
        doRow.setEventName("Overheat");
        doRow.setEventCode("overheat");
        doRow.setProfileId(5L);
        doRow.setTenantId(100L);

        profile = new ProfileBO();
        profile.setId(5L);
        profile.setTenantId(100L);
    }

    @Test
    void saveIgnoresClientEventCode() {
        bo.setEventCode("client-code");
        doRow.setEventCode("generated-code");
        when(profileService.getById(5L)).thenReturn(profile);
        when(eventManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(eventBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(eventManager.save(doRow)).thenReturn(true);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.add(bo));
        assertThat(bo.getEventCode()).isNull();
    }

    @Test
    void updateKeepsGeneratedEventCodeImmutable() {
        bo.setEventCode("client-change");
        when(eventManager.getById(1L)).thenReturn(doRow);
        when(profileService.getById(5L)).thenReturn(profile);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(eventManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(eventBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(eventManager.updateById(doRow)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.update(bo));
        assertThat(bo.getEventCode()).isEqualTo("overheat");
    }

}
