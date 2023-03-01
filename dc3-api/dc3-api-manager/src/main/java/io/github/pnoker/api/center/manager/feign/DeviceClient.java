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

package io.github.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.dto.DeviceDto;
import io.github.pnoker.api.center.manager.fallback.DeviceClientFallback;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

/**
 * 设备 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = ManagerServiceConstant.DEVICE_URL_PREFIX, name = ManagerServiceConstant.SERVICE_NAME, fallbackFactory = DeviceClientFallback.class)
public interface DeviceClient {

    /**
     * 新增 Device
     *
     * @param device   Device
     * @param tenantId 租户ID
     * @return Device
     */
    @PostMapping("/add")
    R<Device> add(@Validated(Insert.class) @RequestBody Device device, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 删除 Device
     *
     * @param id 设备ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 Device
     *
     * @param device   Device
     * @param tenantId 租户ID
     * @return Device
     */
    @PostMapping("/update")
    R<Device> update(@Validated(Update.class) @RequestBody Device device, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 查询 Device
     *
     * @param id 设备ID
     * @return Device
     */
    @GetMapping("/id/{id}")
    R<Device> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 ID 集合查询 Device
     *
     * @param deviceIds 设备ID Set
     * @return Map String:Device
     */
    @PostMapping("/ids")
    R<Map<String, Device>> selectByIds(@RequestBody Set<String> deviceIds);

    /**
     * 模糊分页查询 Device
     *
     * @param tenantId  租户ID
     * @param deviceDto 设备和分页参数
     * @return Page Of Device
     */
    @PostMapping("/list")
    R<Page<Device>> list(@RequestBody(required = false) DeviceDto deviceDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

}
