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
package io.github.pnoker.common.agentic.service.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.model.ConnectivityCheckProfile;
import io.github.pnoker.common.agentic.entity.model.ConnectivityCheckResult;
import io.github.pnoker.common.agentic.entity.model.ConnectivityLevelResult;
import io.github.pnoker.common.agentic.entity.vo.ProviderCheckRequestVO;
import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.agentic.service.check.impl.AgenticConnectivityCheckServiceImpl;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.ConnectivityCheckLevelEnum;
import io.github.pnoker.common.enums.ConnectivityErrorTypeEnum;
import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AgenticConnectivityCheckServiceImplTest {

    @Mock
    private ReactiveModelProviderStore modelProviderStore;

    @Mock
    private ReactiveModelConfigStore modelConfigStore;

    @Mock
    private AgenticConnectivityChecker checker;

    private AgenticProperties properties;

    private AgenticConnectivityCheckServiceImpl service;

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        properties = new AgenticProperties();
        properties.setCheckCacheTtlSeconds(60);
        service = new AgenticConnectivityCheckServiceImpl(
                modelProviderStore, modelConfigStore, properties, List.of(checker));
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        lenient()
                .when(checker.supports(AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE))
                .thenReturn(true);
    }

    private ModelProviderBO savedProvider() {
        ModelProviderBO provider = new ModelProviderBO();
        provider.setId(100L);
        provider.setName("DeepSeek");
        provider.setProviderType(AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE);
        provider.setBaseUrl("https://api.deepseek.com");
        provider.setApiKey("stored-key");
        provider.setTenantId(1L);
        return provider;
    }

    private void stubPassingProbes() {
        when(checker.checkL1(any(ModelProviderBO.class)))
                .thenReturn(
                        Mono.just(ConnectivityLevelResult.passL1(120, List.of("deepseek-chat", "deepseek-reasoner"))));
        when(checker.checkL2(any(ModelProviderBO.class), eq("deepseek-chat")))
                .thenReturn(Mono.just(ConnectivityLevelResult.passL2(240, "deepseek-chat")));
    }

    @Test
    void checkProviderRunsBothLevelsAndPersistsProfile() {
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        stubPassingProbes();
        when(modelConfigStore.list(header, true)).thenReturn(Flux.just(configOf("deepseek-chat", 100L)));
        when(modelProviderStore.updateCheckProfile(eq(100L), any(ConnectivityCheckProfile.class), eq(header)))
                .thenReturn(Mono.just(true));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("100");

        StepVerifier.create(service.checkProvider(request, header))
                .assertNext(result -> {
                    assertEquals(ConnectivityStatusEnum.PASS, result.overall());
                    assertEquals(ConnectivityStatusEnum.PASS, result.l1().status());
                    assertEquals(ConnectivityStatusEnum.PASS, result.l2().status());
                    assertEquals("deepseek-chat", result.l2().model());
                    assertEquals(List.of("CONNECTIVITY", "AUTH", "MODEL_VISIBLE", "INFERENCE"), result.dimensions());
                })
                .verifyComplete();

        ArgumentCaptor<ConnectivityCheckProfile> profileCaptor =
                ArgumentCaptor.forClass(ConnectivityCheckProfile.class);
        verify(modelProviderStore).updateCheckProfile(eq(100L), profileCaptor.capture(), eq(header));
        assertEquals(ConnectivityStatusEnum.PASS, profileCaptor.getValue().status());
        assertEquals(240, profileCaptor.getValue().latencyMs());
        assertEquals("deepseek-chat", profileCaptor.getValue().model());
    }

    @Test
    void l1OnlyCheckSkipsL2AndNeverPersists() {
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        when(checker.checkL1(any(ModelProviderBO.class)))
                .thenReturn(Mono.just(ConnectivityLevelResult.passL1(120, List.of("deepseek-chat"))));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("100");
        request.setLevel(io.github.pnoker.common.enums.ConnectivityCheckLevelEnum.L1);

        StepVerifier.create(service.checkProvider(request, header))
                .assertNext(result -> {
                    assertEquals(ConnectivityStatusEnum.PASS, result.overall());
                    assertEquals(ConnectivityStatusEnum.SKIPPED, result.l2().status());
                })
                .verifyComplete();

        verify(modelProviderStore, never()).updateCheckProfile(any(), any(), any());
        verify(checker, never()).checkL2(any(), any());
    }

    @Test
    void draftCheckUsesRequestBodyAndNeverPersists() {
        when(checker.checkL1(any(ModelProviderBO.class)))
                .thenReturn(Mono.just(ConnectivityLevelResult.passL1(90, List.of("local-model"))));
        when(checker.checkL2(any(ModelProviderBO.class), eq("local-model")))
                .thenReturn(Mono.just(ConnectivityLevelResult.passL2(150, "local-model")));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setProviderType(AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE);
        request.setBaseUrl("http://localhost:11434/v1");
        request.setModel("local-model");

        StepVerifier.create(service.checkProvider(request, header))
                .assertNext(result -> assertEquals(ConnectivityStatusEnum.PASS, result.overall()))
                .verifyComplete();

        ArgumentCaptor<ModelProviderBO> targetCaptor = ArgumentCaptor.forClass(ModelProviderBO.class);
        verify(checker).checkL1(targetCaptor.capture());
        assertEquals("http://localhost:11434/v1", targetCaptor.getValue().getBaseUrl());
        assertEquals("", targetCaptor.getValue().getApiKey());
        verify(modelProviderStore, never()).updateCheckProfile(any(), any(), any());
    }

    @Test
    void savedProviderFallsBackToStoredKeyWhenApiKeyAbsent() {
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        stubPassingProbes();
        when(modelConfigStore.list(header, true)).thenReturn(Flux.just(configOf("deepseek-chat", 100L)));
        when(modelProviderStore.updateCheckProfile(any(), any(), any())).thenReturn(Mono.just(true));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("100");

        service.checkProvider(request, header).block();

        ArgumentCaptor<ModelProviderBO> targetCaptor = ArgumentCaptor.forClass(ModelProviderBO.class);
        verify(checker).checkL1(targetCaptor.capture());
        assertEquals("stored-key", targetCaptor.getValue().getApiKey());
    }

    @Test
    void explicitEmptyApiKeyIsADeliberateNoKeyProbe() {
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        stubPassingProbes();
        when(modelConfigStore.list(header, true)).thenReturn(Flux.just(configOf("deepseek-chat", 100L)));
        when(modelProviderStore.updateCheckProfile(any(), any(), any())).thenReturn(Mono.just(true));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("100");
        request.setApiKey("");

        service.checkProvider(request, header).block();

        ArgumentCaptor<ModelProviderBO> targetCaptor = ArgumentCaptor.forClass(ModelProviderBO.class);
        verify(checker).checkL1(targetCaptor.capture());
        assertEquals("", targetCaptor.getValue().getApiKey());
    }

    @Test
    void infrastructureFailureSkipsL2() {
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        when(checker.checkL1(any(ModelProviderBO.class)))
                .thenReturn(Mono.just(
                        ConnectivityLevelResult.fail(0, ConnectivityErrorTypeEnum.UNREACHABLE, "no dns", null)));
        // A BOTH check that concludes FAIL still persists the failing profile.
        when(modelProviderStore.updateCheckProfile(any(), any(), any())).thenReturn(Mono.just(true));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("100");

        StepVerifier.create(service.checkProvider(request, header))
                .assertNext(result -> {
                    assertEquals(ConnectivityStatusEnum.FAIL, result.overall());
                    assertEquals(ConnectivityStatusEnum.SKIPPED, result.l2().status());
                })
                .verifyComplete();
        verify(checker, never()).checkL2(any(), any());
        ArgumentCaptor<ConnectivityCheckProfile> profileCaptor =
                ArgumentCaptor.forClass(ConnectivityCheckProfile.class);
        verify(modelProviderStore).updateCheckProfile(eq(100L), profileCaptor.capture(), eq(header));
        assertEquals(
                ConnectivityErrorTypeEnum.UNREACHABLE.name(),
                profileCaptor.getValue().errorType());
    }

    @Test
    void missingProbeModelFailsL2WithModelNotFound() {
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        when(checker.checkL1(any(ModelProviderBO.class)))
                .thenReturn(Mono.just(ConnectivityLevelResult.passL1(80, List.of())));
        when(modelConfigStore.list(header, true)).thenReturn(Flux.empty());
        when(modelProviderStore.updateCheckProfile(any(), any(), any())).thenReturn(Mono.just(true));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("100");

        StepVerifier.create(service.checkProvider(request, header))
                .assertNext(result -> {
                    assertEquals(ConnectivityStatusEnum.FAIL, result.overall());
                    assertEquals(ConnectivityStatusEnum.FAIL, result.l2().status());
                    assertEquals(
                            ConnectivityErrorTypeEnum.MODEL_NOT_FOUND,
                            result.l2().errorType());
                })
                .verifyComplete();
    }

    @Test
    void cachedResultIsReusedWithinTtl() {
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        stubPassingProbes();
        when(modelConfigStore.list(header, true)).thenReturn(Flux.just(configOf("deepseek-chat", 100L)));
        when(modelProviderStore.updateCheckProfile(any(), any(), any())).thenReturn(Mono.just(true));

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("100");

        service.checkProvider(request, header).block();
        service.checkProvider(request, header).block();

        verify(checker, times(1)).checkL1(any(ModelProviderBO.class));
        verify(modelProviderStore, times(1)).updateCheckProfile(any(), any(), any());
    }

    @Test
    void cacheKeyIsLevelIsolated() {
        // A BOTH check followed by an L1-only call must re-probe (the dropdown
        // path), never serve the cached BOTH result with its non-skipped L2.
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        stubPassingProbes();
        when(modelConfigStore.list(header, true)).thenReturn(Flux.just(configOf("deepseek-chat", 100L)));
        when(modelProviderStore.updateCheckProfile(any(), any(), any())).thenReturn(Mono.just(true));

        ProviderCheckRequestVO both = new ProviderCheckRequestVO();
        both.setId("100");
        service.checkProvider(both, header).block();

        ProviderCheckRequestVO l1Only = new ProviderCheckRequestVO();
        l1Only.setId("100");
        l1Only.setLevel(ConnectivityCheckLevelEnum.L1);
        ConnectivityCheckResult l1Result = service.checkProvider(l1Only, header).block();

        assertEquals(ConnectivityStatusEnum.SKIPPED, l1Result.l2().status());
        verify(checker, times(2)).checkL1(any(ModelProviderBO.class));
    }

    @Test
    void unknownProviderIdFailsWithNotFound() {
        when(modelProviderStore.get(999L, header)).thenReturn(Mono.empty());

        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("999");

        StepVerifier.create(service.checkProvider(request, header))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void nonNumericProviderIdIsRejected() {
        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setId("not-a-number");

        StepVerifier.create(service.checkProvider(request, header))
                .expectError(RequestException.class)
                .verify();
    }

    @Test
    void draftWithoutProviderTypeIsRejected() {
        ProviderCheckRequestVO request = new ProviderCheckRequestVO();
        request.setBaseUrl("https://api.example.com");

        StepVerifier.create(service.checkProvider(request, header))
                .expectError(RequestException.class)
                .verify();
    }

    @Test
    void checkModelConfigProbesStoredModelAndPersistsConfigProfile() {
        ModelConfigBO config = configOf("deepseek-chat", 100L);
        config.setId(200L);
        when(modelConfigStore.get(200L, header)).thenReturn(Mono.just(config));
        when(modelProviderStore.get(100L, header)).thenReturn(Mono.just(savedProvider()));
        when(checker.checkL2(any(ModelProviderBO.class), eq("deepseek-chat")))
                .thenReturn(Mono.just(ConnectivityLevelResult.passL2(300, "deepseek-chat")));
        when(modelConfigStore.updateCheckProfile(eq(200L), any(ConnectivityCheckProfile.class), eq(header)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.checkModelConfig(200L, header))
                .assertNext(result -> {
                    assertEquals(ConnectivityStatusEnum.PASS, result.overall());
                    assertEquals(ConnectivityStatusEnum.SKIPPED, result.l1().status());
                    assertEquals(ConnectivityStatusEnum.PASS, result.l2().status());
                })
                .verifyComplete();

        verify(modelConfigStore).updateCheckProfile(eq(200L), any(ConnectivityCheckProfile.class), eq(header));
        verify(modelProviderStore, never()).updateCheckProfile(any(), any(), any());
    }

    @Test
    void checkModelConfigRejectsMissingConfig() {
        when(modelConfigStore.get(404L, header)).thenReturn(Mono.empty());

        StepVerifier.create(service.checkModelConfig(404L, header))
                .expectError(NotFoundException.class)
                .verify();
    }

    private ModelConfigBO configOf(String model, Long providerId) {
        ModelConfigBO config = new ModelConfigBO();
        config.setModel(model);
        config.setLabel(model);
        config.setProviderId(providerId);
        config.setTenantId(1L);
        return config;
    }
}
