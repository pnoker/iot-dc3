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

package io.github.pnoker.common.manager.biz.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.DictionaryForManagerBuilder;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.entity.query.DictionaryQuery;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryForManagerServiceImplTest {

    @Mock
    private DictionaryForManagerBuilder dictionaryBuilder;

    @Mock
    private DriverService driverService;

    @Mock
    private ProfileService profileService;

    @Mock
    private DeviceService deviceService;

    @Mock
    private PointService pointService;

    @InjectMocks
    private DictionaryForManagerServiceImpl service;

    @Test
    void driverDictionaryProjectsLabelOntoDriverNameQuery() {
        Page<DriverBO> sourcePage = new Page<>();
        Page<DictionaryBO> mappedPage = new Page<>();
        when(driverService.selectByPage(any(DriverQuery.class))).thenReturn(sourcePage);
        when(dictionaryBuilder.buildVOPageByDriverBOPage(sourcePage)).thenReturn(mappedPage);

        DictionaryQuery query = new DictionaryQuery();
        query.setLabel("Modbus");
        query.setTenantId(100L);
        assertThat(service.driverDictionary(query)).isSameAs(mappedPage);
        assertThat(query.getPage()).isNotNull();

        ArgumentCaptor<DriverQuery> captor = ArgumentCaptor.forClass(DriverQuery.class);
        verify(driverService).selectByPage(captor.capture());
        assertThat(captor.getValue().getDriverName()).isEqualTo("Modbus");
        assertThat(captor.getValue().getTenantId()).isEqualTo(100L);
    }

    @Test
    void profileDictionaryProjectsLabelOntoProfileNameQuery() {
        Page<ProfileBO> sourcePage = new Page<>();
        Page<DictionaryBO> mappedPage = new Page<>();
        when(profileService.selectByPage(any(ProfileQuery.class))).thenReturn(sourcePage);
        when(dictionaryBuilder.buildVOPageByProfileBOPage(sourcePage)).thenReturn(mappedPage);

        DictionaryQuery query = new DictionaryQuery();
        query.setLabel("Default");
        assertThat(service.profileDictionary(query)).isSameAs(mappedPage);

        ArgumentCaptor<ProfileQuery> captor = ArgumentCaptor.forClass(ProfileQuery.class);
        verify(profileService).selectByPage(captor.capture());
        assertThat(captor.getValue().getProfileName()).isEqualTo("Default");
    }

    @Test
    void pointDictionaryForProfileMapsParentIdToProfileId() {
        Page<PointBO> sourcePage = new Page<>();
        Page<DictionaryBO> mappedPage = new Page<>();
        when(pointService.selectByPage(any(PointQuery.class))).thenReturn(sourcePage);
        when(dictionaryBuilder.buildVOPageByPointBOPage(sourcePage)).thenReturn(mappedPage);

        DictionaryQuery query = new DictionaryQuery();
        query.setParentId(5L);
        query.setLabel("Temp");
        assertThat(service.pointDictionaryForProfile(query)).isSameAs(mappedPage);

        ArgumentCaptor<PointQuery> captor = ArgumentCaptor.forClass(PointQuery.class);
        verify(pointService).selectByPage(captor.capture());
        assertThat(captor.getValue().getProfileId()).isEqualTo(5L);
        assertThat(captor.getValue().getDeviceId()).isNull();
    }

    @Test
    void pointDictionaryForDeviceMapsParentIdToDeviceId() {
        Page<PointBO> sourcePage = new Page<>();
        Page<DictionaryBO> mappedPage = new Page<>();
        when(pointService.selectByPage(any(PointQuery.class))).thenReturn(sourcePage);
        when(dictionaryBuilder.buildVOPageByPointBOPage(sourcePage)).thenReturn(mappedPage);

        DictionaryQuery query = new DictionaryQuery();
        query.setParentId(11L);
        query.setLabel("Status");
        assertThat(service.pointDictionaryForDevice(query)).isSameAs(mappedPage);

        ArgumentCaptor<PointQuery> captor = ArgumentCaptor.forClass(PointQuery.class);
        verify(pointService).selectByPage(captor.capture());
        assertThat(captor.getValue().getDeviceId()).isEqualTo(11L);
        assertThat(captor.getValue().getProfileId()).isNull();
    }

    @Test
    void deviceDictionaryProjectsLabelOntoDeviceName() {
        Page<DeviceBO> sourcePage = new Page<>();
        Page<DictionaryBO> mappedPage = new Page<>();
        when(deviceService.selectByPage(any(DeviceQuery.class))).thenReturn(sourcePage);
        when(dictionaryBuilder.buildVOPageByDeviceBOPage(sourcePage)).thenReturn(mappedPage);

        DictionaryQuery query = new DictionaryQuery();
        query.setLabel("Boiler");
        assertThat(service.deviceDictionary(query)).isSameAs(mappedPage);

        ArgumentCaptor<DeviceQuery> captor = ArgumentCaptor.forClass(DeviceQuery.class);
        verify(deviceService).selectByPage(captor.capture());
        assertThat(captor.getValue().getDeviceName()).isEqualTo("Boiler");
        assertThat(captor.getValue().getDriverId()).isNull();
    }

    @Test
    void deviceDictionaryForDriverProjectsParentIdOntoDriverId() {
        Page<DeviceBO> sourcePage = new Page<>();
        Page<DictionaryBO> mappedPage = new Page<>();
        when(deviceService.selectByPage(any(DeviceQuery.class))).thenReturn(sourcePage);
        when(dictionaryBuilder.buildVOPageByDeviceBOPage(sourcePage)).thenReturn(mappedPage);

        DictionaryQuery query = new DictionaryQuery();
        query.setParentId(7L);
        assertThat(service.deviceDictionaryForDriver(query)).isSameAs(mappedPage);

        ArgumentCaptor<DeviceQuery> captor = ArgumentCaptor.forClass(DeviceQuery.class);
        verify(deviceService).selectByPage(captor.capture());
        assertThat(captor.getValue().getDriverId()).isEqualTo(7L);
    }
}
