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
package io.github.pnoker.common.data.support;

import io.github.pnoker.common.config.HmacAuthProperties;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import java.time.Clock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/** Configures the signing key used by point-value cursors. */
@AutoConfiguration
@EnableConfigurationProperties(HmacAuthProperties.class)
public class PointValueCursorConfig {

    /** Create the point-value cursor codec from the signing secret. */
    @Bean
    @ConditionalOnMissingBean
    public PointValueCursorCodec pointValueCursorCodec(HmacAuthProperties properties, Environment environment) {
        String secret = StringUtils.defaultIfBlank(
                properties.getSecret(), environment.getProperty(EnvironmentConstant.AUTH_HMAC_SECRET_ENV, ""));
        if (StringUtils.isBlank(secret)) {
            throw new IllegalStateException("Point-value cursor signing secret must be configured");
        }
        return new PointValueCursorCodec(secret, Clock.systemUTC());
    }
}
