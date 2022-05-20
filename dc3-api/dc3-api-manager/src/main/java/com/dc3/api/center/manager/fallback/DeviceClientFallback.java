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

package com.dc3.api.center.manager.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.DeviceClient;
import com.dc3.common.bean.R;
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DeviceClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DeviceClientFallback implements FallbackFactory<DeviceClient> {

    @Override
    public DeviceClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-MANAGER" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new DeviceClient() {

            @Override
            public R<Device> add(Device device, Long tenantId) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(Long id) {
                return R.fail(message);
            }

            @Override
            public R<Device> update(Device device, Long tenantId) {
                return R.fail(message);
            }

            @Override
            public R<Device> selectById(Long id) {
                return R.fail(message);
            }

            @Override
            public R<List<Device>> selectByDriverId(Long driverId) {
                return R.fail(message);
            }

            @Override
            public R<List<Device>> selectByProfileId(Long profileId) {
                return R.fail(message);
            }

            @Override
            public R<Page<Device>> list(DeviceDto deviceDto, Long tenantId) {
                return R.fail(message);
            }

        };
    }
}