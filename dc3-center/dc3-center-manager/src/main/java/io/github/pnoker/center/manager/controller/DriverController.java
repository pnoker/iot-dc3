/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
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
public class DriverController {

    @Resource
    private DriverService driverService;

    /**
     * 新增 Driver
     *
     * @param entityDO Driver
     * @param tenantId 租户ID
     * @return Driver
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody DriverDO entityDO, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            entityDO.setTenantId(tenantId);
            driverService.add(entityDO);
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
            driverService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 Driver
     *
     * @param entityDO Driver
     * @param tenantId 租户ID
     * @return Driver
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody DriverDO entityDO,
                            @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            entityDO.setTenantId(tenantId);
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
            DriverDO select = driverService.selectById(id);
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
    public R<Map<String, DriverDO>> selectByIds(@RequestBody Set<String> driverIds) {
        try {
            List<DriverDO> entityDOS = driverService.selectByIds(driverIds);
            Map<String, DriverDO> driverMap = entityDOS.stream().collect(Collectors.toMap(DriverDO::getId, Function.identity()));
            return R.ok(driverMap);
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
    public R<DriverDO> selectByServiceName(@NotNull @PathVariable(value = "serviceName") String serviceName,
                                           @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            DriverDO select = driverService.selectByServiceName(serviceName, tenantId, true);
            return R.ok(select);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 模糊分页查询 Driver
     *
     * @param driverPageQuery Driver Dto
     * @param tenantId        租户ID
     * @return Page Of Driver
     */
    @PostMapping("/list")
    public R<Page<DriverDO>> list(@RequestBody(required = false) DriverPageQuery driverPageQuery,
                                  @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(driverPageQuery)) {
                driverPageQuery = new DriverPageQuery();
            }
            driverPageQuery.setTenantId(tenantId);
            Page<DriverDO> page = driverService.list(driverPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
