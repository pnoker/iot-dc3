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

package io.github.pnoker.driver;

import io.github.pnoker.driver.lwm2m.Lwm2mProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * LwM2M Driver Application
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@SpringBootApplication
@EnableConfigurationProperties(Lwm2mProperties.class)
public class Lwm2mDriverApplication {

    public static void main(String[] args) {
        SpringApplication.run(Lwm2mDriverApplication.class, args);
    }

}
