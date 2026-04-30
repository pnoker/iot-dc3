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

package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;

/**
 * Reconciles the full set of HTTP endpoints discovered by a calling center service
 * against the dc3_api and dc3_resource tables: missing rows are inserted, drifted rows
 * are updated, and (optionally) orphaned rows are soft-deleted.
 *
 * @author pnoker
 * @version 2026.4.30
 * @since 2026.4.30
 */
public interface ResourceRegistrySyncService {

    /**
     * Perform a three-way diff against the DB state and apply the required mutations
     * inside a single transaction guarded by a Postgres advisory lock keyed on
     * {@link ResourceRegistrySyncCommand#getServiceName()}.
     *
     * @param command the full API inventory for a single service
     * @return counters describing what was changed
     */
    ResourceRegistrySyncResult sync(ResourceRegistrySyncCommand command);
}
