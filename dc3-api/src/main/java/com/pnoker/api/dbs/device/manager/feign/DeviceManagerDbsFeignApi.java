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

package com.pnoker.device.manager.feign;

import com.pnoker.device.manager.hystrix.DeviceManagerDbsFeignApiHystrix;
import com.pnoker.api.security.BaseAuthConfigurer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@FeignClient(name = "DC3-DEVICE-MANAGER", fallbackFactory = DeviceManagerDbsFeignApiHystrix.class, configuration = BaseAuthConfigurer.class)
@RequestMapping("/api/v3/device/manager")
public interface DeviceManagerDbsFeignApi {

}
