package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ParamDirectionTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.repository.CommandParamFilter;
import io.github.pnoker.common.manager.repository.ReactiveCommandParamStore;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveCommandParamServiceImplTest {
    @Mock ReactiveCommandParamStore store;
    @Mock ReactiveCommandService commands;
    @Mock ReactiveDeviceService devices;
    @Mock ReactiveDriverService drivers;
    @Mock MetadataEventPublisher publisher;

    @Test
    void addKeepsTenantScopeAndPublishesOnlyAfterPersistence() {
        CommandParamBO value = value();
        CommandBO command = new CommandBO(); command.setId(10L); command.setTenantId(7L); command.setProfileId(20L);
        when(commands.getById(7L, 10L)).thenReturn(Mono.just(command));
        when(store.existsByNameOrCode(7L, 10L, "speed", "speed", null)).thenReturn(Mono.just(false));
        when(store.insert(value)).thenReturn(Mono.just(value));
        when(devices.listByProfileId(7L, 20L)).thenReturn(Flux.empty());

        StepVerifier.create(service().add(value)).expectNext(value).verifyComplete();

        verify(store).existsByNameOrCode(7L, 10L, "speed", "speed", null);
        verify(store).insert(value);
        verify(publisher).publishEvent(argThat(event -> event.getId().equals(10L) && event.getTargetServices().isEmpty()));
    }

    @Test
    void updateRejectsOptimisticLockConflict() {
        CommandParamBO value = value(); value.setVersion(2);
        when(store.get(7L, 1L)).thenReturn(Mono.just(value));
        when(commands.getById(7L, 10L)).thenReturn(Mono.just(new CommandBO()));
        when(store.existsByNameOrCode(7L, 10L, "speed", "speed", 1L)).thenReturn(Mono.just(false));
        when(store.update(value, 2)).thenReturn(Mono.empty());

        StepVerifier.create(service().update(value)).expectErrorMessage("Command param version conflict").verify();
        verifyNoInteractions(devices, drivers, publisher);
    }

    @Test
    void updateRejectsParentReassignment() {
        CommandParamBO current = value();
        CommandParamBO requested = value(); requested.setCommandId(11L);
        when(store.get(7L, 1L)).thenReturn(Mono.just(current));
        StepVerifier.create(service().update(requested)).expectErrorMessage("Command param cannot be moved to another command").verify();
        verifyNoInteractions(commands, devices, drivers, publisher);
    }

    @Test
    void listDelegatesImmutableOffsetFilter() {
        CommandParamFilter filter = new CommandParamFilter(7L, null, null, null, null, null, null, null, 20, 10, List.of());
        OffsetPage<CommandParamBO> page = OffsetPage.of(List.of(), 20, 10, 20);
        when(store.list(filter)).thenReturn(Mono.just(page));
        StepVerifier.create(service().list(filter)).expectNext(page).verifyComplete();
    }

    private ReactiveCommandParamServiceImpl service() { return new ReactiveCommandParamServiceImpl(store, commands, devices, drivers, publisher); }
    private CommandParamBO value() { CommandParamBO value = new CommandParamBO(); value.setId(1L); value.setTenantId(7L); value.setCommandId(10L); value.setParamName("speed"); value.setParamCode("speed"); value.setParamDirectionFlag(ParamDirectionTypeEnum.INPUT); value.setParamTypeFlag(PointTypeEnum.INT); value.setRequiredFlag(true); value.setEnableFlag(EnableFlagEnum.ENABLE); value.setVersion(0); return value; }
}
