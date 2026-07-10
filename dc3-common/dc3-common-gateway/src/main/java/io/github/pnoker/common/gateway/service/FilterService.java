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

package io.github.pnoker.common.gateway.service;

import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Service interface for gateway filter logic.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface FilterService {

    /**
     * Resolve the tenant from the request's tenant header, requiring it to be enabled.
     *
     * @param request the incoming request
     * @return the resolved tenant
     */
    FacadeTenantBO getTenant(ServerHttpRequest request);

    /**
     * Resolve the local credential from the request's login header.
     *
     * @param request the incoming request
     * @return the resolved credential
     */
    FacadeLocalCredentialBO getLocalCredential(ServerHttpRequest request);

    /**
     * Assemble the principal header forwarded to downstream services from the resolved
     * credential and tenant.
     *
     * @param credential the local credential
     * @param tenant     the tenant
     * @return the principal header for downstream
     */
    RequestHeader.PrincipalHeader getUser(FacadeLocalCredentialBO credential, FacadeTenantBO tenant);

    /**
     * Validate the request's token (salt + token) against the resolved tenant and
     * credential.
     *
     * @param request    the incoming request
     * @param tenant     the resolved tenant
     * @param credential the resolved credential
     */
    void checkValid(ServerHttpRequest request, FacadeTenantBO tenant, FacadeLocalCredentialBO credential);

}
