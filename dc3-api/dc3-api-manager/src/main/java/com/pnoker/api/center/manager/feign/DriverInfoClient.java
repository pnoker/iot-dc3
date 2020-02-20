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

package com.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.hystrix.DriverInfoClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DriverInfoDto;
import com.pnoker.common.model.DriverInfo;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>驱动配置信息 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_POINT_INFO_URL_PREFIX, name = Common.Service.DC3_MANAGER, fallbackFactory = DriverInfoClientHystrix.class)
public interface DriverInfoClient {

    /**
     * 新增 DriverInfo
     *
     * @param driverInfo
     * @return DriverInfo
     */
    @PostMapping("/add")
    R<DriverInfo> add(@Validated(Insert.class) @RequestBody DriverInfo driverInfo);

    /**
     * 根据 Id 删除 DriverInfo
     *
     * @param id driverInfoId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 DriverInfo
     *
     * @param driverInfo
     * @return DriverInfo
     */
    @PostMapping("/update")
    R<DriverInfo> update(@Validated(Update.class) @RequestBody DriverInfo driverInfo);

    /**
     * 根据 Id 查询 DriverInfo
     *
     * @param id
     * @return DriverInfo
     */
    @GetMapping("/id/{id}")
    R<DriverInfo> selectById(@PathVariable(value = "id") Long id);

    /**
     * 分页查询 DriverInfo
     *
     * @param driverInfoDto
     * @return Page<DriverInfo>
     */
    @PostMapping("/list")
    R<Page<DriverInfo>> list(@RequestBody(required = false) DriverInfoDto driverInfoDto);

}
