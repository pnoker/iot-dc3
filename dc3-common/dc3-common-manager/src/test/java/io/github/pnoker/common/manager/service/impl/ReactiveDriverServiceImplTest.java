package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactiveDriverStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveDriverServiceImplTest {

    @Mock private ReactiveDriverStore driverStore;
    @Mock private MetadataEventPublisher metadataEventPublisher;
    private ReactiveDriverServiceImpl service;

    @BeforeEach void setUp() { service = new ReactiveDriverServiceImpl(driverStore, metadataEventPublisher); }

    @Test
    void addRejectsDuplicateWithinTenant() {
        DriverBO source = driver(7L, null, "driver", "code");
        when(driverStore.getByNameAndCode(7L, "driver", "code")).thenReturn(Mono.just(driver(7L, 1L, "driver", "code")));
        StepVerifier.create(service.add(source)).expectErrorMessage("Failed to create driver: driver has been duplicated").verify();
        verify(driverStore, never()).insert(any());
    }

    @Test
    void updateRequiresStableCodeAndVersion() {
        DriverBO source = driver(7L, 1L, "driver", ""); source.setVersion(null);
        StepVerifier.create(service.update(source)).expectErrorMessage("Driver ID and version are required for update").verify();
        verify(driverStore, never()).update(any(), any(Integer.class));
    }

    @Test
    void updateUsesTenantAndOptimisticVersion() {
        DriverBO source = driver(7L, 1L, "driver", "code"); source.setVersion(3);
        DriverBO current = driver(7L, 1L, "old", "code");
        DriverBO saved = driver(7L, 1L, "driver", "code");
        when(driverStore.get(7L, 1L)).thenReturn(Mono.just(current));
        when(driverStore.getByNameAndCode(7L, "driver", "code")).thenReturn(Mono.empty());
        when(driverStore.update(source, 3)).thenReturn(Mono.just(saved));
        StepVerifier.create(service.update(source)).expectNext(saved).verifyComplete();
        verify(driverStore).update(source, 3);
    }

    @Test
    void deletePublishesOnlyAfterSuccessfulTenantScopedDelete() {
        when(driverStore.get(7L, 1L)).thenReturn(Mono.just(driver(7L, 1L, "driver", "code")));
        when(driverStore.delete(eq(7L), eq(1L), eq(3), eq(2L), eq("operator"))).thenReturn(Mono.just(true));
        StepVerifier.create(service.delete(7L, 1L, 3, 2L, "operator")).expectNext(true).verifyComplete();
        verify(metadataEventPublisher).publishEvent(any());
    }

    private DriverBO driver(Long tenantId, Long id, String name, String code) {
        DriverBO value = new DriverBO(); value.setTenantId(tenantId); value.setId(id); value.setDriverName(name);
        value.setDriverCode(code); value.setServiceName("service"); value.setServiceHost("127.0.0.1"); value.setVersion(0); return value;
    }
}
