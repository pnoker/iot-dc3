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

import java.util.function.Supplier;

/**
 * Thread-bound holder for the current tenant id and an "ignore" flag, consumed by
 * the MyBatis-Plus tenant-line handler to decide whether — and with which value —
 * to inject a {@code tenant_id} predicate.
 * <p>
 * The handler is fail-closed: when {@link #getTenantId()} is {@code null} and
 * {@link #isIgnored()} is {@code false}, queries must be rejected rather than run
 * unscoped. Legitimate tenant-free paths (login before a tenant is resolved,
 * cross-tenant internal orchestration) must wrap their work in {@link #runIgnore}.
 * <p>
 * Because the values live in {@link ThreadLocal}s on a pooled (bounded-elastic)
 * worker thread, every set must be paired with a {@link #clear()}, and
 * {@link #runIgnore} always restores the prior flag — including on exception and
 * across nesting — so a leaked "ignore" can never silently disable filtering on a
 * reused thread.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContextHolder() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @return the tenant id bound to the current thread, or {@code null} if none.
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * Bind the tenant id to the current thread.
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * @return {@code true} while the current thread is inside a {@link #runIgnore} scope.
     */
    public static boolean isIgnored() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /**
     * Run {@code supplier} with tenant filtering disabled, restoring the previous
     * ignore flag afterwards. Safe to nest and exception-safe.
     *
     * @return the supplier's result
     */
    public static <T> T runIgnore(Supplier<T> supplier) {
        boolean previous = isIgnored();
        IGNORE.set(Boolean.TRUE);
        try {
            return supplier.get();
        } finally {
            IGNORE.set(previous);
        }
    }

    /**
     * Void variant of {@link #runIgnore(Supplier)} for actions with no result.
     * Same nesting/exception safety.
     */
    public static void runIgnoreAction(Runnable action) {
        boolean previous = isIgnored();
        IGNORE.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            IGNORE.set(previous);
        }
    }

    /**
     * Detach all tenant state from the current thread. Must be called when a pooled
     * worker finishes the unit of work it was bound for.
     */
    public static void clear() {
        TENANT_ID.remove();
        IGNORE.remove();
    }
}
