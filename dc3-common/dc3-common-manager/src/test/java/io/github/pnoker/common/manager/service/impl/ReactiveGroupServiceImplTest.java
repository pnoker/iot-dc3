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
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.common.manager.repository.ReactiveGroupStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveGroupServiceImplTest {

    @Mock
    private ReactiveGroupStore groupStore;

    @Test
    void updateRejectsDescendantAsParent() {
        GroupBO current = group(10L, 0L, "current");
        GroupBO parent = group(30L, 20L, "parent");
        GroupBO ancestor = group(20L, 10L, "ancestor");
        GroupBO update = group(10L, 30L, "current");

        when(groupStore.get(1L, 10L)).thenReturn(Mono.just(current));
        when(groupStore.get(1L, 30L)).thenReturn(Mono.just(parent));
        when(groupStore.get(1L, 20L)).thenReturn(Mono.just(ancestor));

        StepVerifier.create(service().update(update))
                .expectError(RequestException.class)
                .verify();

        verify(groupStore, never()).update(update);
    }

    @Test
    void addMapsDatabaseDuplicateRaceToDomainError() {
        GroupBO group = group(null, 0L, "production");
        when(groupStore.getByName(1L, EntityTypeEnum.DEVICE.getIndex(), 0L, "production"))
                .thenReturn(Mono.empty());
        when(groupStore.insert(group)).thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(service().add(group))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void deleteRejectsGroupWithChildren() {
        GroupBO group = group(7L, 0L, "parent");
        when(groupStore.get(1L, 7L)).thenReturn(Mono.just(group));
        when(groupStore.hasChildren(1L, 7L)).thenReturn(Mono.just(true));
        when(groupStore.hasActiveBindings(1L, 7L)).thenReturn(Mono.just(false));

        StepVerifier.create(service().delete(1L, 7L, 9L, "operator"))
                .expectError(AssociatedException.class)
                .verify();

        verify(groupStore, never()).delete(1L, 7L, 9L, "operator");
    }

    @Test
    void deleteRejectsGroupWithActiveBindings() {
        GroupBO group = group(7L, 0L, "bound");
        when(groupStore.get(1L, 7L)).thenReturn(Mono.just(group));
        when(groupStore.hasChildren(1L, 7L)).thenReturn(Mono.just(false));
        when(groupStore.hasActiveBindings(1L, 7L)).thenReturn(Mono.just(true));

        StepVerifier.create(service().delete(1L, 7L, 9L, "operator"))
                .expectError(AssociatedException.class)
                .verify();

        verify(groupStore, never()).delete(1L, 7L, 9L, "operator");
    }

    private ReactiveGroupServiceImpl service() {
        return new ReactiveGroupServiceImpl(groupStore);
    }

    private GroupBO group(Long id, Long parentId, String name) {
        GroupBO group = new GroupBO();
        group.setId(id);
        group.setParentGroupId(parentId);
        group.setGroupName(name);
        group.setGroupTypeFlag(EntityTypeEnum.DEVICE);
        group.setGroupLevel((byte) 0);
        group.setTenantId(1L);
        return group;
    }
}
