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
import com.pnoker.api.device.manager.hystrix.DeviceClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DeviceDto;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Group;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>设备 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_DEVICE_URL_PREFIX, name = Common.Service.DC3_MANAGER, fallbackFactory = DeviceClientHystrix.class)
public interface DeviceClient {

    /**
     * 新增 Device 记录
     *
     * @param device
     * @return Device
     */
    @PostMapping("/add")
    R<Device> add(@Validated(Insert.class) @RequestBody Device device);

    /**
     * 根据 ID 删除 Device
     *
     * @param id deviceId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Device 记录
     *
     * @param device
     * @return Device
     */
    @PostMapping("/update")
    R<Device> update(@Validated(Update.class) @RequestBody Device device);

    /**
     * 根据 ID 查询 Device
     *
     * @param id
     * @return Device
     */
    @GetMapping("/id/{id}")
    R<Device> selectById(@PathVariable(value = "id") Long id);

    /**
     * 根据 CODE 查询 Device
     *
     * @param code
     * @return Device
     */
    @GetMapping("/code/{code}")
    R<Device> selectByCode(@PathVariable(value = "code") String code);

    /**
     * 根据 GroupId & Name 查询 Device
     *
     * @param groupId
     * @param name
     * @return Device
     */
    @GetMapping("/group/{groupId}/name/{name}")
    R<Device> selectDeviceByNameAndGroup(@PathVariable(value = "groupId") Long groupId, @PathVariable(value = "name") String name);

    /**
     * 分页查询 Device
     *
     * @param deviceDto
     * @return Page<Device>
     */
    @PostMapping("/list")
    R<Page<Device>> list(@RequestBody(required = false) DeviceDto deviceDto);

    /**
     * 查询 Device 字典
     *
     * @return List<Device>
     */
    @GetMapping("/dictionary")
    R<List<Dic>> dictionary();

}
