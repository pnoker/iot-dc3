package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.query.DictionaryListRequest;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryForManagerServiceImplTest {

    @Mock
    private ReactiveDriverService driverService;

    @Mock
    private ReactiveProfileService profileService;

    @Mock
    private ReactiveDeviceService deviceService;

    @Mock
    private ReactivePointService pointService;

    @Test
    void listDriverOptionsMapsStableOffsetPage() {
        DriverBO driver = new DriverBO();
        driver.setId(7L);
        driver.setDriverName("Modbus TCP");
        when(driverService.list(any(DriverFilter.class)))
                .thenReturn(Mono.just(OffsetPage.of(List.of(driver), 20, 10, 21)));
        DictionaryListRequest request = new DictionaryListRequest(
                20L,
                10,
                List.of(new SortSpec("label", SortSpec.Direction.ASC)),
                "Modbus",
                null);

        StepVerifier.create(service().listDriverOptions(9L, request))
                .assertNext(page -> {
                    assertThat(page.offset()).isEqualTo(20);
                    assertThat(page.total()).isEqualTo(21);
                    assertThat(page.hasNext()).isFalse();
                    assertThat(page.items()).singleElement().satisfies(option -> {
                        assertThat(option.label()).isEqualTo("Modbus TCP");
                        assertThat(option.value()).isEqualTo("7");
                    });
                })
                .verifyComplete();

        ArgumentCaptor<DriverFilter> filter = ArgumentCaptor.forClass(DriverFilter.class);
        verify(driverService).list(filter.capture());
        assertThat(filter.getValue().tenantId()).isEqualTo(9L);
        assertThat(filter.getValue().driverName()).isEqualTo("Modbus");
        assertThat(filter.getValue().sort())
                .containsExactly(new SortSpec("driverName", SortSpec.Direction.ASC));
    }

    @Test
    void listDriverDeviceOptionsRequiresParentBeforeCallingStore() {
        DictionaryListRequest request = new DictionaryListRequest(0L, 50, List.of(), null, null);

        StepVerifier.create(service().listDriverDeviceOptions(9L, request))
                .expectErrorMessage("Parent ID is required")
                .verify();

        verify(deviceService, never()).list(any());
    }

    @Test
    void valueSortTranslatesToStableIdSort() {
        DeviceBO device = new DeviceBO();
        device.setId(3L);
        device.setDeviceName("Boiler");
        when(deviceService.list(any())).thenReturn(Mono.just(OffsetPage.of(List.of(device), 0, 50, 1)));
        DictionaryListRequest request = new DictionaryListRequest(
                0L,
                50,
                List.of(new SortSpec("value", SortSpec.Direction.DESC)),
                null,
                null);

        StepVerifier.create(service().listDeviceOptions(9L, request))
                .expectNextCount(1)
                .verifyComplete();

        var filter = ArgumentCaptor.forClass(io.github.pnoker.common.manager.repository.DeviceFilter.class);
        verify(deviceService).list(filter.capture());
        assertThat(filter.getValue().sort()).containsExactly(new SortSpec("id", SortSpec.Direction.DESC));
    }

    private DictionaryForManagerServiceImpl service() {
        return new DictionaryForManagerServiceImpl(driverService, profileService, deviceService, pointService);
    }

}
