package io.github.pnoker.common.data.biz.store.impl;

import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.data.repository.ReactivePointValueLatestStore;
import io.github.pnoker.common.entity.bo.PointValueBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                .expectNext(value).verifyComplete();
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
                .expectNext(value, secondValue).verifyComplete();
    }
}
