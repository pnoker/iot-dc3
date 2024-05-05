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

package io.github.pnoker.center.data.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.data.biz.PointValueService;
import io.github.pnoker.center.data.entity.builder.PointValueBuilder;
import io.github.pnoker.center.data.entity.vo.PointValueVO;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PointValue Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-位号数据")
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
    @Operation(summary = "分页查询-最新位号值")
    public R<Page<PointValueVO>> latest(@RequestBody PointValueQuery pointValueQuery) {
        try {
            if (ObjectUtil.isEmpty(pointValueQuery)) {
                pointValueQuery = new PointValueQuery();
            }
            pointValueQuery.setTenantId(getTenantId());
            Page<PointValueBO> entityPageBO = pointValueService.latest(pointValueQuery);
            Page<PointValueVO> entityPageVO = pointValueBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 历史 PointValue
     *
     * @param entityQuery 位号值和分页参数
     * @return 带分页的 {@link PointValueBO}
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询-历史位号值")
    public R<Page<PointValueVO>> list(@RequestBody(required = false) PointValueQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new PointValueQuery();
            }
            Page<PointValueBO> entityPageBO = pointValueService.page(entityQuery);
            Page<PointValueVO> entityPageVO = pointValueBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
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
    @Operation(summary = "查询位号值历史数据")
    public R<List<String>> history(
            @Schema(description = "设备ID") @NotNull @PathVariable(name = "deviceId") Long deviceId,
            @Schema(description = "位号ID") @NotNull @PathVariable(name = "pointId") Long pointId,
            @Schema(description = "历史数据量, 最多500条") @RequestParam(name = "count", required = false, defaultValue = "100") Integer count) {
        try {
            List<String> history = pointValueService.history(deviceId, pointId, count);
            return R.ok(history);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}