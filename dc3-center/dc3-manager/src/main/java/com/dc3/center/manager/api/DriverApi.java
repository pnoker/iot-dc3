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

package com.dc3.center.manager.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.DriverClient;
import com.dc3.center.manager.service.DriverService;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.model.Driver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * <p>驱动 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_MANAGER_DRIVER_URL_PREFIX)
public class DriverApi implements DriverClient {
    @Resource
    private DriverService driverService;

    @Override
    public R<Driver> add(Driver driver) {
        try {
            Driver add = driverService.add(driver);
            if (null != add) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> delete(Long id) {
        try {
            return driverService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Driver> update(Driver driver) {
        try {
            Driver update = driverService.update(driver);
            if (null != update) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Driver> selectById(Long id) {
        try {
            Driver select = driverService.selectById(id);
            return R.ok(select);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Driver> selectByServiceName(String serviceName) {
        try {
            Driver select = driverService.selectByServiceName(serviceName);
            return R.ok(select);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Driver> selectByHostPort(String host, Integer port) {
        try {
            Driver select = driverService.selectByHostPort(host, port);
            return R.ok(select);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Map<String, Boolean>> driverStatus(DriverDto driverDto) {
        try {
            Map<String, Boolean> driverStatuses = driverService.driverStatus(driverDto);
            return R.ok(driverStatuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Page<Driver>> list(DriverDto driverDto) {
        try {
            Page<Driver> page = driverService.list(driverDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
