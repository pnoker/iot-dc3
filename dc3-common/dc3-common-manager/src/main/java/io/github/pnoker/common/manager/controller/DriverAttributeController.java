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
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.DriverAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.DriverAttributeVO;
import io.github.pnoker.common.manager.service.DriverAttributeService;
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
 * 驱动连接配置信息 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DRIVER_ATTRIBUTE_URL_PREFIX)
public class DriverAttributeController implements BaseController {

    private final DriverAttributeBuilder driverAttributeBuilder;
    private final DriverAttributeService driverAttributeService;

    public DriverAttributeController(DriverAttributeBuilder driverAttributeBuilder, DriverAttributeService driverAttributeService) {
        this.driverAttributeBuilder = driverAttributeBuilder;
        this.driverAttributeService = driverAttributeService;
    }

    /**
     * 新增 DriverAttribute
     *
     * @param entityVO {@link DriverAttributeVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverAttributeVO entityVO) {
        try {
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            driverAttributeService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除 DriverAttribute
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            driverAttributeService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新 DriverAttribute
     *
     * @param entityVO {@link DriverAttributeVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverAttributeVO entityVO) {
        try {
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByVO(entityVO);
            driverAttributeService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 DriverAttribute
     *
     * @param id ID
     * @return DriverAttributeVO {@link DriverAttributeVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<DriverAttributeVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            DriverAttributeBO entityBO = driverAttributeService.selectById(id);
            DriverAttributeVO entityVO = driverAttributeBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 驱动ID 查询 DriverAttribute
     *
     * @param id 驱动属性ID
     * @return DriverAttribute
     */
    @GetMapping("/driver_id/{id}")
    public Mono<R<List<DriverAttributeVO>>> selectByDriverId(@NotNull @PathVariable(value = "id") Long id) {
        try {
            List<DriverAttributeBO> entityBOList = driverAttributeService.selectByDriverId(id);
            List<DriverAttributeVO> entityVO = driverAttributeBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVO));
        } catch (NotFoundException ne) {
            return Mono.just(R.ok(Collections.emptyList()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 DriverAttribute
     *
     * @param entityQuery 驱动属性Dto
     * @return Page Of DriverAttribute
     */
    @PostMapping("/list")
    public Mono<R<Page<DriverAttributeVO>>> list(@RequestBody(required = false) DriverAttributeQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new DriverAttributeQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<DriverAttributeBO> entityPageBO = driverAttributeService.selectByPage(entityQuery);
            Page<DriverAttributeVO> entityPageVO = driverAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
