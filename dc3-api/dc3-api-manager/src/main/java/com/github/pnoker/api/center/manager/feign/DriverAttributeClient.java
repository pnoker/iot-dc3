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

package com.github.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pnoker.api.center.manager.hystrix.DriverAttributeClientHystrix;
import com.github.pnoker.common.bean.R;
import com.github.pnoker.common.constant.Common;
import com.github.pnoker.common.dto.DriverAttributeDto;
import com.github.pnoker.common.model.DriverAttribute;
import com.github.pnoker.common.valid.Insert;
import com.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * <p>驱动配置属性 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_DRIVER_ATTRIBUTE_URL_PREFIX, name = Common.Service.DC3_MANAGER, fallbackFactory = DriverAttributeClientHystrix.class)
public interface DriverAttributeClient {

    /**
     * 新增 ConnectInfo
     *
     * @param driverAttribute
     * @return ConnectInfo
     */
    @PostMapping("/add")
    R<DriverAttribute> add(@Validated(Insert.class) @RequestBody DriverAttribute driverAttribute);

    /**
     * 根据 ID 删除 ConnectInfo
     *
     * @param id connectInfoId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 修改 ConnectInfo
     *
     * @param driverAttribute
     * @return ConnectInfo
     */
    @PostMapping("/update")
    R<DriverAttribute> update(@Validated(Update.class) @RequestBody DriverAttribute driverAttribute);

    /**
     * 根据 ID 查询 ConnectInfo
     *
     * @param id
     * @return ConnectInfo
     */
    @GetMapping("/id/{id}")
    R<DriverAttribute> selectById(@NotNull @PathVariable(value = "id") Long id);

    /**
     * 分页查询 ConnectInfo
     *
     * @param connectInfoDto
     * @return Page<ConnectInfo>
     */
    @PostMapping("/list")
    R<Page<DriverAttribute>> list(@RequestBody(required = false) DriverAttributeDto connectInfoDto);

}
