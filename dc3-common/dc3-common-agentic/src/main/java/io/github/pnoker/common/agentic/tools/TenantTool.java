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
package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.annotation.AgenticToolMetadata;
import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tenant-context tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class TenantTool {

    @Tool(description = "Get the current tenant context. Returns only the current tenant ID.")
    @AgenticToolMetadata(domain = "tenant", title = "Read current tenant context")
    public AgenticToolResult<Map<String, Long>> getCurrentTenantInfo(ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getCurrentTenantInfo", tenantId);
        return AgenticToolResult.ok("Current tenant context loaded", Map.of("tenantId", tenantId));
    }

}
