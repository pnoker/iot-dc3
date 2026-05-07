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
import io.github.pnoker.common.auth.entity.model.MenuDO;

/**
 * Reconciles the full set of HTTP endpoints discovered by a calling center service
 * against the dc3_api and dc3_resource tables: missing rows are inserted, drifted rows
 * are updated, and (optionally) orphaned rows are soft-deleted.
 *
 * @author pnoker
 * @version 2026.5.5
 * @since 2026.5.5
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

    /**
     * Mirror a single menu row into dc3_resource as a MENU-type leaf. Called by
     * MenuServiceImpl on save/update so the Resource tree always tracks menu state.
     * Idempotent — updates the existing resource row if one already exists for the menu.
     *
     * @param menu the menu row that was just inserted or updated
     */
    void syncMenuResource(MenuDO menu);

    /**
     * Soft-delete the resource row mirroring the given menu, if any. No-op when no mirror
     * exists.
     *
     * @param menuId the id of the menu being removed
     */
    void removeMenuResource(Long menuId);

}
