package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.CommandAttributeFilter;
import io.github.pnoker.common.manager.repository.ReactiveCommandAttributeStore;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveCommandAttributeServiceImplTest {
    @Mock ReactiveCommandAttributeStore store;
    @Mock ReactiveDriverService drivers;
    @Mock MetadataEventPublisher publisher;

    @Test
    void addValidatesDriverAndReturnsSavedAttribute() {
        CommandAttributeBO value = value(null);
        CommandAttributeBO saved = value(10L);
        when(drivers.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.manager.entity.bo.DriverBO()));
        when(store.getByCodeAndDriver(7L, "FUNC", 11L)).thenReturn(Mono.empty());
        when(store.insert(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(new ReactiveCommandAttributeServiceImpl(store, drivers, publisher).add(value))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(10L)).verifyComplete();
        verify(store).insert(value);
        verify(publisher).publishEvent(any());
    }

    @Test
    void duplicateCodeFailsBeforeInsert() {
        CommandAttributeBO value = value(null);
        when(drivers.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.manager.entity.bo.DriverBO()));
        when(store.getByCodeAndDriver(7L, "FUNC", 11L)).thenReturn(Mono.just(value(12L)));

        StepVerifier.create(new ReactiveCommandAttributeServiceImpl(store, drivers, publisher).add(value))
                .expectErrorMessage("Command attribute has been duplicated").verify();
        verify(store, never()).insert(any());
    }

    @Test
    void listDelegatesCanonicalOffsetFilter() {
        var filter = new CommandAttributeFilter(7L, null, null, null, 11L, null, null, 20, 10, List.of());
        var page = OffsetPage.of(List.<CommandAttributeBO>of(), 20, 10, 20);
        when(store.list(filter)).thenReturn(Mono.just(page));
        StepVerifier.create(new ReactiveCommandAttributeServiceImpl(store, drivers, publisher).list(filter))
                .expectNext(page).verifyComplete();
        verify(store).list(filter);
    }

    private CommandAttributeBO value(Long id) {
        CommandAttributeBO value = new CommandAttributeBO();
        value.setId(id); value.setTenantId(7L); value.setDriverId(11L);
        value.setAttributeName("Function"); value.setAttributeCode("FUNC");
        value.setAttributeTypeFlag(AttributeTypeEnum.STRING); value.setVersion(0);
        return value;
    }
}
