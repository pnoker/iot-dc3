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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pnoker.api.device.manager.hystrix.DeviceManagerFeignApiHystrix;
import com.pnoker.common.dto.device.DeviceDto;
import com.pnoker.common.model.device.Device;
import com.pnoker.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * <p>设备管理FeignCLient
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@FeignClient(name = "DC3-DEVICE-MANAGER", fallbackFactory = DeviceManagerFeignApiHystrix.class)
@RequestMapping(value = "/api/v3/device/manager")
public interface DeviceManagerDbsFeignApi {

    /**
     * 新增 新增 Device 任务记录
     *
     * @param device
     * @return true/false
     */
    @PostMapping("/add")
    Response<Long> add(Device device);

    /**
     * 删除 根据 ID 删除 Device
     *
     * @param id deviceId
     * @return true/false
     */
    @DeleteMapping("/delete/{id}")
    Response<Boolean> delete(@PathVariable Long id);

    /**
     * 修改 修改 Device 任务记录
     *
     * @param device
     * @return true/false
     */
    @PutMapping("/update")
    Response<Boolean> update(Device device);

    /**
     * 查询 根据ID查询 Device
     *
     * @param id
     * @return device
     */
    @GetMapping("/{id}")
    Response<Device> selectById(@PathVariable Long id);

    /**
     * 分页查询 按照查询 Device
     *
     * @param deviceDto
     * @return deviceList
     */
    @GetMapping("/")
    Response<IPage<Device>> list(DeviceDto deviceDto);
}
