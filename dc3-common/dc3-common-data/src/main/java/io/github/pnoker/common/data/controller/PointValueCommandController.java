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
import io.github.pnoker.common.data.biz.PointValueCommandService;
import io.github.pnoker.common.data.entity.vo.PointValueReadVO;
import io.github.pnoker.common.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * PointValue Controller
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_VALUE_COMMAND_URL_PREFIX)
public class PointValueCommandController implements BaseController {

    private final PointValueCommandService pointValueCommandService;

    public PointValueCommandController(PointValueCommandService pointValueCommandService) {
        this.pointValueCommandService = pointValueCommandService;
    }

    /**
     * 读指令
     *
     * @param entityVO PointValueReadVO
     * @return PointValue
     */
    @PostMapping("/read")
    public Mono<R<Boolean>> read(@Validated @RequestBody PointValueReadVO entityVO) {
        try {
            pointValueCommandService.read(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
        return Mono.just(R.ok());
    }

    /**
     * 写指令
     *
     * @param entityVO PointValueWriteVO
     * @return PointValue
     */
    @PostMapping("/write")
    public Mono<R<Boolean>> write(@Validated @RequestBody PointValueWriteVO entityVO) {
        try {
            pointValueCommandService.write(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
        return Mono.just(R.ok());
    }

}