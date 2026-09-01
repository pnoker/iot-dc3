package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.auth.repository.ReactiveTenantDictionaryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryForAuthServiceImplTest {
    @Mock private ReactiveTenantDictionaryStore tenantStore;
    @InjectMocks private DictionaryForAuthServiceImpl service;

    @Test
    void listTenantOptionsMapsEnabledTenantsReactively() {
        TenantDO first = new TenantDO(); first.setId(1L); first.setTenantName("Acme");
        TenantDO second = new TenantDO(); second.setId(2L); second.setTenantName("Globex");
        when(tenantStore.listEnabled()).thenReturn(Flux.just(first, second));

        StepVerifier.create(service.listTenantOptions())
                .assertNext(options -> {
                    org.assertj.core.api.Assertions.assertThat(options)
                            .extracting(option -> option.label() + ":" + option.value())
                            .containsExactly("Acme:1", "Globex:2");
                })
                .verifyComplete();
    }
}
