/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.model.Driver;
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
     * @param driver   Driver
     * @param tenantId 租户ID
     * @return Driver
     */
    @PostMapping("/add")
    public R<Driver> add(@Validated(Insert.class) @RequestBody Driver driver,
                         @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            driver.setTenantId(tenantId);
            Driver add = driverService.add(driver);
            if (ObjectUtil.isNotNull(add)) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 删除 Driver
     *
     * @param id 驱动ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            return driverService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 Driver
     *
     * @param driver   Driver
     * @param tenantId 租户ID
     * @return Driver
     */
    @PostMapping("/update")
    public R<Driver> update(@Validated(Update.class) @RequestBody Driver driver,
                            @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            driver.setTenantId(tenantId);
            Driver update = driverService.update(driver);
            if (ObjectUtil.isNotNull(update)) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 查询 Driver
     *
     * @param id 驱动ID
     * @return Driver
     */
    @GetMapping("/id/{id}")
    public R<Driver> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            Driver select = driverService.selectById(id);
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
    public R<Map<String, Driver>> selectByIds(@RequestBody Set<String> driverIds) {
        try {
            List<Driver> drivers = driverService.selectByIds(driverIds);
            Map<String, Driver> driverMap = drivers.stream().collect(Collectors.toMap(Driver::getId, Function.identity()));
            return R.ok(driverMap);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 SERVICENAME 查询 Driver
     *
     * @param serviceName Driver Service Name
     * @return Driver
     */
    @GetMapping("/service/{serviceName}")
    public R<Driver> selectByServiceName(@NotNull @PathVariable(value = "serviceName") String serviceName) {
        try {
            Driver select = driverService.selectByServiceName(serviceName);
            return R.ok(select);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 TYPE 、 HOST 、 PORT 查询 Driver
     *
     * @param type     Driver type
     * @param host     Driver Host
     * @param port     Driver Port
     * @param tenantId 租户ID
     * @return Driver
     */
    @GetMapping("/type/{type}/host/{host}/port/{port}")
    public R<Driver> selectByHostPort(@NotNull @PathVariable(value = "type") String type,
                                      @NotNull @PathVariable(value = "host") String host,
                                      @NotNull @PathVariable(value = "port") Integer port,
                                      @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            DriverTypeFlagEnum typeEnum = DriverTypeFlagEnum.of(type);
            Driver select = driverService.selectByHostPort(typeEnum, host, port, tenantId);
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
    public R<Page<Driver>> list(@RequestBody(required = false) DriverPageQuery driverPageQuery,
                                @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            if (ObjectUtil.isEmpty(driverPageQuery)) {
                driverPageQuery = new DriverPageQuery();
            }
            driverPageQuery.setTenantId(tenantId);
            Page<Driver> page = driverService.list(driverPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
