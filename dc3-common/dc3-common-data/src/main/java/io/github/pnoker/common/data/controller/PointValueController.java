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
 * @version 2025.6.0
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
     * @param entityQuery 位号值和分页参数
     * @return 带分页的 {@link PointValueBO}
     */
    @PostMapping("/latest")
    public Mono<R<Page<PointValueVO>>> latest(@RequestBody PointValueQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                PointValueQuery query = Objects.isNull(entityQuery) ? new PointValueQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<PointValueBO> entityPageBO = pointValueService.latest(query);
                Page<PointValueVO> entityPageVO = pointValueBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
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