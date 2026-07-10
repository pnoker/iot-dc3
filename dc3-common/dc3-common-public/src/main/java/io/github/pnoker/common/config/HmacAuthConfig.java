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

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.utils.HmacAuthSigner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Auto-configuration for the shared {@link HmacAuthSigner} bean. Picked up by every
 * application that depends on {@code dc3-common-public} via its
 * {@code AutoConfiguration.imports}, so the gateway and every backend service share the
 * same secret-loading rules.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@AutoConfiguration
@EnableConfigurationProperties(HmacAuthProperties.class)
public class HmacAuthConfig {

    private static final String WEAK_DEFAULT_SECRET = "io.github.pnoker.dc3";

    /**
     * Build the HMAC signer bean, resolving the secret from properties or the
     * environment variable and validating it in protected environments.
     *
     * @param properties  HMAC auth properties
     * @param environment Spring environment
     * @return the HMAC signer
     */
    @Bean
    @ConditionalOnMissingBean
    public HmacAuthSigner hmacAuthSigner(HmacAuthProperties properties, Environment environment) {
        String secret = StringUtils.defaultIfBlank(properties.getSecret(),
                environment.getProperty(EnvironmentConstant.AUTH_HMAC_SECRET_ENV, ""));
        validateSecret(secret, environment);
        return new HmacAuthSigner(secret);
    }

    /**
     * Validate the HMAC secret in protected (pre/pro) environments: it must be set and
     * must not be the weak development default. No-op in other environments.
     *
     * @param secret      the resolved secret
     * @param environment Spring environment
     */
    private void validateSecret(String secret, Environment environment) {
        if (!isProtectedEnvironment(environment)) {
            return;
        }
        if (StringUtils.isBlank(secret)) {
            throw new IllegalStateException(EnvironmentConstant.AUTH_HMAC_SECRET_PROPERTY + " or "
                    + EnvironmentConstant.AUTH_HMAC_SECRET_ENV
                    + " must be configured in pre/pro environments");
        }
        if (WEAK_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException(EnvironmentConstant.AUTH_HMAC_SECRET_PROPERTY
                    + " must not use the default development secret in pre/pro environments");
        }
    }

    /**
     * Return whether the runtime is a protected environment (pre or pro), determined
     * from the active Spring profiles and the {@code spring.env} property.
     *
     * @param environment Spring environment
     * @return true if running under the pre or pro profile
     */
    private boolean isProtectedEnvironment(Environment environment) {
        Set<String> names = Arrays.stream(environment.getActiveProfiles())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(profile -> !profile.isEmpty())
                .collect(Collectors.toSet());
        String springEnv = environment.getProperty(EnvironmentConstant.SPRING_ENV);
        if (StringUtils.isNotBlank(springEnv)) {
            names.add(springEnv.trim());
        }
        return names.contains(EnvironmentConstant.ENV_PRE) || names.contains(EnvironmentConstant.ENV_PRO);
    }

}
