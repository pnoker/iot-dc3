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

package io.github.pnoker.common.data.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointCommandHistoryService;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller for point command history queries.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Tag(name = "point_command_history", description = "Point command audit trail: query historical records of commands dispatched to device data points including timestamps and execution outcomes")
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_COMMAND_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class PointCommandHistoryController implements BaseController {

    private final PointCommandHistoryService pointCommandHistoryService;

    /**
     * Return the full audit record for a single point command identified by its command ID.
     *
     * @param commandId id of the dispatched point command to look up
     * @return the matched PointCommandHistoryVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('point_command_history', 'get')")
    @Operation(summary = "Get Point Command History by Command ID", description = "Return the full audit record for a single point command identified by its command ID (tenant-scoped). Use to look up the request value, response value, status, error and timing of one dispatched command.")
    @GetMapping("/get_by_command_id")
    public Mono<R<PointCommandHistoryVO>> getByCommandId(@Parameter(description = "Identifier of the point command to look up; must reference a command dispatched under the current tenant.", example = "1024") @NotBlank @RequestParam String commandId) {
        return getTenantId().flatMap(tenantId -> async(() ->
                R.ok(pointCommandHistoryService.getByCommandId(tenantId, commandId))));
    }

    /**
     * Page through the point command audit trail owned by the current tenant.
     *
     * @param queryVO optional filter query (device, point, command type, status); treated as empty when null
     * @return a page of PointCommandHistoryVO matching the query
     */
    @PreAuthorize("@perm.can('point_command_history', 'list')")
    @Operation(summary = "List Point Command History", description = "Page through the point command audit trail for the current tenant, filterable by device, point, command type and status. Use to review which commands were dispatched to data points and whether each succeeded.")
    @PostMapping("/list")
    public Mono<R<Page<PointCommandHistoryVO>>> list(@RequestBody(required = false) PointCommandHistoryQueryVO queryVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointCommandHistoryQueryVO query = Objects.isNull(queryVO) ? new PointCommandHistoryQueryVO() : queryVO;
            return R.ok(pointCommandHistoryService.list(tenantId, query));
        }));
    }

}
