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

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.query.DeviceImportRequest;
import io.github.pnoker.common.manager.entity.query.DeviceImportTemplateRequest;
import io.github.pnoker.common.manager.entity.query.DeviceListRequest;
import io.github.pnoker.common.manager.entity.vo.DeviceVO;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceImportService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing device management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(
        name = "device",
        description =
                "Industrial device lifecycle: register, configure, and manage device connectivity including driver assignment, topic binding, and operational status tracking")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DEVICE_URL_PREFIX)
@RequiredArgsConstructor
public class DeviceController implements BaseController {
    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final long MAX_IMPORT_BYTES = 20 * 1024 * 1024;

    private final DeviceBuilder deviceBuilder;

    private final ReactiveDeviceService reactiveDeviceService;
    private final ReactiveDeviceImportService deviceImportService;

    /**
     * Register a new device for the current tenant, then return the add-success status.
     *
     * @param entityVO device payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('device', 'add')")
    @Operation(
            summary = "Add Device",
            description =
                    "Register a new IoT device for the current tenant. "
                            + "A device is a physical or virtual data source that collects point values through a driver; returns the device ID.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<DeviceVO>> add(@Validated(Add.class) @RequestBody DeviceVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    Long tenantId = tuple.getT1().getT1();
                    DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tenantId);
                    entityBO.setCreatorId(tuple.getT1().getT2());
                    entityBO.setCreatorName(tuple.getT2());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return reactiveDeviceService
                            .add(entityBO)
                            .map(deviceBuilder::buildVOByBO)
                            .map(created ->
                                    ResponseEntity.status(HttpStatus.CREATED).body(created));
                });
    }

    /**
     * Delete a device after verifying it belongs to the current tenant, then return the delete-success status.
     *
     * @param id id of the device to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('device', 'delete')")
    @Operation(
            summary = "Delete Device",
            description =
                    "Permanently delete a device by ID (tenant-scoped). "
                            + "Removes the device and its point-value configuration while preserving collected history; the action cannot be undone.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(
            @Parameter(
                            description = "Primary key of the entity to delete. Must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id,
            @Parameter(
                            description = "Current optimistic-lock version required as a deletion precondition.",
                            example = "0")
                    @NotNull
                    @Min(0)
                    @RequestParam("version")
                    Integer version) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> reactiveDeviceService
                        .delete(
                                tuple.getT1().getT1(),
                                id,
                                version,
                                tuple.getT1().getT2(),
                                tuple.getT2())
                        .thenReturn(ResponseEntity.noContent().build()));
    }

    /**
     * Update an existing device after verifying tenant ownership, then return the update-success status.
     *
     * @param entityVO device payload to update
     * @return update-success status
     */
    @PreAuthorize("@perm.can('device', 'update')")
    @Operation(
            summary = "Update Device",
            description = "Modify an existing device's attributes such as name, profile, "
                    + "driver and connection settings. Tenant ownership is verified before applying the update.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<DeviceVO>> update(@Validated(Update.class) @RequestBody DeviceVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    Long tenantId = tuple.getT1().getT1();
                    DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tenantId);
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return reactiveDeviceService
                            .update(entityBO)
                            .map(deviceBuilder::buildVOByBO)
                            .map(ResponseEntity::ok);
                });
    }

    /**
     * Fetch one device by ID after verifying it belongs to the current tenant.
     *
     * @param id id of the device to fetch
     * @return the matched DeviceVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('device', 'get')")
    @Operation(
            summary = "Get Device by ID",
            description =
                    "Fetch one device with its bound profile, driver "
                            + "and connection attributes. Use to inspect a device before sending commands or reading its point values.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_id")
    public Mono<DeviceVO> getById(
            @Parameter(
                            description = "Primary key of the target record; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> reactiveDeviceService.getById(tenantId, id).map(deviceBuilder::buildVOByBO));
    }

    /**
     * Resolve a batch of device IDs to their details, filtered to the current tenant.
     *
     * @param deviceIds ids of the devices to resolve
     * @return a map of id to DeviceVO for the tenant-owned matched ids
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(
            summary = "List Devices by IDs",
            description =
                    "Resolve a batch of device IDs to their details for the current tenant. "
                            + "Returns a map of device ID to device VO; IDs the tenant does not own are filtered out, so treat missing keys as not-found.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list_by_ids")
    public Mono<Map<String, DeviceVO>> listByIds(@RequestBody List<Long> deviceIds) {
        return getTenantId()
                .flatMap(tenantId -> reactiveDeviceService
                        .listByIds(tenantId, deviceIds)
                        .collectMap(device -> String.valueOf(device.getId()), deviceBuilder::buildVOByBO));
    }

    /**
     * List every device that instantiates a given profile template, filtered to the current tenant.
     *
     * @param profileId id of the profile template to match
     * @return a list of DeviceVO matching the profile
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(
            summary = "List Devices by Profile ID",
            description = "Return every device that instantiates a given profile template (tenant-scoped). "
                    + "Use to find which devices share the same point, command and event definitions.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_by_profile_id")
    public Mono<List<DeviceVO>> listByProfileId(
            @Parameter(
                            description =
                                    "Identifier of the profile template whose instantiated devices are returned; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "profile_id")
                    Long profileId) {
        return getTenantId()
                .flatMap(tenantId -> reactiveDeviceService
                        .listByProfileId(tenantId, profileId)
                        .map(deviceBuilder::buildVOByBO)
                        .collectList());
    }

    /**
     * Page through devices for the current tenant using the supplied query filters.
     *
     * @param request     optional query filters (name, profile, driver, enable flag); a new query is used when null
     * @return a page of DeviceVO matching the query
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(
            summary = "List Devices",
            description =
                    "Page through devices for the current tenant with filters such as name, "
                            + "profile, driver and enable flag. Returns a page of devices; use for browsing or selecting a target device.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<io.github.pnoker.db.r2dbc.core.page.OffsetPage<DeviceVO>> list(
            @RequestBody(required = false) DeviceListRequest request) {
        DeviceListRequest query = request == null ? new DeviceListRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> reactiveDeviceService
                        .list(new DeviceFilter(
                                tenantId,
                                query.deviceName(),
                                query.deviceCode(),
                                query.driverId(),
                                query.profileId(),
                                query.enableFlag(),
                                query.version(),
                                query.groupId(),
                                query.labelId(),
                                query.offset(),
                                query.limit(),
                                query.sort()))
                        .map(page -> new io.github.pnoker.db.r2dbc.core.page.OffsetPage<>(
                                page.items().stream()
                                        .map(deviceBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total(),
                                page.hasNext())));
    }

    /**
     * Bulk-create devices for the current tenant by importing an XLSX file (max 20 MB).
     *
     * @param request import context containing only the driver and profile identifiers
     * @param filePart uploaded XLSX file whose rows become devices
     * @return add-success status once the import completes
     */
    @PreAuthorize("@perm.can('device', 'add')")
    @Operation(
            summary = "Import Devices",
            description =
                    "Submit an atomic, durable XLSX import for the current tenant. "
                            + "The response is HTTP 202; poll statusUri until SUCCEEDED, FAILED, CANCELLED, or EXPIRED. "
                            + "Reusing Idempotency-Key with the same request returns the original operation; reuse with different content is rejected.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @ApiResponse(
            responseCode = "202",
            description = "Import operation accepted",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema =
                                    @Schema(
                                            implementation =
                                                    io.github.pnoker.db.r2dbc.core.operation.OperationAccepted.class)),
            headers =
                    @Header(
                            name = HttpHeaders.LOCATION,
                            description = "Operation status URI when supplied by the HTTP adapter"))
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<io.github.pnoker.db.r2dbc.core.operation.OperationAccepted>> importDevice(
            @Valid @RequestPart("request") DeviceImportRequest request,
            @RequestPart("file") Mono<FilePart> filePart,
            @RequestHeader(RequestConstant.Header.IDEMPOTENCY_KEY) String idempotencyKey) {
        return getPrincipalHeader().flatMap(principal -> {
            DeviceBO entityBO = new DeviceBO();
            entityBO.setDriverId(request.driverId());
            entityBO.setProfileId(request.profileId());
            entityBO.setTenantId(principal.getTenantId());
            entityBO.setOperatorId(principal.getUserId());
            entityBO.setOperatorName(principal.getUserName());
            return filePart.flatMap(part -> {
                assertXlsxFile(part);
                assertImportContentLength(part);
                return DataBufferUtils.join(part.content(), Math.toIntExact(MAX_IMPORT_BYTES))
                        .map(buffer -> {
                            byte[] content = new byte[buffer.readableByteCount()];
                            try {
                                buffer.read(content);
                                return content;
                            } finally {
                                DataBufferUtils.release(buffer);
                            }
                        })
                        .flatMap(content ->
                                deviceImportService.submit(entityBO, part.filename(), content, idempotencyKey))
                        .map(accepted -> ResponseEntity.accepted()
                                .location(URI.create(accepted.statusUri()))
                                .body(accepted));
            });
        });
    }

    /**
     * Generate and stream the XLSX import template shaped for the supplied profile and driver.
     *
     * @param request template context containing only the driver and profile identifiers
     * @return a ResponseEntity streaming the generated template XLSX file
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(
            summary = "Download Device Import Template",
            description =
                    "Generate and download the XLSX template used for bulk device import, "
                            + "pre-shaped for the supplied profile and driver. Fill it in and upload it to the import endpoint.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @ApiResponse(
            responseCode = "200",
            description = "Versioned XLSX device import template",
            content =
                    @Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary")))
    @PostMapping(
            value = "/export/import_template",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = XLSX_MEDIA_TYPE)
    public Mono<ResponseEntity<ByteArrayResource>> importTemplate(
            @Valid @RequestBody DeviceImportTemplateRequest request) {
        return getTenantId()
                .flatMap(tenantId -> deviceImportService
                        .generateTemplate(tenantId, request.driverId(), request.profileId())
                        .map(content -> ResponseEntity.ok()
                                .header(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=device-import-template-v1.xlsx")
                                .contentType(MediaType.parseMediaType(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .contentLength(content.length)
                                .body(new ByteArrayResource(content))));
    }

    /**
     * Count how many tenant-owned devices are driven by the given driver.
     *
     * @param driverId id of the driver to count devices for
     * @return the number of devices driven by the driver
     */
    @PreAuthorize("@perm.can('device', 'list')")
    @Operation(
            summary = "Count Devices by Driver",
            description =
                    "Return how many devices for the current tenant are driven by a given driver. "
                            + "Use for quick cardinality checks before reconfiguring a driver; the driver must belong to the tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_count_by_driver_id")
    public Mono<Integer> getCountByDriverId(
            @Parameter(
                            description =
                                    "Identifier of the driver whose device count is returned; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "driver_id")
                    Long driverId) {
        return getTenantId()
                .flatMap(tenantId -> reactiveDeviceService
                        .listByDriverId(tenantId, driverId)
                        .count()
                        .map(Math::toIntExact));
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
}
