package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.entity.bo.ResourceRegistryScannedApi;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.repository.ReactiveResourceRegistryStore;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class ReactiveResourceRegistrySyncServiceImplTest {

    @Test
    void rejectsInvalidCommandBeforeTouchingStore() {
        ReactiveResourceRegistryStore store = mock(ReactiveResourceRegistryStore.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        PermissionCacheInvalidator invalidator = mock(PermissionCacheInvalidator.class);
        ReactiveResourceRegistrySyncServiceImpl service = new ReactiveResourceRegistrySyncServiceImpl(store, transaction, invalidator);

        StepVerifier.create(service.sync(null)).expectErrorMessage("serviceName is required").verify();
    }

    @Test
    void reconcilesSingleApiWithoutBlockingBridge() {
        ReactiveResourceRegistryStore store = mock(ReactiveResourceRegistryStore.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        PermissionCacheInvalidator invalidator = mock(PermissionCacheInvalidator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.acquireLock("global:svc")).thenReturn(Mono.just(1L));
        when(store.listApis("svc")).thenReturn(Flux.empty());
        when(store.getResourceByCode(any())).thenReturn(Mono.empty());
        when(store.insertApi(any())).thenReturn(Mono.just(api(42L)));
        when(store.insertResource(any())).thenAnswer(invocation -> {
            ResourceDO resource = invocation.getArgument(0);
            resource.setId(resource.getResourceCode().contains("api:service") ? 100L : 101L);
            return Mono.just(resource);
        });
        when(store.listResourcesByEntityIds(any())).thenReturn(Flux.empty());
        when(store.listApiResources("svc")).thenReturn(Flux.empty());

        ReactiveResourceRegistrySyncServiceImpl service = new ReactiveResourceRegistrySyncServiceImpl(store, transaction, invalidator);
        ResourceRegistrySyncCommand command = ResourceRegistrySyncCommand.builder()
                .serviceName("svc")
                .apis(List.of(ResourceRegistryScannedApi.builder().method("GET").path("/probe")
                        .apiName("probe:get").apiGroup("Probe").build()))
                .build();

        StepVerifier.create(service.sync(command))
                .assertNext(result -> {
                    assertThat(result.getInserted()).isEqualTo(1);
                    assertThat(result.getUpdated()).isZero();
                    assertThat(result.getDeleted()).isZero();
                })
                .verifyComplete();
        verify(invalidator).invalidateAll();
    }

    @Test
    void sharesOnePermissionResourceAcrossEndpoints() {
        ReactiveResourceRegistryStore store = mock(ReactiveResourceRegistryStore.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        PermissionCacheInvalidator invalidator = mock(PermissionCacheInvalidator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.acquireLock("global:svc")).thenReturn(Mono.just(1L));
        when(store.listApis("svc")).thenReturn(Flux.empty());
        when(store.getResourceByCode(any())).thenReturn(Mono.empty());
        when(store.insertApi(any())).thenAnswer(invocation -> {
            ApiDO api = invocation.getArgument(0);
            api.setId(api.getApiCode().endsWith("/probe") ? 42L : 43L);
            return Mono.just(api);
        });
        when(store.insertResource(any())).thenAnswer(invocation -> {
            ResourceDO resource = invocation.getArgument(0);
            resource.setId(resource.getResourceCode().contains("api:service") ? 100L : 101L);
            return Mono.just(resource);
        });
        when(store.listApiResources("svc")).thenReturn(Flux.empty());

        ReactiveResourceRegistrySyncServiceImpl service = new ReactiveResourceRegistrySyncServiceImpl(store, transaction, invalidator);
        ResourceRegistrySyncCommand command = ResourceRegistrySyncCommand.builder()
                .serviceName("svc")
                .apis(List.of(
                        ResourceRegistryScannedApi.builder().method("GET").path("/probe")
                                .apiName("probe:get").apiGroup("Probe").build(),
                        ResourceRegistryScannedApi.builder().method("GET").path("/probe/by_name")
                                .apiName("probe:get").apiGroup("Probe").build()))
                .build();

        StepVerifier.create(service.sync(command))
                .assertNext(result -> assertThat(result.getInserted()).isEqualTo(2))
                .verifyComplete();
        verify(store, times(3)).insertResource(any());
    }

    private ApiDO api(Long id) {
        ApiDO api = new ApiDO();
        api.setId(id);
        api.setServiceName("svc");
        api.setApiName("probe:get");
        api.setApiCode("svc:GET:/probe");
        api.setApiTypeFlag((byte) 3);
        api.setApiGroup("Probe");
        api.setEnableFlag((byte) 0);
        api.setRemark("");
        return api;
    }
}
