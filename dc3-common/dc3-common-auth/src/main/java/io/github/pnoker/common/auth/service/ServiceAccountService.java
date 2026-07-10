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

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.query.ServiceAccountQuery;
import io.github.pnoker.common.base.service.BaseService;

/**
 * Business service for service accounts.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface ServiceAccountService extends BaseService<ServiceAccountBO, ServiceAccountQuery> {

    /**
     * Get a service account by principal id.
     *
     * @param principalId    principal id to look up
     * @param throwException whether to throw {@code NotFoundException} when not found
     * @return the service account, or {@code null} when not found and throwException is false
     */
    ServiceAccountBO getByPrincipalId(Long principalId, boolean throwException);

}
