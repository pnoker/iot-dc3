/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverBuilder;
import io.github.pnoker.common.manager.entity.query.DriverListRequest;
import io.github.pnoker.common.manager.entity.vo.DriverVO;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Tag(name = "driver", description = "Protocol driver lifecycle")
@RestController
@RequestMapping(ManagerConstant.DRIVER_URL_PREFIX)
@RequiredArgsConstructor
public class DriverController implements BaseController {

    private final DriverBuilder driverBuilder;
    private final ReactiveDriverService reactiveDriverService;

    @PreAuthorize("@perm.can('driver', 'add')")
    @Operation(summary = "Add Driver", description = "Register a driver protocol adapter for the current tenant and return the created resource.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
            @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "false"),
            @ExtensionProperty(name = "openWorld", value = "false") }))
    @PostMapping("/add")
    public Mono<ResponseEntity<DriverVO>> add(@Validated(Add.class) @RequestBody DriverVO request) {
        return principal().flatMap(principal -> {
            DriverBO driver = driverBuilder.buildBOByVO(request);
            driver.setTenantId(principal.tenantId()); driver.setCreatorId(principal.userId()); driver.setCreatorName(principal.userName());
            driver.setOperatorId(principal.userId()); driver.setOperatorName(principal.userName());
            return reactiveDriverService.add(driver).map(driverBuilder::buildVOByBO)
                    .map(value -> ResponseEntity.status(HttpStatus.CREATED).body(value));
        });
    }

    @PreAuthorize("@perm.can('driver', 'delete')")
    @Operation(summary = "Delete Driver", description = "Soft-delete a tenant-owned driver and preserve its audit history for later review.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "HIGH"), @ExtensionProperty(name = "destructive", value = "true"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false") }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "Identifier of the tenant-owned driver to delete.", example = "1024") @NotNull @RequestParam("id") Long id,
                                             @Parameter(description = "Current optimistic-lock version required as a deletion precondition.", example = "0") @NotNull @Min(0) @RequestParam("version") Integer version) {
        return principal().flatMap(principal -> reactiveDriverService.delete(principal.tenantId(), id, version, principal.userId(), principal.userName())
                .thenReturn(ResponseEntity.noContent().build()));
    }

    @PreAuthorize("@perm.can('driver', 'update')")
    @Operation(summary = "Update Driver", description = "Update a tenant-owned driver using its optimistic-lock version and return the new resource.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false") }))
    @PostMapping("/update")
    public Mono<ResponseEntity<DriverVO>> update(@Validated(Update.class) @RequestBody DriverVO request) {
        return principal().flatMap(principal -> {
            DriverBO driver = driverBuilder.buildBOByVO(request);
            driver.setTenantId(principal.tenantId()); driver.setOperatorId(principal.userId()); driver.setOperatorName(principal.userName());
            return reactiveDriverService.update(driver).map(driverBuilder::buildVOByBO).map(ResponseEntity::ok);
        });
    }

    @PreAuthorize("@perm.can('driver', 'get')")
    @Operation(summary = "Get Driver by ID", description = "Return one tenant-owned driver resource identified by its primary key.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false") }))
    @GetMapping("/get_by_id")
    public Mono<DriverVO> getById(@Parameter(description = "Identifier of the tenant-owned driver to retrieve.", example = "1024") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenantId -> reactiveDriverService.getById(tenantId, id).map(driverBuilder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('driver', 'list')")
    @Operation(summary = "List Drivers by IDs", description = "Resolve a set of driver identifiers to tenant-owned driver resources.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false") }))
    @PostMapping("/list_by_ids")
    public Mono<Map<String, DriverVO>> listByIds(@RequestBody List<Long> ids) {
        return getTenantId().flatMap(tenantId -> reactiveDriverService.listByIds(tenantId, ids)
                .collectMap(driver -> String.valueOf(driver.getId()), driverBuilder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('driver', 'get')")
    @Operation(summary = "Get Driver by Service Name", description = "Resolve one tenant-owned driver by its registered service name.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false") }))
    @GetMapping("/get_by_service_name")
    public Mono<DriverVO> getByServiceName(@Parameter(description = "Registered service name used to locate the driver.", example = "dc3-driver-modbus-tcp") @NotNull @RequestParam("service_name") String serviceName) {
        return getTenantId().flatMap(tenantId -> reactiveDriverService.getByServiceName(tenantId, serviceName).map(driverBuilder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('driver', 'list')")
    @Operation(summary = "List Drivers", description = "List tenant-owned drivers with offset pagination and validated filters and sort fields.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false") }))
    @PostMapping("/list")
    public Mono<io.github.pnoker.db.r2dbc.core.page.OffsetPage<DriverVO>> list(@RequestBody(required = false) DriverListRequest request) {
        DriverListRequest query = request == null ? new DriverListRequest() : request;
        return getTenantId().flatMap(tenantId -> reactiveDriverService.list(new DriverFilter(tenantId, query.driverName(), query.driverCode(),
                        query.serviceName(), query.serviceHost(), query.driverTypeFlag(), query.enableFlag(), query.version(), query.groupId(),
                        query.labelId(), query.offset(), query.limit(), query.sort()))
                .map(page -> new io.github.pnoker.db.r2dbc.core.page.OffsetPage<>(page.items().stream().map(driverBuilder::buildVOByBO).toList(),
                        page.offset(), page.limit(), page.total(), page.hasNext())));
    }

    private Mono<Principal> principal() {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .map(tuple -> new Principal(tuple.getT1().getT1(), tuple.getT1().getT2(), tuple.getT2()));
    }

    private record Principal(Long tenantId, Long userId, String userName) { }
}
