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

package io.github.pnoker.common.data.cache;

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class PointValueLocalCacheTest {

    private LocalCacheImpl localCache;
    private PointValueLocalCache service;

    private static PointValueBO pv(Long tenantId, Long deviceId, Long pointId, String raw) {
        return PointValueBO.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .pointId(pointId)
                .rawValue(raw)
                .build();
    }

    @BeforeEach
    void setUp() {
        localCache = new LocalCacheImpl();
        localCache.init();
        service = new PointValueLocalCache(localCache);
    }

    @Test
    void singleSaveSilentlyDropsEntriesMissingTenantOrDeviceOrPoint() {
        assertThatNoException().isThrownBy(() -> service.savePointValue(pv(null, 10L, 20L, "v")));
        assertThatNoException().isThrownBy(() -> service.savePointValue(pv(1L, null, 20L, "v")));
        assertThatNoException().isThrownBy(() -> service.savePointValue(pv(1L, 10L, null, "v")));

        assertThat(service.selectLatestPointValue(1L, 10L, List.of(20L))).isEmpty();
    }

    @Test
    void singleSaveStoresUnderTenantDevicePointKey() {
        PointValueBO bo = pv(1L, 10L, 20L, "42.5");
        service.savePointValue(bo);
        Map<Long, PointValueBO> hits = service.selectLatestPointValue(1L, 10L, List.of(20L));
        assertThat(hits).containsKey(20L);
        assertThat(hits.get(20L).getRawValue()).isEqualTo("42.5");
        // The internal key shape is part of the contract for cross-service consumers.
        String key = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + "1" + SymbolConstant.DOT + "10"
                + SymbolConstant.DOT + "20";
        assertThat((PointValueBO) localCache.getKey(key)).isSameAs(bo);
    }

    @Test
    void batchSaveSilentlyDropsBlankInputs() {
        assertThatNoException().isThrownBy(() -> service.savePointValue(null, List.of(pv(1L, 10L, 20L, "v"))));
        assertThatNoException().isThrownBy(() -> service.savePointValue(10L, null));
        assertThatNoException().isThrownBy(() -> service.savePointValue(10L, List.of()));
    }

    @Test
    void batchSaveSkipsEntriesMissingTenantOrPointButKeepsTheRest() {
        PointValueBO ok = pv(1L, 10L, 20L, "ok");
        PointValueBO missingTenant = pv(null, 10L, 21L, "skipped");
        PointValueBO missingPoint = pv(1L, 10L, null, "skipped");

        service.savePointValue(10L, List.of(ok, missingTenant, missingPoint));
        Map<Long, PointValueBO> hits = service.selectLatestPointValue(1L, 10L, List.of(20L, 21L));
        assertThat(hits).containsOnlyKeys(20L);
    }

    @Test
    void selectLatestReturnsEmptyForBlankInputs() {
        assertThat(service.selectLatestPointValue(null, 10L, List.of(20L))).isEmpty();
        assertThat(service.selectLatestPointValue(1L, null, List.of(20L))).isEmpty();
        assertThat(service.selectLatestPointValue(1L, 10L, List.of())).isEmpty();
        assertThat(service.selectLatestPointValue(1L, 10L, null)).isEmpty();
    }

    @Test
    void selectLatestReturnsOnlyHitsForRequestedPointIds() {
        service.savePointValue(pv(1L, 10L, 20L, "a"));
        // 21L is intentionally not seeded — should be missing from result.
        Map<Long, PointValueBO> hits = service.selectLatestPointValue(1L, 10L, List.of(20L, 21L));
        assertThat(hits).hasSize(1).containsKey(20L);
    }
}
