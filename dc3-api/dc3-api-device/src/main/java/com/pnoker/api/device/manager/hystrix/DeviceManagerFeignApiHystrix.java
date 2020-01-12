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

package com.pnoker.api.device.manager.hystrix;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pnoker.api.device.manager.feign.DeviceManagerDbsFeignClient;
import com.pnoker.common.dto.DeviceDto;
import com.pnoker.common.model.Device;
import com.pnoker.common.bean.R;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DeviceManagerFeignApiHystrix implements FallbackFactory<DeviceManagerDbsFeignClient> {

    @Override
    public DeviceManagerDbsFeignClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-DBS" : throwable.getMessage();
        log.error("DeviceFeignApi失败:{},hystrix服务降级处理", message, throwable);

        return new DeviceManagerDbsFeignClient() {
            @Override
            public R add(Device rtmp) {
                return R.fail(message);
            }

            @Override
            public R delete(Long id) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> update(Device rtmp) {
                return R.fail(message);
            }

            @Override
            public R<Device> selectById(Long id) {
                return R.fail(message);
            }

            @Override
            public R<IPage<Device>> list(DeviceDto rtmpDto) {
                return R.fail(message);
            }
        };
    }
}