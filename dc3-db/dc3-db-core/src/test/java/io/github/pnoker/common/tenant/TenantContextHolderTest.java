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

package io.github.pnoker.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract for the thread-bound tenant context that backs the MyBatis-Plus
 * tenant-line handler. The {@code runIgnore} family carries the security-critical
 * semantics: it must enable bypass only for the duration of the supplied work and
 * always restore the prior flag, even on exception and across nesting, so a leaked
 * "ignore" can never silently disable tenant filtering on a reused thread.
 */
class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getReturnsValueAfterSet() {
        TenantContextHolder.setTenantId(7L);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
    }

    @Test
    void getReturnsNullWhenUnset() {
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void clearRemovesTenant() {
        TenantContextHolder.setTenantId(7L);
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void notIgnoredByDefault() {
        assertThat(TenantContextHolder.isIgnored()).isFalse();
    }

    @Test
    void runIgnoreEnablesIgnoreDuringExecution() {
        boolean[] seenInside = {false};
        TenantContextHolder.runIgnore(() -> {
            seenInside[0] = TenantContextHolder.isIgnored();
            return null;
        });
        assertThat(seenInside[0]).isTrue();
        assertThat(TenantContextHolder.isIgnored()).isFalse();
    }

    @Test
    void runIgnoreRestoresFlagOnException() {
        assertThatThrownBy(() -> TenantContextHolder.runIgnore(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContextHolder.isIgnored()).isFalse();
    }

    @Test
    void runIgnoreSupportsNesting() {
        TenantContextHolder.runIgnore(() -> {
            TenantContextHolder.runIgnore(() -> null);
            // After the inner scope exits, the outer scope must still be ignoring.
            assertThat(TenantContextHolder.isIgnored()).isTrue();
            return null;
        });
        assertThat(TenantContextHolder.isIgnored()).isFalse();
    }

    @Test
    void runIgnoreReturnsSupplierResult() {
        String result = TenantContextHolder.runIgnore(() -> "ok");
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void runIgnoreActionDisablesFilteringAndRestores() {
        assertThat(TenantContextHolder.isIgnored()).isFalse();
        TenantContextHolder.runIgnoreAction(() -> {
            assertThat(TenantContextHolder.isIgnored()).isTrue();
        });
        assertThat(TenantContextHolder.isIgnored()).isFalse();
    }

    @Test
    void runIgnoreActionRestoresOnException() {
        assertThat(TenantContextHolder.isIgnored()).isFalse();
        assertThatThrownBy(() ->
                TenantContextHolder.runIgnoreAction(() -> {
                    throw new IllegalStateException("boom");
                })
        ).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContextHolder.isIgnored()).isFalse();
    }
}
