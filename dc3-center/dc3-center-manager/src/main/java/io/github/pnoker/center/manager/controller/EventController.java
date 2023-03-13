/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DeviceEventPageQuery;
import io.github.pnoker.center.manager.entity.query.DriverEventPageQuery;
import io.github.pnoker.center.manager.service.EventService;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.DeviceEvent;
import io.github.pnoker.common.entity.DriverEvent;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 事件 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.EVENT_URL_PREFIX)
public class EventController {

    @Resource
    private EventService eventService;

    /**
     * 模糊分页查询 DriverEvent
     *
     * @param driverEventPageQuery DriverEventDto
     * @return Page Of DriverEvent
     */
    @PostMapping("/driver")
    public R<Page<DriverEvent>> driverEvent(@RequestBody(required = false) DriverEventPageQuery driverEventPageQuery) {
        try {
            if (ObjectUtil.isEmpty(driverEventPageQuery)) {
                driverEventPageQuery = new DriverEventPageQuery();
            }
            Page<DriverEvent> page = eventService.driverEvent(driverEventPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 模糊分页查询 DeviceEvent
     *
     * @param deviceEventPageQuery DeviceEventDto
     * @return Page Of DeviceEvent
     */
    @PostMapping("/device")
    public R<Page<DeviceEvent>> deviceEvent(@RequestBody(required = false) DeviceEventPageQuery deviceEventPageQuery) {
        try {
            if (ObjectUtil.isEmpty(deviceEventPageQuery)) {
                deviceEventPageQuery = new DeviceEventPageQuery();
            }
            Page<DeviceEvent> page = eventService.deviceEvent(deviceEventPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}