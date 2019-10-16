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

package com.pnoker.api.dbs.device.manager.feign;

import com.pnoker.api.dbs.device.manager.hystrix.DeviceManagerDbsFeignApiHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@FeignClient(name = "DC3-DEVICE-MANAGER", fallbackFactory = DeviceManagerDbsFeignApiHystrix.class)
@RequestMapping("/api/v3/device/manager")
public interface DeviceManagerDbsFeignApi {

}
