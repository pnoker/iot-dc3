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
package io.github.pnoker.common.agentic.tool;

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.entity.common.RequestHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Auth-domain tools exposed to the LLM via Spring AI @Tool.
 * <p>
 * These tools only expose low-sensitivity information from the current authenticated
 * request context. They intentionally do not accept IDs, login names, tenant codes, or
 * return phone/email/login/password-related data.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class AuthToolSet {

    @Tool(description = "Get the current tenant context. Returns only the current tenant ID.")
    public String getCurrentTenantInfo(ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "getCurrentTenantInfo", tenantId);
        recordTool(toolContext, "getCurrentTenantInfo", "Read current tenant context");
        return String.format("CurrentTenant: tenantId=%d", tenantId);
    }

    @Tool(description = "Get the current user profile. Returns only user ID, username, and nickname.")
    public String getCurrentUserProfile(ToolContext toolContext) {
        RequestHeader.UserHeader header = AgenticRequestContext.requireUserHeader();
        Long userId = AgenticRequestContext.requireUserId(toolContext);
        log.debug("Agentic tool invoked, tool={}, userId={}", "getCurrentUserProfile", userId);
        recordTool(toolContext, "getCurrentUserProfile", "Read current user profile");
        return String.format("CurrentUser: userId=%d, username=%s, nickname=%s", userId, header.getUserName(),
                header.getNickName());
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "auth", description);
    }

}
