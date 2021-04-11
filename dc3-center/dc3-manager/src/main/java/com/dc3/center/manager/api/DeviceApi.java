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

package com.dc3.center.manager.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.DeviceClient;
import com.dc3.center.manager.service.DeviceService;
import com.dc3.center.manager.service.NotifyService;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>设备 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_MANAGER_DEVICE_URL_PREFIX)
public class DeviceApi implements DeviceClient {
    @Resource
    private DeviceService deviceService;
    @Resource
    private NotifyService notifyService;

    @Override
    public R<Device> add(Device device) {
        try {
            Device add = deviceService.add(device);
            if (null != add) {
                notifyService.notifyDriverDevice(Common.Driver.Device.ADD, add);
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
            Device device = deviceService.selectById(id);
            if (null != device && deviceService.delete(id)) {
                notifyService.notifyDriverDevice(Common.Driver.Device.DELETE, device);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Device> update(Device device) {
        try {
            Device update = deviceService.update(device);
            if (null != update) {
                notifyService.notifyDriverDevice(Common.Driver.Device.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Device> selectById(Long id) {
        try {
            Device select = deviceService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Device>> list(DeviceDto deviceDto) {
        try {
            Page<Device> page = deviceService.list(deviceDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
