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

package io.github.pnoker.common.sdk.api;

import io.github.pnoker.api.transfer.rtmp.feign.DriverCommandClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.driver.command.CmdParameter;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.sdk.service.DriverCommandService;
import io.github.pnoker.common.valid.ValidatableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 驱动操作指令 Rest Api
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Driver.COMMAND_URL_PREFIX)
public class DriverCommandApi implements DriverCommandClient {

    @Resource
    private DriverCommandService driverCommandService;

    @Override
    public R<List<PointValue>> readPoint(ValidatableList<CmdParameter> cmdParameters) {
        List<PointValue> pointValues = new ArrayList<>(16);
        try {
            if (cmdParameters.size() > CommonConstant.Driver.DEFAULT_MAX_REQUEST_SIZE) {
                return R.fail("point request size are limited to " + CommonConstant.Driver.DEFAULT_MAX_REQUEST_SIZE);
            }
            cmdParameters.forEach(cmdParameter -> {
                PointValue pointValue = driverCommandService.read(cmdParameter.getDeviceId(), cmdParameter.getPointId());
                Optional.ofNullable(pointValue).ifPresent(pointValues::add);
            });
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.ok(pointValues);
    }

    @Override
    public R<Boolean> writePoint(ValidatableList<CmdParameter> cmdParameters) {
        try {
            if (cmdParameters.size() > CommonConstant.Driver.DEFAULT_MAX_REQUEST_SIZE) {
                return R.fail("point request size are limited to " + CommonConstant.Driver.DEFAULT_MAX_REQUEST_SIZE);
            }
            cmdParameters.forEach(cmdParameter -> driverCommandService.write(cmdParameter.getDeviceId(), cmdParameter.getPointId(), cmdParameter.getValue()));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.ok();
    }
}
