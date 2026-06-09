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

package io.github.pnoker.common.data.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI group definition for the Data Center.
 * <p>
 * Produces a {@code data} document group covering point-value ingestion,
 * query, command dispatch, events, notifications, and dashboard endpoints.
 * When this module is deployed standalone the Swagger UI shows a single group;
 * when bundled into {@code dc3-center-single} it appears as one of several
 * selectable groups.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Configuration
public class DataApiGroupConfig {

    @Bean
    public GroupedOpenApi dataApiGroup() {
        return GroupedOpenApi.builder()
                .group("data")
                .displayName("Data Center")
                .packagesToScan("io.github.pnoker.common.data.controller")
                .build();
    }
}
