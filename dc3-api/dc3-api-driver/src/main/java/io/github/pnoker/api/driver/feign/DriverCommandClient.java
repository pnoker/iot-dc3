/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.driver.feign;

import io.github.pnoker.api.driver.fallback.DriverCommandFallback;
import io.github.pnoker.common.constant.service.DriverServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.ValidatableList;
import io.github.pnoker.common.entity.driver.CmdParameter;
import io.github.pnoker.common.entity.point.PointValue;
import io.github.pnoker.common.valid.Read;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * DriverCommand FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = DriverServiceConstant.COMMAND_URL_PREFIX, fallbackFactory = DriverCommandFallback.class)
public interface DriverCommandClient {

    /**
     * 读取位号值
     * 最多读取 {@link io.github.pnoker.common.constant.common.RequestConstant#DEFAULT_MAX_REQUEST_SIZE RequestConstant.DEFAULT_MAX_REQUEST_SIZE} 个
     *
     * @param cmdParameters 读取指令列表
     * @return 位号值列表
     */
    @PostMapping("/read")
    R<List<PointValue>> read(@Validated(Read.class) @RequestBody ValidatableList<CmdParameter> cmdParameters);

    /**
     * 写取位号值
     * 最多写入 {@link io.github.pnoker.common.constant.common.RequestConstant#DEFAULT_MAX_REQUEST_SIZE RequestConstant.DEFAULT_MAX_REQUEST_SIZE} 个
     *
     * @param cmdParameters 写入指令列表
     * @return 是否写入
     */
    @PostMapping("/write")
    R<Boolean> write(@Validated(Read.class) @RequestBody ValidatableList<CmdParameter> cmdParameters);

}
