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

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.entity.model.PointCommandDO;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.entity.R;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing point command management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_COMMAND_URL_PREFIX)
@RequiredArgsConstructor
public class PointCommandController implements BaseController {

    private final PointCommandService pointCommandService;

    /**
     * Read instruction
     *
     * @param entityVO PointCommandReadVO
     * @return PointValue
     */
    @PostMapping("/read")
    public Mono<R<Boolean>> read(@Validated @RequestBody PointCommandReadVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            pointCommandService.read(tenantId, entityVO);
            return R.ok();
        }));
    }

    /**
     * Write instruction
     *
     * @param entityVO PointCommandWriteVO
     * @return PointValue
     */
    @PostMapping("/write")
    public Mono<R<Boolean>> write(@Validated @RequestBody PointCommandWriteVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            pointCommandService.write(tenantId, entityVO);
            return R.ok();
        }));
    }

    /**
     * Query a single command by commandId
     *
     * @param commandId unique command identifier
     * @return command record
     */
    @GetMapping("/get_by_command_id")
    public Mono<R<PointCommandDO>> getByCommandId(@NotBlank @RequestParam(value = "commandId") String commandId) {
        return async(() -> R.ok(pointCommandService.getByCommandId(commandId)));
    }

}
