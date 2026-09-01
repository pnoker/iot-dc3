package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;
import io.github.pnoker.common.data.entity.builder.PointCommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.data.repository.ReactivePointCommandContext;
import io.github.pnoker.common.data.repository.ReactivePointCommandStore;
import io.github.pnoker.common.data.validator.PointCommandValidator;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PointCommandServiceImplTest {

    @Mock ReactivePointCommandContext context;
    @Mock ReactivePointCommandStore store;
    @Mock ReactiveMessageSender sender;
    @Mock PointCommandHistoryBuilder builder;
    @Mock PointCommandValidator validator;
    private PointCommandServiceImpl service;
    private FacadeDeviceBO device;
    private FacadePointBO point;
    private FacadeDriverBO driver;
    private FacadeDeviceOwnerBO owner;

    @BeforeEach
    void setUp() {
        service = new PointCommandServiceImpl(context, store, sender, builder, validator);
        device = new FacadeDeviceBO(); device.setId(10L); device.setProfileId(5L); device.setEnableFlag(EnableFlagEnum.ENABLE);
        point = new FacadePointBO(); point.setId(20L); point.setProfileId(5L); point.setEnableFlag(EnableFlagEnum.ENABLE); point.setRwFlag(RwTypeEnum.READ_WRITE);
        driver = new FacadeDriverBO(); driver.setId(30L); driver.setServiceName("driver");
        owner = new FacadeDeviceOwnerBO(30L, "node", 7L);
        when(store.find(anyLong(), anyString())).thenReturn(Mono.empty());
        when(store.insert(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(store.markSent(anyLong(), anyString(), any())).thenReturn(Mono.just(true));
        when(sender.sendConfirmed(any())).thenReturn(Mono.empty());
        when(context.device(1L, 10L)).thenReturn(Mono.just(device));
        when(context.point(1L, 20L)).thenReturn(Mono.just(point));
        when(context.driverByDevice(1L, 10L)).thenReturn(Mono.just(driver));
        when(context.activeOwner(1L, 10L)).thenReturn(Mono.just(owner));
    }

    @Test
    void readReturnsCommandIdAfterBrokerConfirmation() {
        PointCommandReadBO request = new PointCommandReadBO(10L, 20L, null);
        StepVerifier.create(service.read(1L, request)).assertNext(id -> assertThat(id).isNotBlank()).verifyComplete();
        verify(store).insert(any(PointCommandHistoryDO.class));
        verify(store).markSent(eq(1L), anyString(), any());
        verify(sender).sendConfirmed(any());
    }

    @Test
    void writeRejectsReadOnlyPointWithoutPersistence() {
        point.setRwFlag(RwTypeEnum.READ_ONLY);
        PointCommandWriteBO request = new PointCommandWriteBO(10L, 20L, "42", null);
        StepVerifier.create(service.write(1L, request)).expectErrorMessage("Point is not writable").verify();
        verifyNoInteractions(store, sender);
    }

    @Test
    void missingDeviceIsNotFound() {
        when(context.device(1L, 10L)).thenReturn(Mono.empty());
        StepVerifier.create(service.read(1L, new PointCommandReadBO(10L, 20L, null)))
                .expectError(NotFoundException.class).verify();
    }

    @Test
    void brokerFailureMarksCommandFailed() {
        when(sender.sendConfirmed(any())).thenReturn(Mono.error(new IllegalStateException("nack")));
        when(store.markPublishFailed(anyLong(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(true));
        StepVerifier.create(service.read(1L, new PointCommandReadBO(10L, 20L, null)))
                .expectErrorMessage("Failed to route point command to active driver owner").verify();
        verify(store).markPublishFailed(eq(1L), anyString(), eq("BROKER_PUBLISH_FAILED"), eq("nack"), any());
    }

    @Test
    void idempotentRetryReturnsExistingCommandBeforeResourceLookup() {
        PointCommandHistoryDO existing = new PointCommandHistoryDO();
        existing.setTenantId(1L);
        existing.setCommandId("cmd-existing");
        existing.setDeviceId(10L);
        existing.setPointId(20L);
        existing.setType(io.github.pnoker.common.enums.PointCommandTypeEnum.READ);
        when(store.find(1L, "cmd-existing")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.read(1L, new PointCommandReadBO(10L, 20L, "cmd-existing")))
                .expectNext("cmd-existing").verifyComplete();
        verifyNoInteractions(context, sender);
    }

    @Test
    void commandSourceIsPersistedForNonHttpSubmission() {
        PointCommandReadBO request = new PointCommandReadBO(10L, 20L, null, PointCommandSourceEnum.AGENTIC);
        ArgumentCaptor<PointCommandHistoryDO> captor = ArgumentCaptor.forClass(PointCommandHistoryDO.class);
        StepVerifier.create(service.read(1L, request)).expectNextCount(1).verifyComplete();
        verify(store).insert(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(PointCommandSourceEnum.AGENTIC);
    }
}
