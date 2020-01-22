/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.api.device.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.device.manager.hystrix.DriverClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DriverDto;
import com.pnoker.common.model.Driver;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>驱动 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_GROUP_URL_PREFIX, name = Common.Service.DC3_MANAGER, fallbackFactory = DriverClientHystrix.class)
public interface DriverClient {

    /**
     * 新增 Driver 记录
     *
     * @param driver
     * @return Driver
     */
    @PostMapping("/add")
    R<Driver> add(@Validated(Insert.class) @RequestBody Driver driver);

    /**
     * 根据 ID 删除 Driver
     *
     * @param id driverId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Driver 记录
     *
     * @param driver
     * @return Driver
     */
    @PostMapping("/update")
    R<Driver> update(@Validated(Update.class) @RequestBody Driver driver);

    /**
     * 根据 ID 查询 Driver
     *
     * @param id
     * @return Driver
     */
    @GetMapping("/id/{id}")
    R<Driver> selectById(@PathVariable(value = "id") Long id);

    /**
     * 根据 Name 查询 Driver
     *
     * @param name
     * @return Driver
     */
    @GetMapping("/name/{name}")
    R<Driver> selectByName(@PathVariable(value = "name") String name);

    /**
     * 分页查询 Driver
     *
     * @param driverDto
     * @return Page<Driver>
     */
    @PostMapping("/list")
    R<Page<Driver>> list(@RequestBody(required = false) DriverDto driverDto);

}
