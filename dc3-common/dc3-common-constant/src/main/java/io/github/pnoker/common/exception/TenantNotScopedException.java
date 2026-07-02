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

package io.github.pnoker.common.exception;

/**
 * Thrown when a tenant-scoped query runs without a tenant id bound to the thread and
 * the ignore flag is not set — i.e. a code path forgot to establish tenant context
 * (via BaseController.async) or forgot to wrap cross-tenant work in runIgnore.
 * Maps to HTTP 500: this is a programming error, not a client error.
 */
public class TenantNotScopedException extends RuntimeException {

    public TenantNotScopedException(String message) {
        super(message);
    }
}
