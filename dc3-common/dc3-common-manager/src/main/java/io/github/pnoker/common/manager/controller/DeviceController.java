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
import io.github.pnoker.common.enums.SuccessCode;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "device", description = "Industrial device lifecycle: register, configure, and manage device connectivity including driver assignment, topic binding, and operational status tracking")
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
     * Register a new device for the current tenant, then return the add-success status.
     *
     * @param entityVO device payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('device', 'add')")
    @Operation(summary = "Add Device", description = "Register a new IoT device for the current tenant. " +
            "A device is a physical or virtual data source that collects point values through a driver; returns the device ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DeviceVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            deviceService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a device after verifying it belongs to the current tenant, then return the delete-success status.
     *
     * @param id id of the device to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('device', 'delete')")
    @Operation(summary = "Delete Device", description = "Permanently delete a device by ID (tenant-scoped). " +
            "Removes the device and its point-value configuration while preserving collected history; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(id));
            deviceService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update an existing device after verifying tenant ownership, then return the update-success status.
     *
     * @param entityVO device payload to update
     * @return update-success status
     */
    @PreAuthorize("@perm.can('device', 'update')")
    @Operation(summary = "Update Device", description = "Modify an existing device's attributes such as name, profile, " +
            "driver and connection settings. Tenant ownership is verified before applying the update.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DeviceVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, deviceService.getById(entityBO.getId()));
            deviceService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one device by ID after verifying it belongs to the current tenant.
     *
     * @param id id of the device to fetch
     * @return the matched DeviceVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('device', 'get')")
    @Operation(summary = "Get Device by ID", description = "Fetch one device with its bound profile, driver " +
            "and connection attributes. Use to inspect a device before sending commands or reading its point values.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<DeviceVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceBO entityBO = requireTenant(tenantId, deviceService.getById(id));
            DeviceVO entityVO = deviceBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Resolve a batch of device IDs to their details, filtered to the current tenant.
     *
     * @param deviceIds ids of the devices to resolve
     * @return a map of id to DeviceVO for the tenant-owned matched ids
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(summary = "List Devices by IDs", description = "Resolve a batch of device IDs to their details for the current tenant. " +
            "Returns a map of device ID to device VO; IDs the tenant does not own are filtered out, so treat missing keys as not-found.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
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
     * List every device that instantiates a given profile template, filtered to the current tenant.
     *
     * @param profileId id of the profile template to match
     * @return a list of DeviceVO matching the profile
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(summary = "List Devices by Profile ID", description = "Return every device that instantiates a given profile template (tenant-scoped). " +
            "Use to find which devices share the same point, command and event definitions.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_profile_id")
    public Mono<R<List<DeviceVO>>> listByProfileId(@Parameter(description = "Identifier of the profile template whose instantiated devices are returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<DeviceBO> entityBOList = filterTenant(tenantId, deviceService.listByProfileId(profileId, tenantId));
            List<DeviceVO> entityVOList = deviceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Page through devices for the current tenant using the supplied query filters.
     *
     * @param entityQuery optional query filters (name, profile, driver, enable flag); a new query is used when null
     * @return a page of DeviceVO matching the query
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(summary = "List Devices", description = "Page through devices for the current tenant with filters such as name, " +
            "profile, driver and enable flag. Returns a page of devices; use for browsing or selecting a target device.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
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
     * Bulk-create devices for the current tenant by importing an XLSX file (max 20 MB).
     *
     * @param entityVO device payload carrying the profile and driver context for the import
     * @param filePart uploaded XLSX file whose rows become devices
     * @return add-success status once the import completes
     */
    @PreAuthorize("@perm.can('device', 'add')")
    @Operation(summary = "Import Devices", description = "Bulk-create devices for the current tenant by uploading an XLSX file " +
            "(max 20 MB) shaped by the import template; each row becomes a device under the supplied profile and driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
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
     * Generate and stream the XLSX import template shaped for the supplied profile and driver.
     *
     * @param entityVO device payload carrying the profile and driver context for the template
     * @return a ResponseEntity streaming the generated template XLSX file
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(summary = "Download Device Import Template", description = "Generate and download the XLSX template used for bulk device import, " +
            "pre-shaped for the supplied profile and driver. Fill it in and upload it to the import endpoint.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
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
     * Count how many tenant-owned devices are driven by the given driver.
     *
     * @param driverId id of the driver to count devices for
     * @return the number of devices driven by the driver
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(summary = "Count Devices by Driver", description = "Return how many devices for the current tenant are driven by a given driver. " +
            "Use for quick cardinality checks before reconfiguring a driver; the driver must belong to the tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_count_by_driver_id")
    public Mono<R<Integer>> getCountByDriverId(@Parameter(description = "Identifier of the driver whose device count is returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, driverService.getById(driverId));
            List<DeviceBO> deviceBOList = filterTenant(tenantId, deviceService.listByDriverId(driverId, tenantId));
            return R.ok(deviceBOList.size());
        }));
    }

    /**
     * Reject the upload unless the file name has an {@code .xlsx} extension.
     *
     * @param part the uploaded file part
     */
    private void assertXlsxFile(FilePart part) {
        String fileName = part.filename();
        if (Objects.isNull(fileName) || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new RequestException("Only XLSX files can be imported");
        }
    }

    /**
     * Reject the upload when the declared content length exceeds the import limit.
     *
     * @param part the uploaded file part
     */
    private void assertImportContentLength(FilePart part) {
        long contentLength = part.headers().getContentLength();
        if (contentLength > MAX_IMPORT_BYTES) {
            throw new RequestException("Import file size exceeds 20 MB");
        }
    }

    /**
     * Re-check the actual on-disk file size against the import limit after the upload
     * lands, since the content-length header can be spoofed.
     *
     * @param filePath the landed temp file path
     */
    private void assertImportFileSize(Path filePath) {
        try {
            if (Files.size(filePath) > MAX_IMPORT_BYTES) {
                throw new RequestException("Import file size exceeds 20 MB");
            }
        } catch (java.io.IOException ignored) {
            throw new RequestException("Import file read failed");
        }
    }

    /**
     * Best-effort delete of a temporary import file, logging (not throwing) on failure.
     *
     * @param filePath the temp file path to delete
     */
    private void deleteTempFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (java.io.IOException e) {
            log.warn("Failed to delete temporary import file: {}", filePath, e);
        }
    }

}
