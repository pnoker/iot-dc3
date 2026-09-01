package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.common.manager.entity.bo.GroupBindBO;
import io.github.pnoker.common.manager.repository.ReactiveGroupBindStore;
import io.github.pnoker.common.manager.repository.ReactiveGroupStore;
import io.github.pnoker.common.enums.EntityTypeEnum;
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
class ReactiveGroupBindServiceImplTest {

    @Mock
    private ReactiveGroupBindStore groupBindStore;

    @Mock
    private ReactiveGroupStore groupStore;

    @Test
    void addRejectsOwnerTypeMismatch() {
        GroupBindBO binding = binding();
        GroupBO owner = new GroupBO();
        owner.setGroupTypeFlag(EntityTypeEnum.DRIVER);
        when(groupStore.get(1L, 20L)).thenReturn(Mono.just(owner));

        StepVerifier.create(service().add(binding))
                .expectError(NotFoundException.class)
                .verify();

        verify(groupBindStore, never()).insert(binding);
    }

    @Test
    void addMapsDatabaseDuplicateRaceToDomainError() {
        GroupBindBO binding = binding();
        GroupBO owner = new GroupBO();
        owner.setGroupTypeFlag(EntityTypeEnum.DEVICE);
        when(groupStore.get(1L, 20L)).thenReturn(Mono.just(owner));
        when(groupBindStore.getByEntity(1L, EntityTypeEnum.DEVICE.getIndex(), 30L)).thenReturn(Mono.empty());
        when(groupBindStore.insert(binding)).thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(service().add(binding))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void getByIdUsesTenantScope() {
        when(groupBindStore.get(2L, 8L)).thenReturn(Mono.empty());

        StepVerifier.create(service().getById(2L, 8L))
                .expectError(NotFoundException.class)
                .verify();

        verify(groupBindStore).get(2L, 8L);
    }

    private ReactiveGroupBindServiceImpl service() {
        return new ReactiveGroupBindServiceImpl(groupBindStore, groupStore);
    }

    private GroupBindBO binding() {
        GroupBindBO binding = new GroupBindBO();
        binding.setTenantId(1L);
        binding.setEntityTypeFlag(EntityTypeEnum.DEVICE);
        binding.setGroupId(20L);
        binding.setEntityId(30L);
        return binding;
    }

}
