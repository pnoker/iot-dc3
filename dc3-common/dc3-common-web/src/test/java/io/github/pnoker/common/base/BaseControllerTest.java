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

package io.github.pnoker.common.base;

import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.TenantOwned;
import io.github.pnoker.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseControllerTest {

    private record TenantEntity(Long tenantId) implements TenantOwned {
        @Override
        public Long getTenantId() {
            return tenantId;
        }
    }

    private final BaseController controller = new BaseController() {
    };

    @Test
    void requireTenantPassesThroughOwnedEntity() {
        TenantEntity entity = new TenantEntity(1L);
        assertThat(controller.requireTenant(1L, entity)).isSameAs(entity);
    }

    @Test
    void requireTenantThrowsForNullEntity() {
        assertThatThrownBy(() -> controller.requireTenant(1L, (TenantEntity) null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void requireTenantThrowsForCrossTenantEntity() {
        TenantEntity entity = new TenantEntity(2L);
        assertThatThrownBy(() -> controller.requireTenant(1L, entity))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void filterTenantReturnsEmptyForNullOrEmpty() {
        assertThat(controller.filterTenant(1L, null)).isEmpty();
        assertThat(controller.filterTenant(1L, List.of())).isEmpty();
    }

    @Test
    void filterTenantKeepsOnlyMatchingEntities() {
        List<TenantEntity> entities = List.of(
                new TenantEntity(1L),
                new TenantEntity(2L),
                new TenantEntity(1L));
        assertThat(controller.filterTenant(1L, entities))
                .extracting(TenantEntity::tenantId)
                .containsExactly(1L, 1L);
    }

    @Test
    void filterTenantSkipsNullEntries() {
        java.util.ArrayList<TenantEntity> entities = new java.util.ArrayList<>();
        entities.add(new TenantEntity(1L));
        entities.add(null);
        entities.add(new TenantEntity(1L));
        assertThat(controller.filterTenant(1L, entities)).hasSize(2);
    }

    @Test
    void asyncWrapsSupplierIntoMono() {
        StepVerifier.create(controller.async(() -> R.ok("hello")))
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getMessage()).isEqualTo("hello");
                })
                .verifyComplete();
    }

    @Test
    void asyncPropagatesSupplierExceptionAsMonoError() {
        StepVerifier.create(controller.async(() -> {
                    throw new IllegalStateException("supplier failed");
                }))
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException
                        && "supplier failed".equals(throwable.getMessage()))
                .verify();
    }
}
