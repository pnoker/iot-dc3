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
package io.github.pnoker.common.driver.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.grpc.client.DeviceClient;
import io.github.pnoker.common.exception.ServiceException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeviceMetadataTest {

    @Mock
    private DeviceClient deviceClient;

    private DriverProperties driverProperties;
    private DriverMetadata driverMetadata;
    private DeviceMetadata deviceMetadata;

    @BeforeEach
    void setUp() {
        driverProperties = new DriverProperties();
        driverProperties.getMetadata().getCache().setRecordStats(true);
        driverMetadata = new DriverMetadata();
        driverMetadata.setDeviceLeases(Map.of(10L, 1L, 11L, 2L), System.currentTimeMillis() + 60_000, 1L);
        deviceMetadata = new DeviceMetadata(driverProperties, driverMetadata, deviceClient);
    }

    @Test
    void refreshCachePopulatesCacheOnSuccess() {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        when(deviceClient.getById(10L)).thenReturn(Mono.just(device));

        StepVerifier.create(deviceMetadata.refreshCache(10L)).expectNext(device).verifyComplete();

        DeviceBO cached = deviceMetadata.getCache(10L);
        assertThat(cached).isSameAs(device);
        verify(deviceClient, times(1)).getById(10L);
    }

    @Test
    void refreshCacheEmptyResultDropsOrphanDeviceId() {
        when(deviceClient.getById(10L)).thenReturn(Mono.empty());

        StepVerifier.create(deviceMetadata.refreshCache(10L)).verifyComplete();

        assertThat(driverMetadata.getDeviceIds()).doesNotContain(10L);
        assertThat(driverMetadata.getDeviceIds()).contains(11L);
    }

    @Test
    void refreshCachePropagatesLoaderFailure() {
        when(deviceClient.getById(10L)).thenReturn(Mono.error(new RuntimeException("manager unreachable")));

        StepVerifier.create(deviceMetadata.refreshCache(10L))
                .expectErrorMatches(error -> error instanceof ServiceException
                        && error.getMessage().contains("device cache")
                        && error.getCause().getMessage().equals("manager unreachable"))
                .verify();

        assertThat(driverMetadata.getDeviceIds()).contains(10L);
    }

    @Test
    void getCacheTriggersLoaderOnMissAndReturnsValue() {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        when(deviceClient.getById(10L)).thenReturn(Mono.just(device));

        DeviceBO returned = deviceMetadata.getCache(10L);

        assertThat(returned).isSameAs(device);
        verify(deviceClient).getById(10L);
    }

    @Test
    void removeCacheInvalidatesEntry() {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        when(deviceClient.getById(10L)).thenReturn(Mono.just(device));
        StepVerifier.create(deviceMetadata.refreshCache(10L)).expectNext(device).verifyComplete();

        deviceMetadata.removeCache(10L);
        deviceMetadata.getCache(10L);

        // After invalidate the next getCache must re-issue gRPC.
        verify(deviceClient, times(2)).getById(10L);
    }

    @Test
    void clearCacheInvalidatesAllEntries() {
        DeviceBO device10 = new DeviceBO();
        device10.setId(10L);
        DeviceBO device11 = new DeviceBO();
        device11.setId(11L);
        when(deviceClient.getById(10L)).thenReturn(Mono.just(device10));
        when(deviceClient.getById(11L)).thenReturn(Mono.just(device11));
        StepVerifier.create(deviceMetadata.refreshCache(10L))
                .expectNext(device10)
                .verifyComplete();
        StepVerifier.create(deviceMetadata.refreshCache(11L))
                .expectNext(device11)
                .verifyComplete();

        deviceMetadata.clearCache();
        deviceMetadata.getCache(10L);
        deviceMetadata.getCache(11L);

        verify(deviceClient, times(2)).getById(10L);
        verify(deviceClient, times(2)).getById(11L);
    }
}
