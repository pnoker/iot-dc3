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
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeCommandQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Command-domain tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandTool {

    private final CommandFacade commandFacade;

    @Tool(description = "Look up a command (custom instruction) by its numeric ID. Returns command name, code, type (custom/config/action), call type (sync/async), timeout, and bound profile ID.")
    @AgenticToolMetadata(domain = "command", title = "Query command by ID")
    public AgenticToolResult<FacadeCommandBO> lookupCommandById(
            @ToolParam(description = "The numeric command ID") Long commandId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, commandId={}", "lookupCommandById", tenantId, commandId);
        FacadeCommandBO bo = commandFacade.getById(tenantId, commandId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("Command not found for ID: " + commandId);
        }
        return AgenticToolResult.ok("Command loaded", bo);
    }

    @Tool(description = "Batch look up commands by numeric IDs. Returns up to 50 tenant-scoped commands.")
    @AgenticToolMetadata(domain = "command", title = "Batch query commands by IDs")
    public AgenticToolResult<List<FacadeCommandBO>> lookupCommandsByIds(
            @ToolParam(description = "The numeric command IDs") List<Long> commandIds,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(commandIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, commandIds={}", "lookupCommandsByIds", tenantId, ids);
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid command IDs provided.");
        }
        List<FacadeCommandBO> commands = commandFacade.listByIds(tenantId, ids);
        if (Objects.isNull(commands) || commands.isEmpty()) {
            return AgenticToolResult.empty("No commands found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Commands loaded", commands);
    }

    @Tool(description = "Search for commands with optional filters. Returns a paginated list.")
    @AgenticToolMetadata(domain = "command", title = "Search commands")
    public AgenticToolResult<FacadePage<FacadeCommandBO>> searchCommands(
            @ToolParam(description = "Command name filter (partial match), or null to skip") String commandName,
            @ToolParam(description = "Profile ID filter, or null to skip") Long profileId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, commandName={}, profileId={}, page={}, size={}",
                "searchCommands", tenantId, commandName, profileId, page, size);

        FacadeCommandQuery query = new FacadeCommandQuery();
        query.setCommandName(commandName);
        query.setProfileId(profileId);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeCommandBO> result = commandFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No commands found.", result);
        }
        return AgenticToolResult.ok("Command page loaded", result);
    }

    @Tool(description = "List commands bound to a specific device ID. Use this before executing commands when the user knows the device but not the command ID.")
    @AgenticToolMetadata(domain = "command", title = "List commands by device")
    public AgenticToolResult<FacadePage<FacadeCommandBO>> listCommandsByDeviceId(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, page={}, size={}", "listCommandsByDeviceId",
                tenantId, deviceId, page, size);

        FacadeCommandQuery query = new FacadeCommandQuery();
        query.setDeviceId(deviceId);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeCommandBO> result = commandFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No commands found for device ID: " + deviceId, result);
        }
        return AgenticToolResult.ok("Command page loaded for device " + deviceId, result);
    }

    @Tool(description = "List commands under a specific profile/template ID. Use this when the user wants all custom instructions defined by a template.")
    @AgenticToolMetadata(domain = "command", title = "List commands by profile")
    public AgenticToolResult<FacadePage<FacadeCommandBO>> listCommandsByProfileId(
            @ToolParam(description = "The profile/template ID") Long profileId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}, page={}, size={}",
                "listCommandsByProfileId", tenantId, profileId, page, size);

        FacadeCommandQuery query = new FacadeCommandQuery();
        query.setProfileId(profileId);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeCommandBO> result = commandFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No commands found for profile ID: " + profileId, result);
        }
        return AgenticToolResult.ok("Command page loaded for profile " + profileId, result);
    }

}
