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

import com.pnoker.common.bean.R;
import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.constant.Common;
import com.pnoker.common.sdk.service.DriverCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 驱动操作指令 Rest Api
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DRIVER_URL_PREFIX)
public class DriverCommandApi {
    @Resource
    private DriverCommandService driverCommandService;

    /**
     * 读
     *
     * @param deviceId
     * @param pointId
     */
    @GetMapping("/device/{deviceId}/point/{pointId}")
    public R<PointValue> readPoint(@NotNull @PathVariable("deviceId") Long deviceId,
                                   @NotNull @PathVariable("pointId") Long pointId) {
        try {
            PointValue pointValue = driverCommandService.read(deviceId, pointId);
            if (null != pointValue) {
                return R.ok(pointValue);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 写
     *
     * @param deviceId
     * @param pointId
     * @param value
     */
    @PostMapping("/device/{deviceId}/point/{pointId}/value/{value}")
    public Boolean writePoint(@NotNull @PathVariable("deviceId") Long deviceId,
                              @NotNull @PathVariable("pointId") Long pointId,
                              @NotNull @PathVariable("value") String value) {
        return driverCommandService.write(deviceId, pointId, value);
    }
}
