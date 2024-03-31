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
import io.github.pnoker.center.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.center.manager.entity.query.DeviceQuery;
import io.github.pnoker.center.manager.entity.vo.DeviceVO;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.utils.ResponseUtil;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.stream.Collectors;

/**
 * 设备 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-设备")
@RequestMapping(ManagerConstant.DEVICE_URL_PREFIX)
public class DeviceController implements BaseController {

    @Resource
    private DeviceBuilder deviceBuilder;

    @Resource
    private DeviceService deviceService;

    /**
     * 新增设备
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-设备")
    public R<String> add(@Validated(Add.class) @RequestBody DeviceVO entityVO) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            deviceService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除设备
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            deviceService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新设备
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody DeviceVO entityVO) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            deviceService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Device
     *
     * @param id ID
     * @return DeviceVO {@link DeviceVO}
     */
    @GetMapping("/id/{id}")
    public R<DeviceVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            DeviceBO entityBO = deviceService.selectById(id);
            DeviceVO entityVO = deviceBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 集合查询 Device
     *
     * @param deviceIds 设备ID集
     * @return Map(ID, DeviceVO)
     */
    @PostMapping("/ids")
    public R<Map<Long, DeviceVO>> selectByIds(@RequestBody Set<Long> deviceIds) {
        try {
            List<DeviceBO> entityBOS = deviceService.selectByIds(deviceIds);
            Map<Long, DeviceVO> deviceMap = entityBOS.stream().collect(Collectors.toMap(DeviceBO::getId, entityBO -> deviceBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 Device
     *
     * @param entityQuery 设备和分页参数
     * @return R Of DeviceVO Page
     */
    @PostMapping("/list")
    public R<Page<DeviceVO>> list(@RequestBody(required = false) DeviceQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new DeviceQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<DeviceBO> entityPageBO = deviceService.selectByPage(entityQuery);
            Page<DeviceVO> entityPageVO = deviceBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 导入 Device
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/import")
    public R<String> importDevice(@Validated(Update.class) DeviceVO entityVO, @RequestParam("file") MultipartFile multipartFile) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            deviceService.importDevice(entityBO, multipartFile);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 下载导入模板
     *
     * @param entityVO {@link DeviceVO}
     * @return 模板文件流
     */
    @PostMapping("/export/import_template")
    public ResponseEntity<org.springframework.core.io.Resource> importTemplate(@Validated(Update.class) @RequestBody DeviceVO entityVO) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            Path filePath = deviceService.generateImportTemplate(entityBO);
            return ResponseUtil.responseFile(filePath);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 驱动下设备数量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDeviceByDriverId/{driverId}")
    public R<String> getDeviceByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            List<DeviceBO> deviceBOS = deviceService.selectByDriverId(driverId);
            return R.ok(String.valueOf(deviceBOS.size()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
