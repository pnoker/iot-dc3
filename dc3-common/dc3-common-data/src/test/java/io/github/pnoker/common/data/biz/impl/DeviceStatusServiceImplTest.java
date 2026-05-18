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
import io.github.pnoker.common.data.entity.query.DeviceQuery;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
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
class DeviceStatusServiceImplTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private LocalCacheService localCacheService;

    @InjectMocks
    private DeviceStatusServiceImpl service;

    private FacadeDeviceBO device(Long id) {
        FacadeDeviceBO bo = new FacadeDeviceBO();
        bo.setId(id);
        return bo;
    }

    @Test
    void selectByPageReturnsEmptyMapForEmptyPage() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of());
        when(deviceFacade.listByPage(any())).thenReturn(page);
        assertThat(service.selectByPage(new DeviceQuery())).isEmpty();
    }

    @Test
    void selectByPageDefaultsToOfflineWhenCacheMissing() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of(device(10L)));
        when(deviceFacade.listByPage(any())).thenReturn(page);
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L)).thenReturn(null);
        assertThat(service.selectByPage(new DeviceQuery()))
                .containsEntry(10L, DeviceStatusEnum.OFFLINE.getCode());
    }

    @Test
    void selectByPageReturnsCachedStatus() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of(device(10L)));
        when(deviceFacade.listByPage(any())).thenReturn(page);
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L))
                .thenReturn(DeviceStatusEnum.ONLINE.getCode());
        assertThat(service.selectByPage(new DeviceQuery()))
                .containsEntry(10L, DeviceStatusEnum.ONLINE.getCode());
    }

    @Test
    void selectByProfileIdReturnsEmptyMapForEmptyDevices() {
        when(deviceFacade.listByProfileId(1L, 5L)).thenReturn(List.of());
        assertThat(service.listByProfileId(1L, 5L)).isEmpty();
    }

    @Test
    void selectByProfileIdMapsAllDevicesToCachedOrOfflineStatus() {
        when(deviceFacade.listByProfileId(1L, 5L)).thenReturn(List.of(device(10L), device(11L)));
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L))
                .thenReturn(DeviceStatusEnum.ONLINE.getCode());
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 11L)).thenReturn(null);

        assertThat(service.listByProfileId(1L, 5L))
                .containsEntry(10L, DeviceStatusEnum.ONLINE.getCode())
                .containsEntry(11L, DeviceStatusEnum.OFFLINE.getCode());
    }
}
