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

package io.github.pnoker.common.auth.tool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The JSON side (aggregator) and the DB side (ResourceRegistrySyncServiceImpl.apiCodeOf) must
 * produce the same {@code api_code} so the refresh join lands. This guards against either side
 * silently changing the format.
 */
class ApiCodeConsistencyTest {

    private final McpOpenApiAggregator aggregator = new McpOpenApiAggregator();

    @Test
    void aggregatorKeysUseServiceColonMethodColonPath() {
        Map<String, ToolQuality> quality = aggregator.toolQualityByApiCode();
        // Write-side format: serviceName + ":" + ApiTypeEnum.<METHOD>.name() + ":" + path
        // -> dc3-center-fixturesvc:POST:/device/add . The aggregator must match it verbatim.
        assertThat(quality).containsKey("dc3-center-fixturesvc:POST:/device/add");
        assertThat(quality.keySet()).allMatch(k -> k.matches("^dc3-center-[^:]+:(GET|POST|PUT|DELETE):/.*"));
    }
}
