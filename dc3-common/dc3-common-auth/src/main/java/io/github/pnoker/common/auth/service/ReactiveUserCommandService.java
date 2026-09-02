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

import io.github.pnoker.common.auth.entity.bo.UserBO;
import reactor.core.publisher.Mono;

/** Atomic user lifecycle commands spanning principal, user and membership rows. */
public interface ReactiveUserCommandService {
    Mono<UserBO> add(Long tenantId, UserBO user, Long operatorId, String operatorName);

    Mono<UserBO> update(Long tenantId, UserBO user, Long operatorId, String operatorName);

    Mono<Boolean> delete(Long tenantId, Long userId, Long operatorId, String operatorName);
}
