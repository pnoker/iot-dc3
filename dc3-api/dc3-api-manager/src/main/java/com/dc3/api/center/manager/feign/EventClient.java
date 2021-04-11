/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.hystrix.EventClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DeviceEventDto;
import com.dc3.common.dto.DriverEventDto;
import com.dc3.common.model.DeviceEvent;
import com.dc3.common.model.DriverEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 数据 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_EVENT_URL_PREFIX, name = Common.Service.DC3_DATA_SERVICE_NAME, fallbackFactory = EventClientHystrix.class)
public interface EventClient {

    /**
     * 分页查询 DriverEvent
     *
     * @param driverEventDto DriverEventDto
     * @return Page<DriverEvent>
     */
    @PostMapping("/driver")
    R<Page<DriverEvent>> driverEvent(@RequestBody(required = false) DriverEventDto driverEventDto);

    /**
     * 分页查询 DeviceEvent
     *
     * @param deviceEventDto DeviceEventDto
     * @return Page<DeviceEvent>
     */
    @PostMapping("/device")
    R<Page<DeviceEvent>> deviceEvent(@RequestBody(required = false) DeviceEventDto deviceEventDto);
}
