package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.common.manager.repository.ReactiveLabelStore;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveLabelServiceImplTest {

    @Mock
    private ReactiveLabelStore labelStore;

    @Test
    void getByIdDoesNotExposeAnotherTenant() {
        when(labelStore.get(2L, 7L)).thenReturn(Mono.empty());

        StepVerifier.create(service().getById(2L, 7L))
                .expectError(NotFoundException.class)
                .verify();

        verify(labelStore).get(2L, 7L);
    }

    @Test
    void addMapsDatabaseDuplicateRaceToDomainError() {
        LabelBO label = label(1L, "critical");
        when(labelStore.getByName(1L, "critical", EntityTypeEnum.DEVICE.getIndex())).thenReturn(Mono.empty());
        when(labelStore.insert(label)).thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(service().add(label))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void deleteRejectsLabelWithActiveBindings() {
        LabelBO label = label(1L, "critical");
        label.setId(7L);
        when(labelStore.get(1L, 7L)).thenReturn(Mono.just(label));
        when(labelStore.hasActiveBindings(1L, 7L)).thenReturn(Mono.just(true));

        StepVerifier.create(service().delete(1L, 7L, 9L, "operator"))
                .expectError(AssociatedException.class)
                .verify();

        verify(labelStore, never()).delete(1L, 7L, 9L, "operator");
    }

    private ReactiveLabelServiceImpl service() {
        return new ReactiveLabelServiceImpl(labelStore);
    }

    private LabelBO label(Long tenantId, String name) {
        LabelBO label = new LabelBO();
        label.setTenantId(tenantId);
        label.setLabelName(name);
        label.setEntityTypeFlag(EntityTypeEnum.DEVICE);
        return label;
    }

}
