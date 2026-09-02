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
package io.github.pnoker.common.auth.config;

import io.github.pnoker.common.auth.support.IdentityAuditCursorCodec;
import io.github.pnoker.common.config.HmacAuthProperties;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import java.time.Clock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Configures the signed identity-audit cursor codec from the shared HMAC secret. */
@Configuration
public class AuditCursorConfig {

    @Bean
    IdentityAuditCursorCodec identityAuditCursorCodec(HmacAuthProperties properties, Environment environment) {
        String secret = StringUtils.defaultIfBlank(
                properties.getSecret(), environment.getProperty(EnvironmentConstant.AUTH_HMAC_SECRET_ENV, ""));
        return new IdentityAuditCursorCodec(secret, Clock.systemUTC());
    }
}
