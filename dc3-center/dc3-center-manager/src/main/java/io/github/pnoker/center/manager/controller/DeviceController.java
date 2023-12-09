/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.query.DeviceBOPageQuery;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.utils.RequestUtil;
import io.github.pnoker.common.valid.Add;
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
public class DeviceController implements Controller {

    @Resource
    private DeviceService deviceService;

    /**
     * 新增 Device
     *
     * @param deviceBO Device
     * @return Device
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Add.class) @RequestBody DeviceBO deviceBO) {
        try {
            deviceBO.setTenantId(getTenantId());
            deviceService.save(deviceBO);
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
            deviceService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 Device
     *
     * @param deviceBO Device
     * @return Device
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody DeviceBO deviceBO) {
        try {
            deviceBO.setTenantId(getTenantId());
            deviceService.update(deviceBO);
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
    public R<DeviceBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            DeviceBO select = deviceService.selectById(Long.parseLong(id));
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
    public R<Map<Long, DeviceBO>> selectByIds(@RequestBody Set<String> deviceIds) {
        try {
            Set<Long> collect = deviceIds.stream().map(Long::parseLong).collect(Collectors.toSet());
            List<DeviceBO> deviceBOS = deviceService.selectByIds(collect);
            Map<Long, DeviceBO> deviceMap = deviceBOS.stream().collect(Collectors.toMap(DeviceBO::getId, Function.identity()));
            return R.ok(deviceMap);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 Device
     *
     * @param devicePageQuery 设备和分页参数
     * @return Page Of Device
     */
    @PostMapping("/list")
    public R<Page<DeviceBO>> list(@RequestBody(required = false) DeviceBOPageQuery devicePageQuery) {
        try {
            if (ObjectUtil.isEmpty(devicePageQuery)) {
                devicePageQuery = new DeviceBOPageQuery();
            }
            devicePageQuery.setTenantId(getTenantId());
            Page<DeviceBO> page = deviceService.selectByPage(devicePageQuery);
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
     * @param deviceBO Device
     * @return Device
     */
    @PostMapping("/import")
    public R<String> importDevice(@Validated(Update.class) DeviceBO deviceBO, @RequestParam("file") MultipartFile multipartFile) {
        try {
            deviceBO.setTenantId(getTenantId());
            deviceService.importDevice(deviceBO, multipartFile);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 导入 Device 模板
     *
     * @param deviceBO Device
     * @return Device
     */
    @PostMapping("/import/template")
    public ResponseEntity<org.springframework.core.io.Resource> importTemplate(@Validated(Update.class) @RequestBody DeviceBO deviceBO) {
        try {
            deviceBO.setTenantId(getTenantId());
            Path filePath = deviceService.generateImportTemplate(deviceBO);
            return RequestUtil.responseFile(filePath);
        } catch (Exception e) {
            return null;
        }
    }

}
