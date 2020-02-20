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

package com.pnoker.common.sdk.api;

import com.pnoker.common.constant.Common;
import com.pnoker.common.sdk.message.DriverSender;
import com.pnoker.common.sdk.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DRIVER_URL_PREFIX)
public class DriverSdkApi {
    @Resource
    private DriverSender driverSender;
    @Resource
    private DriverService driverService;


    @GetMapping("/msg")
    public void msg() {
        driverSender.driverSender("hello 123");
    }

    @GetMapping("/device/{deviceId}/point/{pointId}")
    public void readPoint(@PathVariable("deviceId") Long deviceId, @PathVariable("pointId") Long pointId) {
        driverService.read(deviceId, pointId);
    }
}
