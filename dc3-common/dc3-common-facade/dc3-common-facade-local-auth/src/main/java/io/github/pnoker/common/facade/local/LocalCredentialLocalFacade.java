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
package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.facade.api.LocalCredentialFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.local.builder.FacadeLocalCredentialBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** In-process facade for local credential operations. */
@Component
@RequiredArgsConstructor
public class LocalCredentialLocalFacade implements LocalCredentialFacade {
    private final ReactiveLocalCredentialService credentialService;
    private final FacadeLocalCredentialBuilder builder;

    @Override
    public Mono<FacadeLocalCredentialBO> getByLoginName(Long tenantId, String loginName) {
        return credentialService.getByLoginName(tenantId, loginName).map(builder::toFacadeBO);
    }
}
