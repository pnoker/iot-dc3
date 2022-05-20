/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
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

package com.dc3.api.transfer.rtmp.feign;

import com.dc3.api.transfer.rtmp.fallback.DriverCommandFallback;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.command.CmdParameter;
import com.dc3.common.bean.point.PointValue;
import com.dc3.common.constant.ServiceConstant;
import com.dc3.common.valid.Read;
import com.dc3.common.valid.ValidatableList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * DriverCommand FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Driver.COMMAND_URL_PREFIX, fallbackFactory = DriverCommandFallback.class)
public interface DriverCommandClient {

    /**
     * 读
     *
     * @param cmdParameters list<{deviceId,pointId}>
     * @return R<List < PointValue>>
     */
    @PostMapping("/read")
    public R<List<PointValue>> readPoint(@Validated(Read.class) @RequestBody ValidatableList<CmdParameter> cmdParameters);

    /**
     * 写
     *
     * @param cmdParameters list<{deviceId,pointId,stringValue}>
     * @return R<Boolean>
     */
    @PostMapping("/write")
    public R<Boolean> writePoint(@Validated(Read.class) @RequestBody ValidatableList<CmdParameter> cmdParameters);

}
