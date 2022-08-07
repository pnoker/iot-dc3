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

package io.github.pnoker.center.manager.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.EventClient;
import io.github.pnoker.center.manager.service.EventService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.DeviceEventDto;
import io.github.pnoker.common.dto.DriverEventDto;
import io.github.pnoker.common.model.DeviceEvent;
import io.github.pnoker.common.model.DriverEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.EVENT_URL_PREFIX)
public class EventApi implements EventClient {

    @Resource
    private EventService eventService;

    @Override
    public R<Page<DriverEvent>> driverEvent(DriverEventDto driverEventDto) {
        try {
            if (ObjectUtil.isEmpty(driverEventDto)) {
                driverEventDto = new DriverEventDto();
            }
            Page<DriverEvent> page = eventService.driverEvent(driverEventDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<DeviceEvent>> deviceEvent(DeviceEventDto deviceEventDto) {
        try {
            if (ObjectUtil.isEmpty(deviceEventDto)) {
                deviceEventDto = new DeviceEventDto();
            }
            Page<DeviceEvent> page = eventService.deviceEvent(deviceEventDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}