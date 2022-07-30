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
import io.github.pnoker.api.center.manager.fallback.EventClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.DeviceEventDto;
import io.github.pnoker.common.dto.DriverEventDto;
import io.github.pnoker.common.model.DeviceEvent;
import io.github.pnoker.common.model.DriverEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 数据 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Manager.EVENT_URL_PREFIX, name = ServiceConstant.Manager.SERVICE_NAME, fallbackFactory = EventClientFallback.class)
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
