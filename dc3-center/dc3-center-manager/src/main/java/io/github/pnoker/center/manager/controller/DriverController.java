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

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.builder.DriverBuilder;
import io.github.pnoker.center.manager.entity.query.DriverQuery;
import io.github.pnoker.center.manager.entity.vo.DriverVO;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 驱动 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-驱动")
@RequestMapping(ManagerConstant.DRIVER_URL_PREFIX)
public class DriverController implements BaseController {

    @Resource
    private DriverBuilder driverBuilder;

    @Resource
    private DriverService driverService;

    /**
     * 新增 Driver
     *
     * @param entityVO {@link DriverVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-驱动")
    public R<String> add(@Validated(Add.class) @RequestBody DriverVO entityVO) {
        try {
            DriverBO entityBO = driverBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            driverService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Driver
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            driverService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 Driver
     *
     * @param entityVO {@link DriverVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody DriverVO entityVO) {
        try {
            DriverBO entityBO = driverBuilder.buildBOByVO(entityVO);
            driverService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Driver
     *
     * @param id ID
     * @return DriverVO {@link DriverVO}
     */
    @GetMapping("/id/{id}")
    public R<DriverVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            DriverBO entityBO = driverService.selectById(id);
            DriverVO entityVO = driverBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 集合查询 Driver
     *
     * @param driverIds 驱动ID集
     * @return Map(ID, DriverVO)
     */
    @PostMapping("/ids")
    public R<Map<Long, DriverVO>> selectByIds(@RequestBody Set<Long> driverIds) {
        try {
            List<DriverBO> entityBOS = driverService.selectByIds(driverIds);
            Map<Long, DriverVO> driverMap = entityBOS.stream().collect(Collectors.toMap(DriverBO::getId, entityBO -> driverBuilder.buildVOByBO(entityBO)));
            return R.ok(driverMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 SERVICENAME 查询 Driver
     *
     * @param serviceName 驱动服务名称
     * @return Driver
     */
    @GetMapping("/service/{serviceName}")
    public R<DriverVO> selectByServiceName(@NotNull @PathVariable(value = "serviceName") String serviceName) {
        try {
            DriverBO entityBO = driverService.selectByServiceName(serviceName, getTenantId());
            DriverVO entityVO = driverBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 Driver
     *
     * @param entityQuery Driver Dto
     * @return Page Of Driver
     */
    @PostMapping("/list")
    public R<Page<DriverVO>> list(@RequestBody(required = false) DriverQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new DriverQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<DriverBO> entityPageBO = driverService.selectByPage(entityQuery);
            Page<DriverVO> entityPageVO = driverBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
