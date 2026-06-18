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

package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.tool.ToolQuality;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolQualityMergeTest {

    private static McpToolRecord record(String method) {
        McpToolRecord r = new McpToolRecord();
        r.setApiCode("dc3-center-manager:" + method + ":/device/x");
        r.setHttpMethod(method);
        r.setToolTitle("fallback title");
        r.setRemark("fallback remark");
        return r;
    }

    @Test
    void appliesDeclaredQualityAndDerivesReadOnlyForGet() {
        McpToolRecord r = record("GET");
        OAuthMcpRuntimeServiceImpl.applyQuality(r, ToolQuality.builder()
                .summary("List Devices").description("List devices.")
                .riskLevel("LOW").destructive(false).idempotent(true).openWorld(false).hidden(false)
                .inputSchema("{\"type\":\"object\"}").build());

        assertThat(r.getRiskLevel()).isEqualTo("LOW");
        assertThat(r.getReadOnlyHint()).isEqualTo((byte) 1); // GET derived
        assertThat(r.getDestructiveHint()).isEqualTo((byte) 0);
        assertThat(r.getIdempotentHint()).isEqualTo((byte) 1);
        assertThat(r.getOpenWorldHint()).isEqualTo((byte) 0);
        assertThat(r.getToolTitle()).isEqualTo("List Devices");
        assertThat(r.getRemark()).isEqualTo("List devices.");
        assertThat(r.getToolExt()).isEqualTo("{\"inputSchema\":{\"type\":\"object\"}}");
        assertThat(r.getEnableFlag()).isEqualTo((byte) 0);
    }

    @Test
    void appliesConservativeDefaultsWhenQualityIsNull() {
        McpToolRecord r = record("POST");
        OAuthMcpRuntimeServiceImpl.applyQuality(r, null);

        assertThat(r.getRiskLevel()).isEqualTo("HIGH");
        assertThat(r.getReadOnlyHint()).isEqualTo((byte) 0);   // POST
        assertThat(r.getDestructiveHint()).isEqualTo((byte) 1);
        assertThat(r.getIdempotentHint()).isEqualTo((byte) 0);
        assertThat(r.getOpenWorldHint()).isEqualTo((byte) 1);
        assertThat(r.getToolTitle()).isEqualTo("fallback title"); // keep SQL fallback
    }

    @Test
    void hiddenDisablesTheCatalogRow() {
        McpToolRecord r = record("GET");
        OAuthMcpRuntimeServiceImpl.applyQuality(r, ToolQuality.builder()
                .riskLevel("LOW").hidden(true).build());

        assertThat(r.getEnableFlag()).isEqualTo((byte) 1); // hidden -> disabled
    }
}
