/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.fallback.DriverAttributeClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.DriverAttributeDto;
import io.github.pnoker.common.model.DriverAttribute;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 驱动配置属性 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Manager.DRIVER_ATTRIBUTE_URL_PREFIX, name = ServiceConstant.Manager.SERVICE_NAME, fallbackFactory = DriverAttributeClientFallback.class)
public interface DriverAttributeClient {

    /**
     * 新增 DriverAttribute
     *
     * @param driverAttribute DriverAttribute
     * @return DriverAttribute
     */
    @PostMapping("/add")
    R<DriverAttribute> add(@Validated(Insert.class) @RequestBody DriverAttribute driverAttribute);

    /**
     * 根据 ID 删除 DriverAttribute
     *
     * @param id DriverAttributeId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 DriverAttribute
     *
     * @param driverAttribute DriverAttribute
     * @return DriverAttribute
     */
    @PostMapping("/update")
    R<DriverAttribute> update(@Validated(Update.class) @RequestBody DriverAttribute driverAttribute);

    /**
     * 根据 ID 查询 DriverAttribute
     *
     * @param id DriverAttribute Id
     * @return DriverAttribute
     */
    @GetMapping("/id/{id}")
    R<DriverAttribute> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 驱动ID 查询 DriverAttribute
     *
     * @param id DriverAttribute Id
     * @return DriverAttribute
     */
    @GetMapping("/driver_id/{id}")
    R<List<DriverAttribute>> selectByDriverId(@NotNull @PathVariable(value = "id") String id);

    /**
     * 分页查询 DriverAttribute
     *
     * @param driverAttributeDto DriverAttribute Dto
     * @return Page<DriverAttribute>
     */
    @PostMapping("/list")
    R<Page<DriverAttribute>> list(@RequestBody(required = false) DriverAttributeDto driverAttributeDto);

}
