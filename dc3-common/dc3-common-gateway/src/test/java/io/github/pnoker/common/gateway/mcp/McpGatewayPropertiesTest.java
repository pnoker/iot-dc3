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

package io.github.pnoker.common.gateway.mcp;

import io.github.pnoker.common.constant.service.ManagerConstant;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpGatewayPropertiesTest {

    @Test
    void backendBaseUrlReturnsConfiguredServiceUrlWithoutTrailingSlash() {
        McpGatewayProperties properties = new McpGatewayProperties();
        properties.setBackendBaseUrls(new LinkedHashMap<>(Map.of(
                ManagerConstant.SERVICE_NAME, "http://dc3-center-manager:8400/manager/"
        )));

        assertThat(properties.backendBaseUrl(ManagerConstant.SERVICE_NAME))
                .isEqualTo("http://dc3-center-manager:8400/manager");
    }

    @Test
    void backendBaseUrlRejectsUnknownServiceName() {
        McpGatewayProperties properties = new McpGatewayProperties();

        assertThatThrownBy(() -> properties.backendBaseUrl("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown backend service");
    }

}
