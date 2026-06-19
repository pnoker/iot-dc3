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

package io.github.pnoker.common.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.entity.query.PrincipalQuery;
import io.github.pnoker.common.enums.EnableFlagEnum;

import java.util.Collection;
import java.util.List;

/**
 * Read-only business service for principals. Principals themselves are created transitively
 * (USER via user management, SERVICE_ACCOUNT via service-account management, SYSTEM at boot), so
 * this service exposes lookup/list and enable/disable only — no create.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
public interface PrincipalService {

    PrincipalBO getById(Long id);

    Page<PrincipalBO> list(PrincipalQuery entityQuery);

    /**
     * Batch-resolve principals by their IDs, used to turn principalId references in other
     * lists into display names. Returns only the matched rows; unknown IDs are skipped.
     */
    List<PrincipalBO> listByIds(Collection<Long> ids);

    void setEnableFlag(Long id, EnableFlagEnum target, Long operatorId, String operatorName);
}
