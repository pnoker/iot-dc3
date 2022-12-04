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

package io.github.pnoker.api.center.data.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.data.fallback.PointValueClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.DataServiceConstant;
import io.github.pnoker.common.dto.PointValueDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 数据 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Validated
@FeignClient(path = DataServiceConstant.VALUE_URL_PREFIX, name = DataServiceConstant.SERVICE_NAME, fallbackFactory = PointValueClientFallback.class)
public interface PointValueClient {

    /**
     * 查询最新 PointValue 集合
     *
     * @param pointValueDto 位号值和分页参数
     * @param tenantId      租户ID
     * @return 带分页的 {@link io.github.pnoker.common.bean.point.PointValue}
     */
    @PostMapping("/latest")
    R<Page<PointValue>> latest(@RequestBody PointValueDto pointValueDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 模糊分页查询 PointValue
     *
     * @param pointValueDto 位号值和分页参数
     * @param tenantId      租户ID
     * @return 带分页的 {@link io.github.pnoker.common.bean.point.PointValue}
     */
    @PostMapping("/list")
    R<Page<PointValue>> list(@RequestBody(required = false) PointValueDto pointValueDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);
}
