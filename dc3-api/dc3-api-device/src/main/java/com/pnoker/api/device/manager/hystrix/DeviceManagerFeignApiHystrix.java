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
import com.pnoker.api.device.manager.feign.DeviceManagerDbsFeignApi;
import com.pnoker.common.base.dto.device.DeviceDto;
import com.pnoker.common.base.model.device.Device;
import com.pnoker.common.base.bean.Response;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Component
public class DeviceManagerFeignApiHystrix implements FallbackFactory<DeviceManagerDbsFeignApi> {

    @Override
    public DeviceManagerDbsFeignApi create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-DBS" : throwable.getMessage();
        log.error("DeviceFeignApi失败:{},hystrix服务降级处理", message, throwable);

        return new DeviceManagerDbsFeignApi() {
            @Override
            public Response add(Device rtmp) {
                return Response.fail(message);
            }

            @Override
            public Response delete(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<Boolean> update(Device rtmp) {
                return Response.fail(message);
            }

            @Override
            public Response<Device> selectById(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<IPage<Device>> list(DeviceDto rtmpDto) {
                return Response.fail(message);
            }
        };
    }
}