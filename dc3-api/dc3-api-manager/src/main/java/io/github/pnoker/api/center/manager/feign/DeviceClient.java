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
import io.github.pnoker.api.center.manager.fallback.DeviceClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.DeviceDto;
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
 */
@FeignClient(path = ServiceConstant.Manager.DEVICE_URL_PREFIX, name = ServiceConstant.Manager.SERVICE_NAME, fallbackFactory = DeviceClientFallback.class)
public interface DeviceClient {

    /**
     * 新增 Device
     *
     * @param device Device
     * @return Device
     */
    @PostMapping("/add")
    R<Device> add(@Validated(Insert.class) @RequestBody Device device, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 删除 Device
     *
     * @param id Device Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 Device
     *
     * @param device Device
     * @return Device
     */
    @PostMapping("/update")
    R<Device> update(@Validated(Update.class) @RequestBody Device device, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 查询 Device
     *
     * @param id Device Id
     * @return Device
     */
    @GetMapping("/id/{id}")
    R<Device> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 ID 集合查询 Device
     *
     * @param deviceIds Device Id Set
     * @return Map<String, Device>
     */
    @PostMapping("/ids")
    R<Map<String, Device>> selectByIds(@RequestBody Set<String> deviceIds);

    /**
     * 分页查询 Device
     *
     * @param deviceDto Device Dto
     * @return Page < Device>
     */
    @PostMapping("/list")
    R<Page<Device>> list(@RequestBody(required = false) DeviceDto deviceDto, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

}
