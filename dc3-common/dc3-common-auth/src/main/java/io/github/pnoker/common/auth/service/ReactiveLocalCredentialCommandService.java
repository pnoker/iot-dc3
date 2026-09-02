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

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import reactor.core.publisher.Mono;

/** Atomic non-blocking local credential lifecycle and login state commands. */
public interface ReactiveLocalCredentialCommandService {
    Mono<LocalCredentialBO> add(Long tenantId, LocalCredentialBO credential, Long operatorId, String operatorName);

    Mono<LocalCredentialBO> update(Long tenantId, LocalCredentialBO credential, Long operatorId, String operatorName);

    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);

    Mono<LocalCredentialBO> resetPassword(
            Long tenantId, Long id, String rawPassword, Long operatorId, String operatorName);

    Mono<LocalCredentialBO> changePassword(
            Long tenantId,
            String loginName,
            String currentPassword,
            String newPassword,
            Long operatorId,
            String operatorName);

    Mono<Void> recordSuccessfulLogin(Long tenantId, Long id);

    Mono<Void> recordFailedLogin(Long tenantId, Long id);
}
