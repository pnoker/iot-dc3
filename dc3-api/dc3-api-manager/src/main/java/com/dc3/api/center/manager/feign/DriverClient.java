/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.hystrix.DriverClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.model.Driver;
import com.dc3.common.valid.Insert;
import com.dc3.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * <p>驱动 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_DRIVER_URL_PREFIX, name = Common.Service.DC3_MANAGER_SERVICE_NAME, fallbackFactory = DriverClientHystrix.class)
public interface DriverClient {

    /**
     * 新增 Driver
     *
     * @param driver Driver
     * @return Driver
     */
    @PostMapping("/add")
    R<Driver> add(@Validated(Insert.class) @RequestBody Driver driver);

    /**
     * 根据 ID 删除 Driver
     *
     * @param id Driver Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 修改 Driver
     *
     * @param driver Driver
     * @return Driver
     */
    @PostMapping("/update")
    R<Driver> update(@Validated(Update.class) @RequestBody Driver driver);

    /**
     * 根据 ID 查询 Driver
     *
     * @param id Driver Id
     * @return Driver
     */
    @GetMapping("/id/{id}")
    R<Driver> selectById(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 根据 SERVICENAME 查询 Driver
     *
     * @param serviceName Driver Service Name
     * @return Driver
     */
    @GetMapping("/service/{serviceName}")
    R<Driver> selectByServiceName(@NotNull @PathVariable(value = "serviceName") String serviceName);

    /**
     * 根据 HOST & PORT 查询 Driver
     *
     * @param host Driver Host
     * @param port Driver Port
     * @return Driver
     */
    @GetMapping("/host/{host}/port/{port}")
    R<Driver> selectByHostPort(@NotNull @PathVariable(value = "host") String host, @NotNull @PathVariable(value = "port") Integer port);

    /**
     * 查询 Driver 服务状态
     * ONLINE, OFFLINE
     *
     * @param driverDto Driver Dto
     * @return Map<String, Boolean>
     */
    @PostMapping("/status")
    R<Map<String, Boolean>> driverStatus(@RequestBody(required = false) DriverDto driverDto);

    /**
     * 分页查询 Driver
     *
     * @param driverDto Driver Dto
     * @return Page<Driver>
     */
    @PostMapping("/list")
    R<Page<Driver>> list(@RequestBody(required = false) DriverDto driverDto);

}
