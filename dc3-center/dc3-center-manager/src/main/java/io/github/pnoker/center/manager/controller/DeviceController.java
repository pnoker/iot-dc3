/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DevicePageQuery;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.utils.RequestUtil;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.DEVICE_URL_PREFIX)
public class DeviceController {

    @Resource
    private DeviceService deviceService;

    /**
     * 新增 Device
     *
     * @param device   Device
     * @param tenantId 租户ID
     * @return Device
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody Device device, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            device.setTenantId(tenantId);
            deviceService.add(device);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Device
     *
     * @param id 设备ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            deviceService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 Device
     *
     * @param device   Device
     * @param tenantId 租户ID
     * @return Device
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody Device device, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            device.setTenantId(tenantId);
            deviceService.update(device);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Device
     *
     * @param id 设备ID
     * @return Device
     */
    @GetMapping("/id/{id}")
    public R<Device> selectById(@NotNull @PathVariable(value = "id") String id) {
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

    /**
     * 根据 ID 集合查询 Device
     *
     * @param deviceIds 设备ID Set
     * @return Map String:Device
     */
    @PostMapping("/ids")
    public R<Map<String, Device>> selectByIds(@RequestBody Set<String> deviceIds) {
        try {
            List<Device> devices = deviceService.selectByIds(deviceIds);
            Map<String, Device> deviceMap = devices.stream().collect(Collectors.toMap(Device::getId, Function.identity()));
            return R.ok(deviceMap);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 模糊分页查询 Device
     *
     * @param tenantId        租户ID
     * @param devicePageQuery 设备和分页参数
     * @return Page Of Device
     */
    @PostMapping("/list")
    public R<Page<Device>> list(@RequestBody(required = false) DevicePageQuery devicePageQuery, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(devicePageQuery)) {
                devicePageQuery = new DevicePageQuery();
            }
            devicePageQuery.setTenantId(tenantId);
            Page<Device> page = deviceService.list(devicePageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 导入 Device
     *
     * @param device   Device
     * @param tenantId 租户ID
     * @return Device
     */
    @PostMapping("/import")
    public R<String> importDevice(@Validated(Update.class) Device device, @RequestParam("file") MultipartFile multipartFile, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            device.setTenantId(tenantId);
            deviceService.importDevice(device, multipartFile);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 导入 Device 模板
     *
     * @param device   Device
     * @param tenantId 租户ID
     * @return Device
     */
    @PostMapping("/import/template")
    public ResponseEntity<org.springframework.core.io.Resource> importTemplate(@Validated(Update.class) @RequestBody Device device, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            device.setTenantId(tenantId);
            Path filePath = deviceService.generateImportTemplate(device);
            return RequestUtil.responseFile(filePath);
        } catch (Exception e) {
            return null;
        }
    }

}
