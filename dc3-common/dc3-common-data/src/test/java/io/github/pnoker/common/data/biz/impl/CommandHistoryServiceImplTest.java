package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.entity.bo.CommandCallBO;
import io.github.pnoker.common.data.entity.builder.CommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.repository.ReactiveCommandHistoryStore;
import io.github.pnoker.common.data.repository.ReactivePointCommandContext;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandHistoryServiceImplTest {

    @Mock DeviceFacade deviceFacade;
    @Mock DriverFacade driverFacade;
    @Mock CommandFacade commandFacade;
    @Mock ReactivePointCommandContext context;
    @Mock ReactiveCommandHistoryStore historyStore;
    @Mock ReactiveMessageSender sender;
    @Mock CommandHistoryBuilder builder;

    private CommandHistoryServiceImpl service;
    private FacadeDeviceBO device;
    private FacadeCommandBO command;
    private FacadeDriverBO driver;

    @BeforeEach
    void setUp() {
        service = new CommandHistoryServiceImpl(deviceFacade, driverFacade, commandFacade, context,
                historyStore, sender, builder);
        device = new FacadeDeviceBO();
        device.setId(10L); device.setDriverId(40L); device.setProfileId(30L); device.setEnableFlag(EnableFlagEnum.ENABLE);
        command = new FacadeCommandBO();
        command.setId(20L); command.setProfileId(30L); command.setCommandCode("restart");
        command.setTimeout(5); command.setEnableFlag(EnableFlagEnum.ENABLE);
        driver = new FacadeDriverBO(); driver.setId(40L); driver.setServiceName("driver");
        when(deviceFacade.getByIdReactive(100L, 10L)).thenReturn(Mono.just(device));
        when(commandFacade.getById(100L, 20L)).thenReturn(Mono.just(command));
        when(driverFacade.getByIdReactive(100L, 40L)).thenReturn(Mono.just(driver));
        when(context.activeOwner(100L, 10L)).thenReturn(Mono.just(new FacadeDeviceOwnerBO(40L, "node", 1L)));
        when(historyStore.insert(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(historyStore.markSent(any(), any(), any())).thenReturn(Mono.just(true));
        when(sender.sendConfirmed(any())).thenReturn(Mono.empty());
    }

    @Test
    void callPersistsAndPublishesReactively() {
        CommandCallBO request = request(Map.of("mode", "soft"));
        StepVerifier.create(service.call(100L, request)).assertNext(recordId -> assertThat(recordId).isNotBlank()).verifyComplete();
        ArgumentCaptor<CommandHistoryDO> captor = ArgumentCaptor.forClass(CommandHistoryDO.class);
        verify(historyStore).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(100L);
        verify(sender).sendConfirmed(any());
        verify(historyStore).markSent(any(), any(), any());
    }

    @Test
    void publishFailureMarksHistoryAndPropagates() {
        when(sender.sendConfirmed(any())).thenReturn(Mono.error(new IllegalStateException("nack")));
        when(historyStore.markPublishFailed(any(), any(), any(), any(), any())).thenReturn(Mono.just(true));
        StepVerifier.create(service.call(100L, request(null)))
                .expectErrorMessage("Failed to route custom command to active driver owner").verify();
        verify(historyStore).markPublishFailed(any(), any(), any(), any(), any());
    }

    private CommandCallBO request(Map<String, String> values) {
        CommandCallBO request = new CommandCallBO();
        request.setDeviceId(10L); request.setCommandId(20L); request.setParamValues(values);
        return request;
    }
}
