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

package io.github.pnoker.api.center.manager.feign;

import io.github.pnoker.api.center.manager.fallback.AutoClientFallback;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.point.PointDetail;
import io.github.pnoker.common.valid.Insert;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 自发现 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = ManagerServiceConstant.AUTO_URL_PREFIX, name = ManagerServiceConstant.SERVICE_NAME, fallbackFactory = AutoClientFallback.class)
public interface AutoClient {

    @PostMapping("/create_device_point")
    R<PointDetail> autoCreateDeviceAndPoint(@Validated(Insert.class) @RequestBody PointDetail pointDetail, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

}
