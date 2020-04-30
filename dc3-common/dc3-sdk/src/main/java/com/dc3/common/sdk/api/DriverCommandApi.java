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

package com.dc3.common.sdk.api;

import com.dc3.common.sdk.bean.CmdParameter;
import com.dc3.common.sdk.service.DriverCommandService;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.constant.Common;
import com.dc3.common.valid.Read;
import com.dc3.common.valid.ValidatableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

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

    private static final int MAX_REQUEST_SIZE = 100;

    /**
     * 读
     *
     * @param cmdParameters
     */
    @PostMapping("/read")
    public R<List<PointValue>> readPoint(@Validated(Read.class) @RequestBody ValidatableList<CmdParameter> cmdParameters) {
        List<PointValue> pointValues = new ArrayList<>();
        try {
            if (cmdParameters.size() > MAX_REQUEST_SIZE) {
                return R.fail("point request size are limited to " + MAX_REQUEST_SIZE);
            }
            for (CmdParameter cmdParameter : cmdParameters) {
                PointValue pointValue = driverCommandService.read(cmdParameter.getDeviceId(), cmdParameter.getPointId());
                if (null != pointValue) {
                    pointValues.add(pointValue);
                }
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.ok(pointValues);
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
