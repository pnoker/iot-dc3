package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.builder.ApiBuilder;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.repository.ApiFilter;
import io.github.pnoker.common.auth.repository.ReactiveApiStore;
import io.github.pnoker.common.enums.ApiTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveApiServiceImplTest {

    @Mock ReactiveApiStore store;
    @Mock ApiBuilder builder;

    @Test
    void listUsesOffsetPageAndMapsRows() {
        ApiDO row = row(1L, "get_device");
        when(store.list(any(ApiFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(row), 20, 10, 31)));
        ApiBO mapped = api("get_device");
        when(builder.buildBOByDO(row)).thenReturn(mapped);

        StepVerifier.create(service().list(new ApiFilter(null, null, null, null, null, null,
                        new PageRequest(20, 10))))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(mapped);
                    assertThat(page.offset()).isEqualTo(20);
                    assertThat(page.limit()).isEqualTo(10);
                    assertThat(page.total()).isEqualTo(31);
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void addRejectsDuplicateBeforeInsert() {
        ApiBO api = api("get_device");
        when(store.existsDuplicate(api)).thenReturn(Mono.just(true));

        StepVerifier.create(service().add(api))
                .expectError(DuplicateException.class)
                .verify();
        verify(store, never()).insert(any(ApiBO.class));
    }

    @Test
    void addMapsDatabaseDuplicate() {
        ApiBO api = api("get_device");
        when(store.existsDuplicate(api)).thenReturn(Mono.just(false));
        when(store.insert(api)).thenReturn(Mono.error(new org.springframework.dao.DuplicateKeyException("duplicate")));

        StepVerifier.create(service().add(api))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void addFailsWhenStoreReturnsNoRow() {
        ApiBO api = api("get_device");
        when(store.existsDuplicate(api)).thenReturn(Mono.just(false));
        when(store.insert(api)).thenReturn(Mono.empty());

        StepVerifier.create(service().add(api))
                .expectError(io.github.pnoker.common.exception.ServiceException.class)
                .verify();
    }

    @Test
    void updateRequiresExistingRecord() {
        ApiBO api = api("get_device");
        api.setId(10L);
        when(store.existsDuplicate(api)).thenReturn(Mono.just(false));
        when(store.update(api)).thenReturn(Mono.empty());

        StepVerifier.create(service().update(api))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void deletePropagatesNotFound() {
        when(store.getById(42L)).thenReturn(Mono.empty());

        StepVerifier.create(service().delete(42L, 1L, "admin"))
                .expectError(NotFoundException.class)
                .verify();
        verify(store, never()).delete(42L, 1L, "admin");
    }

    @Test
    void invalidIdFailsFast() {
        StepVerifier.create(service().getById(0L))
                .expectError(RequestException.class)
                .verify();
    }

    private ReactiveApiServiceImpl service() {
        return new ReactiveApiServiceImpl(store, builder);
    }

    private ApiBO api(String code) {
        ApiBO api = new ApiBO();
        api.setApiName("GetDevice");
        api.setApiCode(code);
        api.setApiTypeFlag(ApiTypeEnum.GET);
        api.setEnableFlag(EnableFlagEnum.ENABLE);
        return api;
    }

    private ApiDO row(Long id, String code) {
        ApiDO row = new ApiDO();
        row.setId(id);
        row.setApiCode(code);
        return row;
    }
}
