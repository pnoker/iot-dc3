/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.api.center.manager.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.DriverClient;
import com.dc3.common.bean.R;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.model.Driver;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <p>DriverClientHystrix
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DriverClientHystrix implements FallbackFactory<DriverClient> {

    @Override
    public DriverClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-MANAGER" : throwable.getMessage();
        log.error("Hystrix:{}", message);

        return new DriverClient() {

            @Override
            public R<Driver> add(Driver driver) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(Long id) {
                return R.fail(message);
            }

            @Override
            public R<Driver> update(Driver driver) {
                return R.fail(message);
            }

            @Override
            public R<Driver> selectById(Long id) {
                return R.fail(message);
            }

            @Override
            public R<Driver> selectByServiceName(String serviceName) {
                return R.fail(message);
            }

            @Override
            public R<Driver> selectByHostPort(String host, Integer port) {
                return R.fail(message);
            }

            @Override
            public R<Map<String, Boolean>> driverStatus(DriverDto driverDto) {
                return R.fail(message);
            }

            @Override
            public R<Page<Driver>> list(DriverDto driverDto) {
                return R.fail(message);
            }

        };
    }
}