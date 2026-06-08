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
import io.github.pnoker.common.data.entity.builder.PointCommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.common.entity.R;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for point command history queries.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Tag(name = "point_command_history", description = "位号指令历史")
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_COMMAND_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class PointCommandHistoryController implements BaseController {

    private final PointCommandHistoryService pointCommandHistoryService;

    private final PointCommandHistoryBuilder pointCommandHistoryBuilder;

    @PreAuthorize("@perm.can('point_command_history', 'get')")
    @Operation(summary = "查询PointCommandHistory", description = "根据条件查询PointCommandHistory")
    @GetMapping("/get_by_command_id")
    public Mono<R<PointCommandHistoryVO>> getByCommandId(@NotBlank @RequestParam String commandId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointCommandHistoryDO entityDO = pointCommandHistoryService.getByCommandId(tenantId, commandId);
            return R.ok(pointCommandHistoryBuilder.buildVOByDO(entityDO));
        }));
    }

    @PreAuthorize("@perm.can('point_command_history', 'list')")
    @Operation(summary = "查询PointCommandHistory列表", description = "分页查询PointCommandHistory列表")
    @PostMapping("/list")
    public Mono<R<Page<PointCommandHistoryVO>>> list(@RequestBody(required = false) PointCommandHistoryQueryVO queryVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointCommandHistoryQueryVO query = Objects.isNull(queryVO) ? new PointCommandHistoryQueryVO() : queryVO;
            Page<PointCommandHistoryDO> page = pointCommandHistoryService.list(tenantId, query);
            return R.ok(pointCommandHistoryBuilder.buildVOPageByDOPage(page));
        }));
    }

}
