/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.manager.fallback;

import io.github.pnoker.api.center.manager.feign.StatusClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.dto.DeviceDto;
import io.github.pnoker.common.dto.DriverDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DeviceClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class StatusClientFallback implements FallbackFactory<StatusClient> {

    @Override
    public StatusClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-MANAGER" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new StatusClient() {

            @Override
            public R<Map<String, String>> driverStatus(DriverDto driverDto, String tenantId) {
                return R.fail(message);
            }

            @Override
            public R<Map<String, String>> deviceStatus(DeviceDto deviceDto, String tenantId) {
                return R.fail(message);
            }

            @Override
            public R<Map<String, String>> deviceStatusByDriverId(String driverId) {
                return R.fail(message);
            }

            @Override
            public R<Map<String, String>> deviceStatusByProfileId(String profileId) {
                return R.fail(message);
            }
        };
    }
}