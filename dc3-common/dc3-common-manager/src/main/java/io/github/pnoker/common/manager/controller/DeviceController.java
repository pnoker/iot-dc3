/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * Controller
 *
 * @author pnoker
 * @version 2025.9.0
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
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DeviceVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            deviceService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            deviceService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    /**
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DeviceVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            deviceService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID Device
     *
     * @param id ID
     * @return DeviceVO {@link DeviceVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<DeviceVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            DeviceBO entityBO = deviceService.selectById(id);
            DeviceVO entityVO = deviceBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * ID Device
     *
     * @param deviceIds Device ID
     * @return Map(ID, DeviceVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, DeviceVO>>> selectByIds(@RequestBody List<Long> deviceIds) {
        return async(() -> {
            List<DeviceBO> entityBOList = deviceService.selectByIds(deviceIds);
            Map<Long, DeviceVO> deviceMap = entityBOList.stream()
                    .collect(Collectors.toMap(DeviceBO::getId, entityBO -> deviceBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        });
    }

    /**
     * Device
     *
     * @param entityQuery
     * @return R Of DeviceVO Page
     */
    @PostMapping("/list")
    public Mono<R<Page<DeviceVO>>> list(@RequestBody(required = false) DeviceQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceQuery query = Objects.isNull(entityQuery) ? new DeviceQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DeviceBO> entityPageBO = deviceService.selectByPage(query);
            Page<DeviceVO> entityPageVO = deviceBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Device
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/import")
    public Mono<R<String>> importDevice(@Validated(Upload.class) DeviceVO entityVO,
                                        @RequestPart("file") Mono<FilePart> filePart) {
        return getTenantId().flatMap(tenantId -> {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            return filePart.flatMap(part -> {
                String filePath = FileUtil.getTempPath() + FileUtil.getRandomXlsxName();
                File file = new File(filePath);
                return part.transferTo(file).then(Mono.defer(() -> async(() -> {
                    deviceService.importDevice(entityBO, file);
                    return R.ok();
                })));
            });
        });
    }

    /**
     * @param entityVO {@link DeviceVO}
     * @return
     */
    @PostMapping("/export/import_template")
    public ResponseEntity<Resource> importTemplate(@Validated(Upload.class) @RequestBody DeviceVO entityVO) {
        DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
        Path filePath = deviceService.generateImportTemplate(entityBO);
        return ResponseUtil.responseFile(filePath);
    }

    /**
     * @param driverId
     * @return
     */
    @GetMapping("/getDeviceByDriverId/{driverId}")
    public Mono<R<String>> getDeviceByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        return async(() -> {
            List<DeviceBO> deviceBOList = deviceService.selectByDriverId(driverId);
            return R.ok(String.valueOf(deviceBOList.size()));
        });
    }

}
