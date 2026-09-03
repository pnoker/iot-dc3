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

import io.github.pnoker.common.auth.entity.bean.TokenValid;
import reactor.core.publisher.Mono;

/** Fully reactive token lifecycle contract. */
public interface ReactiveTokenService {
    /** Emit the salt seed used to hash the login password. */
    Mono<String> generateSalt(String loginName, String tenantCode);

    /** Emit a login token for valid credentials. */
    Mono<String> generateToken(String loginName, String password, String tenantCode);

    /** Rotate the local credential password after verifying the current one. */
    Mono<Void> changePassword(String loginName, String currentPassword, String newPassword, String tenantCode);

    /** Best-effort cancel of the login token, reporting whether it was active. */
    Mono<Boolean> tryCancelToken(String loginName, String tenantCode);

    /** Validate the login token against the stored material. */
    Mono<TokenValid> checkValid(String loginName, String token, String tenantCode);
}
