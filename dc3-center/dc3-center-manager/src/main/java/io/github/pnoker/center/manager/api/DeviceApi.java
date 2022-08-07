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

package io.github.pnoker.center.manager.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.DeviceClient;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.DeviceDto;
import io.github.pnoker.common.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.DEVICE_URL_PREFIX)
public class DeviceApi implements DeviceClient {

    @Resource
    private DeviceService deviceService;

    @Resource
    private NotifyService notifyService;

    @Override
    public R<Device> add(Device device, String tenantId) {
        try {
            Device add = deviceService.add(device.setTenantId(tenantId));
            if (ObjectUtil.isNotNull(add)) {
                // 通知驱动新增设备
                notifyService.notifyDriverDevice(CommonConstant.Driver.Device.ADD, add);
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> delete(String id) {
        try {
            Device device = deviceService.selectById(id);
            if (null != device && deviceService.delete(id)) {
                // 通知驱动删除设备
                notifyService.notifyDriverDevice(CommonConstant.Driver.Device.DELETE, device);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Device> update(Device device, String tenantId) {
        try {
            Device update = deviceService.update(device.setTenantId(tenantId));
            if (ObjectUtil.isNotNull(update)) {
                // 通知驱动更新设备
                notifyService.notifyDriverDevice(CommonConstant.Driver.Device.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Device> selectById(String id) {
        try {
            Device select = deviceService.selectById(id);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Map<String, Device>> selectByIds(Set<String> deviceIds) {
        try {
            List<Device> devices = deviceService.selectByIds(deviceIds);
            Map<String, Device> deviceMap = devices.stream().collect(Collectors.toMap(Device::getId, Function.identity()));
            return R.ok(deviceMap);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Page<Device>> list(DeviceDto deviceDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(deviceDto)) {
                deviceDto = new DeviceDto();
            }
            deviceDto.setTenantId(tenantId);
            Page<Device> page = deviceService.list(deviceDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
