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

package com.dc3.api.center.data.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.data.hystrix.DeviceEventClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.DeviceEvent;
import com.dc3.common.bean.driver.DeviceEventDto;
import com.dc3.common.constant.Common;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * 数据 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_DATA_DEVICE_EVENT_URL_PREFIX, name = Common.Service.DC3_DATA_SERVICE_NAME, fallbackFactory = DeviceEventClientHystrix.class)
public interface DeviceEventClient {

    /**
     * 获取设备状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param deviceId Device Id
     * @return String
     */
    @GetMapping("/status/deviceId/{deviceId}")
    R<String> deviceStatus(@NotNull @PathVariable(value = "deviceId") Long deviceId);

    /**
     * 分页查询 DeviceEvent
     *
     * @param deviceEventDto DeviceEventDto
     * @return Page<DeviceEvent>
     */
    @PostMapping("/list")
    R<Page<DeviceEvent>> list(@RequestBody(required = false) DeviceEventDto deviceEventDto);
}
