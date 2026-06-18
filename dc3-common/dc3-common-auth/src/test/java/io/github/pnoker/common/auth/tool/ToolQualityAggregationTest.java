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

class ToolQualityAggregationTest {

    private final McpOpenApiAggregator aggregator = new McpOpenApiAggregator();

    @Test
    void parsesXDc3AiFlagsSummaryDescriptionAndSchema() {
        Map<String, ToolQuality> quality = aggregator.toolQualityByApiCode();

        ToolQuality add = quality.get("dc3-center-fixturesvc:POST:/device/add");
        assertThat(add).isNotNull();
        assertThat(add.getSummary()).isEqualTo("Add Device");
        assertThat(add.getDescription()).isEqualTo("Create a device under the tenant.");
        assertThat(add.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(add.getDestructive()).isFalse();
        assertThat(add.getIdempotent()).isFalse();
        assertThat(add.getOpenWorld()).isFalse();
        assertThat(add.getHidden()).isFalse();
        assertThat(add.getInputSchema()).contains("deviceName");
    }

    @Test
    void leavesUndeclaredFlagsNull() {
        // An operation in the fixture WITHOUT an x-dc3-ai block keeps flags null
        // so the refresh applies conservative defaults.
        ToolQuality list = aggregator.toolQualityByApiCode()
                .get("dc3-center-fixturesvc:POST:/device/list_by_ids");
        assertThat(list).isNotNull();
        assertThat(list.getRiskLevel()).isNull();
        assertThat(list.getDestructive()).isNull();
    }
}
