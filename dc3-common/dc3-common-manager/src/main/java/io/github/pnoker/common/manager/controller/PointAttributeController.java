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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.PointAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.PointAttributeVO;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 驱动属性配置信息 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_URL_PREFIX)
public class PointAttributeController implements BaseController {

    private final PointAttributeBuilder pointAttributeBuilder;
    private final PointAttributeService pointAttributeService;

    public PointAttributeController(PointAttributeBuilder pointAttributeBuilder, PointAttributeService pointAttributeService) {
        this.pointAttributeBuilder = pointAttributeBuilder;
        this.pointAttributeService = pointAttributeService;
    }

    /**
     * 新增 PointAttribute
     *
     * @param entityVO {@link PointAttributeVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<PointAttributeBO>> add(@Validated(Add.class) @RequestBody PointAttributeVO entityVO) {
        try {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            pointAttributeService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除 PointAttribute
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            pointAttributeService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新 PointAttribute
     *
     * @param entityVO {@link PointAttributeVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointAttributeVO entityVO) {
        try {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            pointAttributeService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 PointAttribute
     *
     * @param id ID
     * @return PointAttributeVO {@link PointAttributeVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<PointAttributeVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            PointAttributeBO entityBO = pointAttributeService.selectById(id);
            PointAttributeVO entityVO = pointAttributeBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 驱动ID 查询 PointAttribute
     *
     * @param id 位号属性ID
     * @return 位号属性Array
     */
    @GetMapping("/driver_id/{id}")
    public Mono<R<List<PointAttributeVO>>> selectByDriverId(@NotNull @PathVariable(value = "id") Long id) {
        try {
            List<PointAttributeBO> entityBOList = pointAttributeService.selectByDriverId(id);
            List<PointAttributeVO> entityVO = pointAttributeBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVO));
        } catch (NotFoundException ne) {
            return Mono.just(R.ok(Collections.emptyList()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 PointAttribute
     *
     * @param entityQuery 位号属性和分页参数
     * @return Page Of PointAttribute
     */
    @PostMapping("/list")
    public Mono<R<Page<PointAttributeVO>>> list(@RequestBody(required = false) PointAttributeQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new PointAttributeQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<PointAttributeBO> entityPageBO = pointAttributeService.selectByPage(entityQuery);
            Page<PointAttributeVO> entityPageVO = pointAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
