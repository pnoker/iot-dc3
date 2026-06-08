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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger documentation configuration for IoT DC3 services.
 * <p>
 * Produces the {@code /v3/api-docs} (JSON) and {@code /swagger-ui.html}
 * (browser UI) endpoints that describe every REST endpoint with its
 * parameters, request bodies, and response schemas.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI dc3OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IoT DC3 REST API")
                        .description("""
                                IoT DC3 platform REST API documentation.

                                Covers all service modules: auth (authentication and authorization),
                                manager (device/driver/point/profile configuration), data (telemetry,
                                events, commands, notifications), and agentic (AI chat and tools).
                                """)
                        .version("2026.6.0")
                        .contact(new Contact()
                                .name("pnoker")
                                .email("pnokers@icloud.com")
                                .url("https://github.com/pnoker/iot-dc3"))
                        .license(new License()
                                .name("GNU Affero General Public License v3.0")
                                .url("https://www.gnu.org/licenses/agpl-3.0.txt")));
    }
}
