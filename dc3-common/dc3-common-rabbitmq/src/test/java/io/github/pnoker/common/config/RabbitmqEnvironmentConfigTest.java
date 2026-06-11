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
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitmqEnvironmentConfigTest {

    private final RabbitmqEnvironmentConfig config = new RabbitmqEnvironmentConfig();

    @Test
    void storesRabbitTagForDevGroup() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(EnvironmentConstant.SPRING_ENV, EnvironmentConstant.ENV_DEV)
                .withProperty(EnvironmentConstant.SPRING_GROUP, "GroupA");

        config.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(System.getProperty(RabbitmqEnvironmentConfig.DC3_RABBIT_TAG)).isEqualTo("dev_groupa.");
    }

    @Test
    void clearsRabbitTagOutsideDev() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(EnvironmentConstant.SPRING_ENV, EnvironmentConstant.ENV_PRO)
                .withProperty(EnvironmentConstant.SPRING_GROUP, "GroupA");

        config.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(System.getProperty(RabbitmqEnvironmentConfig.DC3_RABBIT_TAG)).isEmpty();
    }

}
