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
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.api.UserLoginFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Auth-domain tools exposed to the LLM via Spring AI @Tool.
 * <p>
 * These tools allow the model to query tenant, user, and login information by calling the
 * existing facade interfaces (routed via gRPC or local depending on dc3.facade.mode).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class AuthToolSet {

    private final TenantFacade tenantFacade;

    private final UserFacade userFacade;

    private final UserLoginFacade userLoginFacade;

    public AuthToolSet(TenantFacade tenantFacade, UserFacade userFacade, UserLoginFacade userLoginFacade) {
        this.tenantFacade = tenantFacade;
        this.userFacade = userFacade;
        this.userLoginFacade = userLoginFacade;
    }

    @Tool(description = "Look up a tenant by its unique code. Returns tenant name, code, and enable status.")
    public String lookupTenantByCode(
            @ToolParam(description = "The unique tenant code, e.g. 'default'") String tenantCode,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, tenantCode={}", "lookupTenantByCode", tenantId,
                tenantCode);
        FacadeTenantBO bo = tenantFacade.selectByCode(tenantCode);
        if (Objects.isNull(bo) || !Objects.equals(tenantId, bo.getId())) {
            return "Tenant not found for code: " + tenantCode;
        }
        return String.format("Tenant: name=%s, code=%s, enabled=%s", bo.getTenantName(), bo.getTenantCode(),
                bo.getEnableFlag());
    }

    @Tool(description = "Look up a user by their numeric ID. Returns nickname, username, email, and phone.")
    public String lookupUserById(@ToolParam(description = "The numeric user ID") Long userId, ToolContext toolContext) {
        Long requestUserId = AgenticRequestContext.requireUserId(toolContext);
        log.debug("Agentic tool invoked, tool={}, requestUserId={}, userId={}", "lookupUserById", requestUserId,
                userId);
        if (!Objects.equals(requestUserId, userId)) {
            return "User not found for ID: " + userId;
        }
        FacadeUserBO bo = userFacade.selectById(userId);
        if (Objects.isNull(bo)) {
            return "User not found for ID: " + userId;
        }
        return String.format("User: nickname=%s, username=%s, phone=%s, email=%s", bo.getNickName(), bo.getUserName(),
                bo.getPhone(), bo.getEmail());
    }

    @Tool(description = "Look up a user login record by login name. Returns the login name, associated user ID, and enable status.")
    public String lookupUserLoginByName(
            @ToolParam(description = "The login name (username used for authentication)") String loginName,
            ToolContext toolContext) {
        Long userId = AgenticRequestContext.requireUserId(toolContext);
        log.debug("Agentic tool invoked, tool={}, userId={}, loginName={}", "lookupUserLoginByName", userId,
                loginName);
        FacadeUserLoginBO bo = userLoginFacade.selectByName(loginName);
        if (Objects.isNull(bo) || !Objects.equals(userId, bo.getUserId())) {
            return "User login not found for name: " + loginName;
        }
        return String.format("UserLogin: loginName=%s, userId=%d, enabled=%s", bo.getLoginName(), bo.getUserId(),
                bo.getEnableFlag());
    }

}
