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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.entity.vo.DeviceVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * REST controller exposing device management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DEVICE_URL_PREFIX)
@RequiredArgsConstructor
public class DeviceController implements BaseController {

    private static final long MAX_IMPORT_BYTES = 20 * 1024 * 1024;

    private final DeviceBuilder deviceBuilder;

    private final DeviceService deviceService;

    private final DriverService driverService;

    /**
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DeviceVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            deviceService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(id));
            deviceService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
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
            requireTenant(tenantId, deviceService.getById(entityBO.getId()));
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
    @GetMapping("/get_by_id")
    public Mono<R<DeviceVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceBO entityBO = requireTenant(tenantId, deviceService.getById(id));
            DeviceVO entityVO = deviceBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * ID Device
     *
     * @param deviceIds Device ID
     * @return Map(ID, DeviceVO)
     */
    @PostMapping("/list_by_ids")
    public Mono<R<Map<Long, DeviceVO>>> listByIds(@RequestBody List<Long> deviceIds) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<DeviceBO> entityBOList = filterTenant(tenantId, deviceService.listByIds(deviceIds));
            Map<Long, DeviceVO> deviceMap = entityBOList.stream()
                    .collect(Collectors.toMap(DeviceBO::getId, entityBO -> deviceBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        }));
    }

    /**
     * Profile ID Device
     *
     * @param profileId Profile ID
     * @return Device array
     */
    @GetMapping("/list_by_profile_id")
    public Mono<R<List<DeviceVO>>> listByProfileId(@NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<DeviceBO> entityBOList = filterTenant(tenantId, deviceService.listByProfileId(profileId));
            List<DeviceVO> entityVOList = deviceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
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
            Page<DeviceBO> entityPageBO = deviceService.list(query);
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
                assertXlsxFile(part);
                assertImportContentLength(part);
                Path filePath = FileUtil.getTempUploadFilePath(FileUtil.getRandomXlsxName(), "manager", "device",
                        "import");
                File file = filePath.toFile();
                return part.transferTo(file).then(Mono.defer(() -> async(() -> {
                    try {
                        assertImportFileSize(filePath);
                        deviceService.importDevice(entityBO, file);
                        return R.<String>ok();
                    } finally {
                        deleteTempFile(filePath);
                    }
                }))).doOnError(error -> deleteTempFile(filePath));
            });
        });
    }

    /**
     * @param entityVO {@link DeviceVO}
     * @return
     */
    @PostMapping("/export/import_template")
    public Mono<ResponseEntity<Resource>> importTemplate(@Validated(Upload.class) @RequestBody DeviceVO entityVO) {
        return getTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            Path filePath = deviceService.generateImportTemplate(entityBO);
            return ResponseUtil.responseFile(filePath);
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * @param driverId
     * @return
     */
    @GetMapping("/get_count_by_driver_id")
    public Mono<R<String>> getCountByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, driverService.getById(driverId));
            List<DeviceBO> deviceBOList = filterTenant(tenantId, deviceService.listByDriverId(driverId));
            return R.ok(String.valueOf(deviceBOList.size()));
        }));
    }

    private void assertXlsxFile(FilePart part) {
        String fileName = part.filename();
        if (Objects.isNull(fileName) || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new RequestException("Only XLSX files can be imported");
        }
    }

    private void assertImportContentLength(FilePart part) {
        long contentLength = part.headers().getContentLength();
        if (contentLength > MAX_IMPORT_BYTES) {
            throw new RequestException("Import file size exceeds 20 MB");
        }
    }

    private void assertImportFileSize(Path filePath) {
        try {
            if (Files.size(filePath) > MAX_IMPORT_BYTES) {
                throw new RequestException("Import file size exceeds 20 MB");
            }
        } catch (java.io.IOException e) {
            throw new RequestException("Import file read failed");
        }
    }

    private void deleteTempFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (java.io.IOException e) {
            log.warn("Failed to delete temporary import file: {}", filePath, e);
        }
    }

}
