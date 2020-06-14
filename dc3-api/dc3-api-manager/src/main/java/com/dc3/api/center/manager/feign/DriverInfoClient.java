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
import com.dc3.api.center.manager.hystrix.DriverInfoClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverInfoDto;
import com.dc3.common.model.DriverInfo;
import com.dc3.common.valid.Insert;
import com.dc3.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * <p>驱动配置信息 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_DRIVER_INFO_URL_PREFIX, name = Common.Service.DC3_MANAGER_SERVICE_NAME, fallbackFactory = DriverInfoClientHystrix.class)
public interface DriverInfoClient {

    /**
     * 新增 DriverInfo
     *
     * @param driverInfo DriverInfo
     * @return DriverInfo
     */
    @PostMapping("/add")
    R<DriverInfo> add(@Validated(Insert.class) @RequestBody DriverInfo driverInfo);

    /**
     * 根据 ID 删除 DriverInfo
     *
     * @param id DriverInfo Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 修改 DriverInfo
     *
     * @param driverInfo DriverInfo
     * @return DriverInfo
     */
    @PostMapping("/update")
    R<DriverInfo> update(@Validated(Update.class) @RequestBody DriverInfo driverInfo);

    /**
     * 根据 ID 查询 DriverInfo
     *
     * @param id DriverInfo Id
     * @return DriverInfo
     */
    @GetMapping("/id/{id}")
    R<DriverInfo> selectById(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 分页查询 DriverInfo
     *
     * @param driverInfoDto DriverInfo Dto
     * @return Page<DriverInfo>
     */
    @PostMapping("/list")
    R<Page<DriverInfo>> list(@RequestBody(required = false) DriverInfoDto driverInfoDto);

}
