/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
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

package com.dc3.api.center.manager.feign;

import com.dc3.api.center.manager.hystrix.AutoClientHystrix;
import com.dc3.common.bean.R;
import com.dc3.common.bean.point.PointDetail;
import com.dc3.common.constant.Common;
import com.dc3.common.valid.Insert;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 自发现 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_AUTO_URL_PREFIX, name = Common.Service.DC3_MANAGER_SERVICE_NAME, fallbackFactory = AutoClientHystrix.class)
public interface AutoClient {

    @PostMapping("/create_device_point")
    R<PointDetail> autoCreateDeviceAndPoint(@Validated(Insert.class) @RequestBody PointDetail pointDetail, @RequestHeader(value = Common.Service.DC3_AUTH_TENANT_ID, defaultValue = "-1") Long tenantId);

}
