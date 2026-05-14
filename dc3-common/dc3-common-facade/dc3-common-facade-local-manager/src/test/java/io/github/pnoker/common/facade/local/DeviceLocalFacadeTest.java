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

package io.github.pnoker.common.facade.local;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDeviceBuilder;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceLocalFacadeTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private FacadeDeviceBuilder facadeDeviceBuilder;

    private DeviceLocalFacade facade;

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    @BeforeEach
    void setUp() throws Exception {
        facade = new DeviceLocalFacade();
        injectField("deviceService", deviceService);
        injectField("facadeDeviceBuilder", facadeDeviceBuilder);
    }

    @Test
    void selectByIdReturnsNullWhenServiceReturnsNull() {
        when(deviceService.selectById(1L)).thenReturn(null);
        assertThat(facade.selectById(1L)).isNull();
        verify(facadeDeviceBuilder, never()).toFacadeBO(any());
    }

    @Test
    void selectByIdMapsThroughBuilder() {
        DeviceBO source = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        when(deviceService.selectById(1L)).thenReturn(source);
        when(facadeDeviceBuilder.toFacadeBO(source)).thenReturn(mapped);
        assertThat(facade.selectById(1L)).isSameAs(mapped);
    }

    @Test
    void selectByIdsReturnsEmptyForNullOrEmptyInput() {
        assertThat(facade.selectByIds(null)).isEmpty();
        assertThat(facade.selectByIds(Set.of())).isEmpty();
        verify(deviceService, never()).selectByIds(any());
    }

    @Test
    void selectByIdsReturnsEmptyWhenServiceReturnsNullOrEmpty() {
        when(deviceService.selectByIds(any())).thenReturn(null);
        assertThat(facade.selectByIds(Set.of(1L))).isEmpty();

        when(deviceService.selectByIds(any())).thenReturn(List.of());
        assertThat(facade.selectByIds(Set.of(1L))).isEmpty();
    }

    @Test
    void selectByIdsMapsEachThroughBuilder() {
        DeviceBO bo1 = new DeviceBO();
        DeviceBO bo2 = new DeviceBO();
        FacadeDeviceBO mapped1 = new FacadeDeviceBO();
        FacadeDeviceBO mapped2 = new FacadeDeviceBO();
        when(deviceService.selectByIds(any())).thenReturn(List.of(bo1, bo2));
        when(facadeDeviceBuilder.toFacadeBO(bo1)).thenReturn(mapped1);
        when(facadeDeviceBuilder.toFacadeBO(bo2)).thenReturn(mapped2);

        assertThat(facade.selectByIds(Set.of(1L, 2L))).containsExactly(mapped1, mapped2);
    }

    @Test
    void selectByPageReturnsEmptyWhenServiceReturnsNullPage() {
        FacadeDeviceQuery query = new FacadeDeviceQuery();
        when(facadeDeviceBuilder.toManagerQuery(query)).thenReturn(new DeviceQuery());
        when(deviceService.selectByPage(any(DeviceQuery.class))).thenReturn(null);

        FacadePage<FacadeDeviceBO> page = facade.selectByPage(query);
        assertThat(page).isNotNull();
        assertThat(page.getRecords()).isEmpty();
    }

    @Test
    void selectByPageMapsRecordsAndCarriesPaginationMeta() {
        FacadeDeviceQuery query = new FacadeDeviceQuery();
        when(facadeDeviceBuilder.toManagerQuery(query)).thenReturn(new DeviceQuery());

        DeviceBO bo = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        Page<DeviceBO> page = new Page<>(2, 20, 100);
        page.setRecords(List.of(bo));
        when(deviceService.selectByPage(any(DeviceQuery.class))).thenReturn(page);
        when(facadeDeviceBuilder.toFacadeBO(bo)).thenReturn(mapped);

        FacadePage<FacadeDeviceBO> result = facade.selectByPage(query);
        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotal()).isEqualTo(100);
        assertThat(result.getRecords()).containsExactly(mapped);
    }

    @Test
    void selectByProfileIdMapsResults() {
        DeviceBO bo = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        when(deviceService.selectByProfileId(5L)).thenReturn(List.of(bo));
        when(facadeDeviceBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.selectByProfileId(5L)).containsExactly(mapped);
    }

    @Test
    void selectByProfileIdReturnsEmptyWhenServiceReturnsNullOrEmpty() {
        when(deviceService.selectByProfileId(5L)).thenReturn(null);
        assertThat(facade.selectByProfileId(5L)).isEmpty();
        when(deviceService.selectByProfileId(5L)).thenReturn(List.of());
        assertThat(facade.selectByProfileId(5L)).isEmpty();
    }

    @Test
    void selectByDriverIdMapsResults() {
        DeviceBO bo = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        when(deviceService.selectByDriverId(9L)).thenReturn(List.of(bo));
        when(facadeDeviceBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.selectByDriverId(9L)).containsExactly(mapped);
    }

    @Test
    void selectByDriverIdReturnsEmptyWhenServiceReturnsNullOrEmpty() {
        when(deviceService.selectByDriverId(9L)).thenReturn(null);
        assertThat(facade.selectByDriverId(9L)).isEmpty();
        when(deviceService.selectByDriverId(9L)).thenReturn(List.of());
        assertThat(facade.selectByDriverId(9L)).isEmpty();
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = DeviceLocalFacade.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(facade, value);
    }
}
