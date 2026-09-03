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

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.query.FacadeCommandOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Command-domain tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class CommandTool {

    private final CommandFacade commandFacade;

    /** Look up the command by id. */
    public Mono<AgenticToolResult<FacadeCommandBO>> lookupCommandByIdReactive(Long commandId, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        return commandFacade
                .getById(tenantId, commandId)
                .map(value -> AgenticToolResult.ok("Command loaded", value))
                .defaultIfEmpty(AgenticToolResult.notFound("Command not found for ID: " + commandId));
    }

    /** Look up the commands for the given ids. */
    public Mono<AgenticToolResult<List<FacadeCommandBO>>> lookupCommandsByIdsReactive(
            List<Long> commandIds, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(commandIds);
        if (ids.isEmpty()) return Mono.just(AgenticToolResult.invalid("No valid command IDs provided."));
        return commandFacade
                .listByIds(tenantId, ids)
                .collectList()
                .map(values -> values.isEmpty()
                        ? AgenticToolResult.empty("No commands found for IDs: " + ids, List.of())
                        : AgenticToolResult.ok("Commands loaded", values));
    }

    /** Search commands matching the request. */
    public Mono<AgenticToolResult<OffsetPage<FacadeCommandBO>>> searchCommandsReactive(
            String commandName, Long profileId, long offset, int limit, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        return Mono.defer(() -> commandFacade.list(new FacadeCommandOffsetQuery(
                        tenantId,
                        commandName,
                        null,
                        null,
                        null,
                        profileId,
                        null,
                        null,
                        null,
                        offset,
                        limit,
                        List.of())))
                .map(value -> value.items().isEmpty()
                        ? AgenticToolResult.empty("No commands found.", value)
                        : AgenticToolResult.ok("Command page loaded", value));
    }

    /** List command tools matched by device id. */
    public Mono<AgenticToolResult<OffsetPage<FacadeCommandBO>>> listCommandsByDeviceIdReactive(
            Long deviceId, long offset, int limit, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        return Mono.defer(() -> commandFacade.list(new FacadeCommandOffsetQuery(
                        tenantId, null, null, null, null, null, null, null, deviceId, offset, limit, List.of())))
                .map(value -> value.items().isEmpty()
                        ? AgenticToolResult.empty("No commands found for device ID: " + deviceId, value)
                        : AgenticToolResult.ok("Command page loaded for device " + deviceId, value));
    }

    /** List command tools matched by profile id. */
    public Mono<AgenticToolResult<OffsetPage<FacadeCommandBO>>> listCommandsByProfileIdReactive(
            Long profileId, long offset, int limit, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        return Mono.defer(() -> commandFacade.list(new FacadeCommandOffsetQuery(
                        tenantId, null, null, null, null, profileId, null, null, null, offset, limit, List.of())))
                .map(value -> value.items().isEmpty()
                        ? AgenticToolResult.empty("No commands found for profile ID: " + profileId, value)
                        : AgenticToolResult.ok("Command page loaded for profile " + profileId, value));
    }
}
