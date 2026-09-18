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
package io.github.pnoker.common.manager.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.common.manager.entity.bo.LabelBindBO;
import io.github.pnoker.common.manager.repository.ReactiveLabelBindStore;
import io.github.pnoker.common.manager.repository.ReactiveLabelStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveLabelBindServiceImplTest {

    @Mock
    private ReactiveLabelBindStore labelBindStore;

    @Mock
    private ReactiveLabelStore labelStore;

    @Test
    void addRejectsOwnerTypeMismatch() {
        LabelBindBO binding = binding();
        LabelBO owner = new LabelBO();
        owner.setEntityTypeFlag(EntityTypeEnum.DRIVER);
        when(labelStore.get(1L, 20L)).thenReturn(Mono.just(owner));

        StepVerifier.create(service().add(binding))
                .expectError(NotFoundException.class)
                .verify();

        verify(labelBindStore, never()).insert(binding);
    }

    @Test
    void addMapsDatabaseDuplicateRaceToDomainError() {
        LabelBindBO binding = binding();
        LabelBO owner = new LabelBO();
        owner.setEntityTypeFlag(EntityTypeEnum.DEVICE);
        when(labelStore.get(1L, 20L)).thenReturn(Mono.just(owner));
        when(labelBindStore.getByEntity(1L, EntityTypeEnum.DEVICE.getIndex(), 20L, 30L))
                .thenReturn(Mono.empty());
        when(labelBindStore.insert(binding)).thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(service().add(binding))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void getByIdUsesTenantScope() {
        when(labelBindStore.get(2L, 8L)).thenReturn(Mono.empty());

        StepVerifier.create(service().getById(2L, 8L))
                .expectError(NotFoundException.class)
                .verify();

        verify(labelBindStore).get(2L, 8L);
    }

    private ReactiveLabelBindServiceImpl service() {
        return new ReactiveLabelBindServiceImpl(labelBindStore, labelStore);
    }

    private LabelBindBO binding() {
        LabelBindBO binding = new LabelBindBO();
        binding.setTenantId(1L);
        binding.setEntityTypeFlag(EntityTypeEnum.DEVICE);
        binding.setLabelId(20L);
        binding.setEntityId(30L);
        return binding;
    }
}
