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
import io.github.pnoker.common.data.biz.CommandHistoryService;
import io.github.pnoker.common.data.entity.builder.CommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.CommandCallVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.entity.R;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller for custom command call management.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.COMMAND_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class CommandHistoryController implements BaseController {

    private final CommandHistoryService commandHistoryService;

    private final CommandHistoryBuilder commandHistoryBuilder;

    @PreAuthorize("@perm.can('command_history', 'add')")
    @PostMapping("/call")
    public Mono<R<String>> call(@Validated @RequestBody CommandCallVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            String recordId = commandHistoryService.call(tenantId, entityVO);
            R<String> result = R.ok();
            result.setData(recordId);
            return result;
        }));
    }

    @PreAuthorize("@perm.can('command_history', 'get')")
    @GetMapping("/get_by_record_id")
    public Mono<R<CommandHistoryVO>> getByRecordId(@NotBlank @RequestParam String recordId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandHistoryDO entityDO = commandHistoryService.getByRecordId(tenantId, recordId);
            return R.ok(commandHistoryBuilder.buildVOByDO(entityDO));
        }));
    }

    @PreAuthorize("@perm.can('command_history', 'list')")
    @PostMapping("/list")
    public Mono<R<Page<CommandHistoryVO>>> list(@RequestBody(required = false) CommandHistoryQueryVO queryVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandHistoryQueryVO query = Objects.isNull(queryVO) ? new CommandHistoryQueryVO() : queryVO;
            Page<CommandHistoryDO> page = commandHistoryService.list(tenantId, query);
            return R.ok(commandHistoryBuilder.buildVOPageByDOPage(page));
        }));
    }

}
