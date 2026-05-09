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

package io.github.pnoker.common.config;

import io.github.pnoker.common.utils.HmacAuthSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the shared {@link HmacAuthSigner} bean. Picked up by every
 * application that depends on {@code dc3-common-public} via its
 * {@code AutoConfiguration.imports}, so the gateway and every backend service share the
 * same secret-loading rules.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@AutoConfiguration
public class HmacAuthConfig {

    @Bean
    public HmacAuthSigner hmacAuthSigner(@Value("${dc3.auth.hmac.secret:${AUTH_HMAC_SECRET:}}") String secret) {
        return new HmacAuthSigner(secret);
    }

}
