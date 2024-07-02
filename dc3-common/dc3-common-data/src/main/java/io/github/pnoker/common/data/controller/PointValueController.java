/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.vo.PointValueVO;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * PointValue Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_VALUE_URL_PREFIX)
public class PointValueController implements BaseController {

    private final PointValueBuilder pointValueBuilder;
    private final PointValueService pointValueService;

    public PointValueController(PointValueBuilder pointValueBuilder, PointValueService pointValueService) {
        this.pointValueBuilder = pointValueBuilder;
        this.pointValueService = pointValueService;
    }

    /**
     * 分页查询 最新 PointValue
     *
     * @param pointValueQuery 位号值和分页参数
     * @return 带分页的 {@link PointValueBO}
     */
    @PostMapping("/latest")
    public Mono<R<Page<PointValueVO>>> latest(@RequestBody PointValueQuery pointValueQuery) {
        try {
            if (Objects.isNull(pointValueQuery)) {
                pointValueQuery = new PointValueQuery();
            }
            pointValueQuery.setTenantId(getTenantId());
            Page<PointValueBO> entityPageBO = pointValueService.latest(pointValueQuery);
            Page<PointValueVO> entityPageVO = pointValueBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 历史 PointValue
     *
     * @param entityQuery 位号值和分页参数
     * @return 带分页的 {@link PointValueBO}
     */
    @PostMapping("/list")
    public Mono<R<Page<PointValueVO>>> list(@RequestBody(required = false) PointValueQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new PointValueQuery();
            }
            Page<PointValueBO> entityPageBO = pointValueService.page(entityQuery);
            Page<PointValueVO> entityPageVO = pointValueBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 查询位号 PointValue 历史
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return 带分页的 {@link PointValueBO}
     */
    @GetMapping("/history/device_id/{deviceId}/point_id/{pointId}")
    public Mono<R<List<String>>> history(
            @NotNull @PathVariable(name = "deviceId") Long deviceId,
            @NotNull @PathVariable(name = "pointId") Long pointId,
            @RequestParam(name = "count", required = false, defaultValue = "100") Integer count) {
        try {
            List<String> history = pointValueService.history(deviceId, pointId, count);
            return Mono.just(R.ok(history));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}