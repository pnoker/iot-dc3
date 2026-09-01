package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.manager.entity.operation.OperationView;
import io.github.pnoker.common.manager.service.ReactiveDeviceImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "operations", description = "Durable asynchronous manager operations")
@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController implements BaseController {

    private final ReactiveDeviceImportService deviceImportService;

    @PreAuthorize("@perm.can('device', 'add')")
    @Operation(summary = "Get Operation by ID", description = "Return tenant-scoped durable operation status, progress, result, or RFC 9457-style failure details.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @ApiResponse(responseCode = "200", description = "Tenant-scoped operation status")
    @GetMapping(value = "/get_by_id", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<OperationView> getById(
            @Parameter(description = "UUIDv7 operation identifier", example = "0198f1d4-3400-7000-8000-000000000001")
            @NotNull @RequestParam("id") UUID id) {
        return getTenantId().flatMap(tenantId -> deviceImportService.getOperation(tenantId, id));
    }
}
