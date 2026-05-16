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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * System-health tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class SystemTool {

    private static final String STATUS_UNAVAILABLE = "Status and health tools are not available in this deployment mode.";

    private final Optional<StatusHealthFacade> statusHealthFacade;

    public SystemTool(Optional<StatusHealthFacade> statusHealthFacade) {
        this.statusHealthFacade = statusHealthFacade;
    }

    @Tool(description = "Get a system health snapshot: center services, infrastructure, driver fleet, and device fleet.")
    public AgenticToolResult<FacadeSystemHealthBO> getSystemHealth(ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getSystemHealth", tenantId);
        recordTool(toolContext, "getSystemHealth", "Get system health");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        FacadeSystemHealthBO health = facade.systemHealth(tenantId);
        if (Objects.isNull(health)) {
            return AgenticToolResult.unavailable("System health snapshot is unavailable.");
        }
        return AgenticToolResult.ok("System health loaded", health);
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "system", description);
    }

}
