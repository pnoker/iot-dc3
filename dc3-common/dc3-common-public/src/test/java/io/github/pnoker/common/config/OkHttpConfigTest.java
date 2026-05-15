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

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OkHttpConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OkHttpConfig.class));

    @Test
    void okHttpClientIsCreatedWithDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OkHttpClient.class);
            assertThat(context).hasSingleBean(OkHttpProperties.class);

            OkHttpClient client = context.getBean(OkHttpClient.class);
            assertThat(client.callTimeoutMillis()).isEqualTo(15_000);
            assertThat(client.connectTimeoutMillis()).isEqualTo(15_000);
            assertThat(client.readTimeoutMillis()).isEqualTo(15_000);
            assertThat(client.writeTimeoutMillis()).isEqualTo(15_000);
            assertThat(client.retryOnConnectionFailure()).isTrue();
        });
    }

    @Test
    void okHttpClientUsesConfiguredProperties() {
        contextRunner
                .withPropertyValues(
                        "dc3.http.client.retry-on-connection-failure=false",
                        "dc3.http.client.call-timeout=3s",
                        "dc3.http.client.connect-timeout=4s",
                        "dc3.http.client.read-timeout=5s",
                        "dc3.http.client.write-timeout=6s")
                .run(context -> {
                    OkHttpClient client = context.getBean(OkHttpClient.class);
                    assertThat(client.retryOnConnectionFailure()).isFalse();
                    assertThat(client.callTimeoutMillis()).isEqualTo(3_000);
                    assertThat(client.connectTimeoutMillis()).isEqualTo(4_000);
                    assertThat(client.readTimeoutMillis()).isEqualTo(5_000);
                    assertThat(client.writeTimeoutMillis()).isEqualTo(6_000);
                });
    }

    @Test
    void okHttpClientBacksOffWhenUserBeanExists() {
        OkHttpClient customClient = new OkHttpClient.Builder().callTimeout(java.time.Duration.ofSeconds(1)).build();

        contextRunner.withBean(OkHttpClient.class, () -> customClient)
                .run(context -> assertThat(context.getBean(OkHttpClient.class)).isSameAs(customClient));
    }

}
