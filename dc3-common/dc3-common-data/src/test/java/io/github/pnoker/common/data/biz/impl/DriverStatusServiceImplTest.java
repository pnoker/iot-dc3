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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.entity.query.DriverQuery;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverStatusServiceImplTest {

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private LocalCacheService localCacheService;

    @InjectMocks
    private DriverStatusServiceImpl service;

    private FacadeDriverBO driver(Long id) {
        FacadeDriverBO bo = new FacadeDriverBO();
        bo.setId(id);
        return bo;
    }

    private FacadeDeviceBO device(Long id) {
        FacadeDeviceBO bo = new FacadeDeviceBO();
        bo.setId(id);
        return bo;
    }

    @Test
    void selectByPageReturnsEmptyMapForEmptyPage() {
        FacadePage<FacadeDriverBO> page = new FacadePage<>();
        page.setRecords(List.of());
        when(driverFacade.listByPage(any())).thenReturn(page);
        assertThat(service.selectByPage(new DriverQuery())).isEmpty();
    }

    @Test
    void selectByPageDefaultsToOfflineWhenCacheMissing() {
        FacadePage<FacadeDriverBO> page = new FacadePage<>();
        page.setRecords(List.of(driver(1L)));
        when(driverFacade.listByPage(any())).thenReturn(page);
        when(localCacheService.getKey(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + 1L)).thenReturn(null);
        assertThat(service.selectByPage(new DriverQuery()))
                .containsEntry(1L, DriverStatusEnum.OFFLINE.getCode());
    }

    @Test
    void selectByPageReturnsCachedStatus() {
        FacadePage<FacadeDriverBO> page = new FacadePage<>();
        page.setRecords(List.of(driver(1L)));
        when(driverFacade.listByPage(any())).thenReturn(page);
        when(localCacheService.getKey(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + 1L))
                .thenReturn(DriverStatusEnum.ONLINE.getCode());
        assertThat(service.selectByPage(new DriverQuery()))
                .containsEntry(1L, DriverStatusEnum.ONLINE.getCode());
    }

    @Test
    void getDeviceOnlineByDriverIdReturnsZeroWhenDriverMissing() {
        when(driverFacade.getById(1L, 7L)).thenReturn(null);
        assertThat(service.getDeviceOnlineByDriverId(1L, 7L)).isEqualTo("0");
    }

    @Test
    void getDeviceOnlineByDriverIdReturnsZeroWhenNoDevices() {
        when(driverFacade.getById(1L, 7L)).thenReturn(driver(7L));
        when(deviceFacade.listByDriverId(1L, 7L)).thenReturn(List.of());
        assertThat(service.getDeviceOnlineByDriverId(1L, 7L)).isEqualTo("0");
    }

    @Test
    void getDeviceOnlineByDriverIdCountsOnlineDevices() {
        when(driverFacade.getById(1L, 7L)).thenReturn(driver(7L));
        when(deviceFacade.listByDriverId(1L, 7L)).thenReturn(List.of(device(10L), device(11L)));
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L))
                .thenReturn(DeviceStatusEnum.ONLINE.getCode());
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 11L))
                .thenReturn(DeviceStatusEnum.OFFLINE.getCode());
        assertThat(service.getDeviceOnlineByDriverId(1L, 7L)).isEqualTo("1");
    }

    @Test
    void getDeviceOnlineByDriverIdTreatsMissingCacheAsOffline() {
        when(driverFacade.getById(1L, 7L)).thenReturn(driver(7L));
        when(deviceFacade.listByDriverId(1L, 7L)).thenReturn(List.of(device(10L)));
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L)).thenReturn(null);
        assertThat(service.getDeviceOnlineByDriverId(1L, 7L)).isEqualTo("0");
    }

    @Test
    void getDeviceOfflineByDriverIdCountsOfflineAndMissingDevices() {
        when(driverFacade.getById(1L, 7L)).thenReturn(driver(7L));
        when(deviceFacade.listByDriverId(1L, 7L)).thenReturn(List.of(device(10L), device(11L), device(12L)));
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L))
                .thenReturn(DeviceStatusEnum.ONLINE.getCode());
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 11L))
                .thenReturn(DeviceStatusEnum.OFFLINE.getCode());
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 12L))
                .thenReturn(null);
        assertThat(service.getDeviceOfflineByDriverId(1L, 7L)).isEqualTo("2");
    }
}
