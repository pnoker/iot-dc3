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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.entity.vo.DeviceVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.utils.FileUtil;
import io.github.pnoker.common.utils.ResponseUtil;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.common.valid.Upload;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DEVICE_URL_PREFIX)
public class DeviceController implements BaseController {

    private final DeviceBuilder deviceBuilder;
    private final DeviceService deviceService;

    public DeviceController(DeviceBuilder deviceBuilder, DeviceService deviceService) {
        this.deviceBuilder = deviceBuilder;
        this.deviceService = deviceService;
    }

    /**
     * 新增设备
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DeviceVO entityVO) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            deviceService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除设备
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            deviceService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新设备
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DeviceVO entityVO) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            deviceService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 Device
     *
     * @param id ID
     * @return DeviceVO {@link DeviceVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<DeviceVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            DeviceBO entityBO = deviceService.selectById(id);
            DeviceVO entityVO = deviceBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 集合查询 Device
     *
     * @param deviceIds 设备ID集
     * @return Map(ID, DeviceVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, DeviceVO>>> selectByIds(@RequestBody List<Long> deviceIds) {
        try {
            List<DeviceBO> entityBOList = deviceService.selectByIds(deviceIds);
            Map<Long, DeviceVO> deviceMap = entityBOList.stream().collect(Collectors.toMap(DeviceBO::getId, entityBO -> deviceBuilder.buildVOByBO(entityBO)));
            return Mono.just(R.ok(deviceMap));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 Device
     *
     * @param entityQuery 设备和分页参数
     * @return R Of DeviceVO Page
     */
    @PostMapping("/list")
    public Mono<R<Page<DeviceVO>>> list(@RequestBody(required = false) DeviceQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new DeviceQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<DeviceBO> entityPageBO = deviceService.selectByPage(entityQuery);
            Page<DeviceVO> entityPageVO = deviceBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 导入 Device
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/import")
    public Mono<R<String>> importDevice(@Validated(Upload.class) DeviceVO entityVO, @RequestPart("file") Mono<FilePart> filePart) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            return filePart.flatMap(part -> {
                String filePath = FileUtil.getTempPath() + FileUtil.getRandomXlsxName();
                File file = new File(filePath);
                return part.transferTo(file).then(Mono.defer(() -> {
                    deviceService.importDevice(entityBO, file);
                    return Mono.just(R.ok());
                }));
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 下载导入模板
     *
     * @param entityVO {@link DeviceVO}
     * @return 模板文件流
     */
    @PostMapping("/export/import_template")
    public ResponseEntity<Resource> importTemplate(@Validated(Upload.class) @RequestBody DeviceVO entityVO) {
        DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
        Path filePath = deviceService.generateImportTemplate(entityBO);
        return ResponseUtil.responseFile(filePath);
    }

    /**
     * 驱动下设备数量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDeviceByDriverId/{driverId}")
    public Mono<R<String>> getDeviceByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            List<DeviceBO> deviceBOList = deviceService.selectByDriverId(driverId);
            return Mono.just(R.ok(String.valueOf(deviceBOList.size())));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
