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
import reactor.core.publisher.Mono;

/**
 * Service interface for gateway filter logic.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface FilterService {





    Mono<FacadeTenantBO> getTenantReactive(ServerHttpRequest request);

    Mono<FacadeLocalCredentialBO> getLocalCredentialReactive(ServerHttpRequest request, Long tenantId);

    Mono<RequestHeader.PrincipalHeader> getUserReactive(FacadeLocalCredentialBO credential, FacadeTenantBO tenant);

    Mono<Void> checkValidReactive(ServerHttpRequest request, FacadeTenantBO tenant,
                                  FacadeLocalCredentialBO credential);

}
