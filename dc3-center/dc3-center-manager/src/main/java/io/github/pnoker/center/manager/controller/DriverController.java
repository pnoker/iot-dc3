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
import io.github.pnoker.center.manager.entity.query.DriverPageQuery;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.DriverDO;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 驱动 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.DRIVER_URL_PREFIX)
public class DriverController implements Controller {

    @Resource
    private DriverService driverService;

    /**
     * 新增 Driver
     *
     * @param entityDO Driver
     * @return Driver
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody DriverDO entityDO) {
        try {
            entityDO.setTenantId(getTenantId());
            driverService.save(entityDO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Driver
     *
     * @param id 驱动ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            driverService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 Driver
     *
     * @param entityDO Driver
     * @return Driver
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody DriverDO entityDO) {
        try {
            entityDO.setTenantId(getTenantId());
            driverService.update(entityDO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Driver
     *
     * @param id 驱动ID
     * @return Driver
     */
    @GetMapping("/id/{id}")
    public R<DriverDO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            DriverDO select = driverService.selectById(Long.parseLong(id));
            return R.ok(select);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 集合查询 Driver
     *
     * @param driverIds Driver ID Set
     * @return Map String:Driver
     */
    @PostMapping("/ids")
    public R<Map<Long, DriverDO>> selectByIds(@RequestBody Set<String> driverIds) {
        try {
            Set<Long> collect = driverIds.stream().map(Long::parseLong).collect(Collectors.toSet());
            List<DriverDO> entityDOS = driverService.selectByIds(collect);
            Map<Long, DriverDO> driverMap = entityDOS.stream().collect(Collectors.toMap(DriverDO::getId, Function.identity()));
            return R.ok(driverMap);
            // todo 返回 long id 前端无法解析
        } catch (Exception e) {
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
    public R<DriverDO> selectByServiceName(@NotNull @PathVariable(value = "serviceName") String serviceName) {
        try {
            DriverDO select = driverService.selectByServiceName(serviceName, getTenantId(), true);
            return R.ok(select);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 Driver
     *
     * @param driverPageQuery Driver Dto
     * @return Page Of Driver
     */
    @PostMapping("/list")
    public R<Page<DriverDO>> list(@RequestBody(required = false) DriverPageQuery driverPageQuery) {
        try {
            if (ObjectUtil.isEmpty(driverPageQuery)) {
                driverPageQuery = new DriverPageQuery();
            }
            driverPageQuery.setTenantId(getTenantId());
            Page<DriverDO> page = driverService.selectByPage(driverPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
