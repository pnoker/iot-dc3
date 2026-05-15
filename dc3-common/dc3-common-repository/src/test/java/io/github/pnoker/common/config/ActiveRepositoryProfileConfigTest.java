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
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveRepositoryProfileConfigTest {

    private final ActiveRepositoryProfileConfig config = new ActiveRepositoryProfileConfig();

    @Test
    void postProcessEnvironmentAddsRepositoryProfileByDefault() {
        StandardEnvironment environment = new StandardEnvironment();

        config.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getActiveProfiles()).contains(EnvironmentConstant.REPOSITORY_PROFILE);
    }

    @Test
    void postProcessEnvironmentSkipsRepositoryProfileWhenDisabled() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources()
                .addFirst(new MapPropertySource("test",
                        Map.of(EnvironmentConstant.REPOSITORY_AUTO_PROFILE, "false")));

        config.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getActiveProfiles()).doesNotContain(EnvironmentConstant.REPOSITORY_PROFILE);
    }

}
