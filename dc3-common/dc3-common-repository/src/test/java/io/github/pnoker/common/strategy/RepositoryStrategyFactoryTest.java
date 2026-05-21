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

package io.github.pnoker.common.strategy;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.repository.RepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryStrategyFactoryTest {

    @AfterEach
    void tearDown() {
        RepositoryStrategyFactory.clear();
    }

    @Test
    void putAndGetRepositoryServiceByName() {
        RepositoryService service = new StubRepositoryService(StrategyConstant.Storage.POSTGRES);

        RepositoryStrategyFactory.put(StrategyConstant.Storage.POSTGRES, service);

        assertThat(RepositoryStrategyFactory.get()).containsExactly(service);
        assertThat(RepositoryStrategyFactory.get(StrategyConstant.Storage.POSTGRES)).isSameAs(service);
    }

    @Test
    void putRepositoryServiceUsesDeclaredName() {
        RepositoryService service = new StubRepositoryService(StrategyConstant.Storage.POSTGRES);

        RepositoryStrategyFactory.put(service);

        assertThat(RepositoryStrategyFactory.get(StrategyConstant.Storage.POSTGRES)).isSameAs(service);
    }

    @Test
    void putReplacesRepositoryServiceWithSameName() {
        RepositoryService first = new StubRepositoryService(StrategyConstant.Storage.POSTGRES);
        RepositoryService second = new StubRepositoryService(StrategyConstant.Storage.POSTGRES);

        RepositoryStrategyFactory.put(StrategyConstant.Storage.POSTGRES, first);
        RepositoryStrategyFactory.put(StrategyConstant.Storage.POSTGRES, second);

        assertThat(RepositoryStrategyFactory.get()).containsExactly(second);
        assertThat(RepositoryStrategyFactory.get(StrategyConstant.Storage.POSTGRES)).isSameAs(second);
    }

    @Test
    void removeAndClearRegisteredServices() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.POSTGRES,
                new StubRepositoryService(StrategyConstant.Storage.POSTGRES));
        RepositoryStrategyFactory.put(StrategyConstant.Storage.INFLUXDB,
                new StubRepositoryService(StrategyConstant.Storage.INFLUXDB));

        RepositoryStrategyFactory.remove(StrategyConstant.Storage.POSTGRES);

        assertThat(RepositoryStrategyFactory.get(StrategyConstant.Storage.POSTGRES)).isNull();
        assertThat(RepositoryStrategyFactory.get()).hasSize(1);

        RepositoryStrategyFactory.clear();
        assertThat(RepositoryStrategyFactory.get()).isEmpty();
    }

    @Test
    void getRemoveAndPutNormalizeNames() {
        RepositoryService service = new StubRepositoryService(StrategyConstant.Storage.POSTGRES);

        RepositoryStrategyFactory.put("  " + StrategyConstant.Storage.POSTGRES + "  ", service);

        assertThat(RepositoryStrategyFactory.get(StrategyConstant.Storage.POSTGRES)).isSameAs(service);
        assertThat(RepositoryStrategyFactory.get("  " + StrategyConstant.Storage.POSTGRES + "  ")).isSameAs(service);

        RepositoryStrategyFactory.remove("  " + StrategyConstant.Storage.POSTGRES + "  ");
        assertThat(RepositoryStrategyFactory.get()).isEmpty();
    }

    @Test
    void getAndRemoveIgnoreBlankNames() {
        assertThat(RepositoryStrategyFactory.get(null)).isNull();
        assertThat(RepositoryStrategyFactory.get("")).isNull();
        assertThat(RepositoryStrategyFactory.get(" ")).isNull();

        RepositoryStrategyFactory.remove(null);
        RepositoryStrategyFactory.remove("");
        RepositoryStrategyFactory.remove(" ");

        assertThat(RepositoryStrategyFactory.get()).isEmpty();
    }

    @Test
    void putRejectsInvalidInputs() {
        RepositoryService service = new StubRepositoryService(StrategyConstant.Storage.POSTGRES);

        assertThatThrownBy(() -> RepositoryStrategyFactory.put(null, service))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> RepositoryStrategyFactory.put(StrategyConstant.Storage.POSTGRES, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
        assertThatThrownBy(() -> RepositoryStrategyFactory.put(new StubRepositoryService(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> RepositoryStrategyFactory.put((RepositoryService) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    private record StubRepositoryService(String name) implements RepositoryService {

        @Override
        public String getRepositoryName() {
            return name;
        }

        @Override
        public void savePointValue(PointValueBO entityBO) throws IOException {
            // no-op
        }

        @Override
        public void savePointValues(List<PointValueBO> entityBOList) throws IOException {
            // no-op
        }

        @Override
        public List<String> listHistoryPointValue(Long tenantId, Long deviceId, Long pointId, int count) {
            return List.of();
        }

        @Override
        public PointValueBO selectLatestPointValue(Long tenantId, Long deviceId, Long pointId) {
            return null;
        }

        @Override
        public List<PointValueBO> listLatestPointValues(Long tenantId, Long deviceId, List<Long> pointIds) {
            return List.of();
        }

        @Override
        public Page<PointValueBO> listPagePointValue(PointValueQuery entityQuery) {
            return new Page<>();
        }

        @Override
        public io.github.pnoker.common.entity.bo.WindowAggregateResult aggregateInWindow(
                io.github.pnoker.common.entity.query.WindowAggregateRequest request) {
            return io.github.pnoker.common.entity.bo.WindowAggregateResult.empty();
        }

        @Override
        public List<PointValueBO> samplesInWindow(Long tenantId, Long deviceId, Long pointId,
                                                  java.time.LocalDateTime from, java.time.LocalDateTime to) {
            return List.of();
        }
    }

}
