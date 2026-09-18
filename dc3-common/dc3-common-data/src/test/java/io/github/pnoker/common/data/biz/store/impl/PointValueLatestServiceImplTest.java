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
package io.github.pnoker.common.data.biz.store.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.data.repository.ReactivePointValueLatestStore;
import io.github.pnoker.common.entity.bo.PointValueBO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PointValueLatestServiceImplTest {

    @Mock
    private ReactivePointValueLatestStore store;

    @Mock
    private PointValueBuilder builder;

    @Mock
    private PointValueDO row;

    @Mock
    private PointValueBO value;

    @Test
    void latestMapsProjectionWithoutBlocking() {
        when(store.latest(1L, 2L, 3L)).thenReturn(Mono.just(row));
        when(builder.buildBOByDO(row)).thenReturn(value);

        StepVerifier.create(new PointValueLatestServiceImpl(store, builder).latest(1L, 2L, 3L))
                .expectNext(value)
                .verifyComplete();
        verify(builder).buildBOByDO(row);
    }

    @Test
    void listLatestPreservesStoreOrder() {
        PointValueDO second = new PointValueDO();
        when(store.listLatest(1L, 2L, List.of(3L, 4L))).thenReturn(Flux.just(row, second));
        when(builder.buildBOByDO(row)).thenReturn(value);
        PointValueBO secondValue = new PointValueBO();
        when(builder.buildBOByDO(second)).thenReturn(secondValue);

        StepVerifier.create(new PointValueLatestServiceImpl(store, builder).listLatest(1L, 2L, List.of(3L, 4L)))
                .expectNext(value, secondValue)
                .verifyComplete();
    }
}
