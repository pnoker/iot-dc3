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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class HmacAuthConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HmacAuthConfig.class));

    @Test
    void hmacAuthSignerIsDisabledWhenNoSecretConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HmacAuthSigner.class);
            assertThat(context.getBean(HmacAuthSigner.class).isEnabled()).isFalse();
        });
    }

    @Test
    void hmacAuthSignerUsesConfiguredPropertySecret() {
        contextRunner.withPropertyValues(EnvironmentConstant.AUTH_HMAC_SECRET_PROPERTY + "=property-secret")
                .run(context -> {
                    HmacAuthSigner signer = context.getBean(HmacAuthSigner.class);
                    assertThat(signer.isEnabled()).isTrue();
                    assertThat(signer.verify("payload", new HmacAuthSigner("property-secret").sign("payload")))
                            .isTrue();
                });
    }

    @Test
    void hmacAuthSignerUsesEnvironmentSecretFallback() {
        contextRunner.withPropertyValues(EnvironmentConstant.AUTH_HMAC_SECRET_ENV + "=env-secret")
                .run(context -> {
                    HmacAuthSigner signer = context.getBean(HmacAuthSigner.class);
                    assertThat(signer.isEnabled()).isTrue();
                    assertThat(signer.verify("payload", new HmacAuthSigner("env-secret").sign("payload")))
                            .isTrue();
                });
    }

    @Test
    void hmacAuthSignerPropertySecretTakesPrecedenceOverEnvironmentSecret() {
        contextRunner
                .withPropertyValues(
                        EnvironmentConstant.AUTH_HMAC_SECRET_PROPERTY + "=property-secret",
                        EnvironmentConstant.AUTH_HMAC_SECRET_ENV + "=env-secret")
                .run(context -> {
                    HmacAuthSigner signer = context.getBean(HmacAuthSigner.class);
                    assertThat(signer.verify("payload", new HmacAuthSigner("property-secret").sign("payload")))
                            .isTrue();
                    assertThat(signer.verify("payload", new HmacAuthSigner("env-secret").sign("payload")))
                            .isFalse();
                });
    }

    @Test
    void hmacAuthSignerBacksOffWhenUserBeanExists() {
        HmacAuthSigner customSigner = new HmacAuthSigner("custom-secret");

        contextRunner.withBean(HmacAuthSigner.class, () -> customSigner)
                .run(context -> assertThat(context.getBean(HmacAuthSigner.class)).isSameAs(customSigner));
    }

}
