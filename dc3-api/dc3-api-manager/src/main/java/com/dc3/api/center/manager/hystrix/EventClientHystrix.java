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

package com.dc3.api.center.manager.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.EventClient;
import com.dc3.common.bean.R;
import com.dc3.common.dto.DeviceEventDto;
import com.dc3.common.dto.DriverEventDto;
import com.dc3.common.model.DeviceEvent;
import com.dc3.common.model.DriverEvent;
import org.springframework.cloud.openfeign.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DeviceEventClientHystrix
 *
 * @author pnoker
 */
@Slf4j
@Component
public class EventClientHystrix implements FallbackFactory<EventClient> {

    @Override
    public EventClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-MANAGER" : throwable.getMessage();
        log.error("Hystrix:{}", message);

        return new EventClient() {

            @Override
            public R<Page<DriverEvent>> driverEvent(DriverEventDto driverEventDto) {
                return R.fail(message);
            }

            @Override
            public R<Page<DeviceEvent>> deviceEvent(DeviceEventDto deviceEventDto) {
                return R.fail(message);
            }

        };
    }
}