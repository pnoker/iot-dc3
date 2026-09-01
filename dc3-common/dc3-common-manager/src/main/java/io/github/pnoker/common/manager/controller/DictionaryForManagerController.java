package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.option.DictionaryOption;
import io.github.pnoker.common.manager.biz.DictionaryForManagerService;
import io.github.pnoker.common.manager.entity.query.DictionaryListRequest;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** REST controller exposing read-only manager option endpoints. */
@Tag(name = "dictionary_manager", description = "Read-only manager options for selection controls")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DICTIONARY_URL_PREFIX)
@RequiredArgsConstructor
public class DictionaryForManagerController implements BaseController {

    private final DictionaryForManagerService dictionaryService;

    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Driver Options", description = "Page through tenant-scoped driver options.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_driver")
    public Mono<OffsetPage<DictionaryOption>> listDriverOptions(
            @RequestBody(required = false) DictionaryListRequest request) {
        DictionaryListRequest query = request == null ? new DictionaryListRequest() : request;
        return getTenantId().flatMap(tenantId -> dictionaryService.listDriverOptions(tenantId, query));
    }

    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Profile Options", description = "Page through tenant-scoped profile options.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_profile")
    public Mono<OffsetPage<DictionaryOption>> listProfileOptions(
            @RequestBody(required = false) DictionaryListRequest request) {
        DictionaryListRequest query = request == null ? new DictionaryListRequest() : request;
        return getTenantId().flatMap(tenantId -> dictionaryService.listProfileOptions(tenantId, query));
    }

    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Profile Point Options", description = "Page through point options under a tenant-owned profile.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_profile_point")
    public Mono<OffsetPage<DictionaryOption>> listProfilePointOptions(
            @RequestBody(required = false) DictionaryListRequest request) {
        DictionaryListRequest query = request == null ? new DictionaryListRequest() : request;
        return getTenantId().flatMap(tenantId -> dictionaryService.listProfilePointOptions(tenantId, query));
    }

    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Device Point Options", description = "Page through point options under a tenant-owned device.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_device_point")
    public Mono<OffsetPage<DictionaryOption>> listDevicePointOptions(
            @RequestBody(required = false) DictionaryListRequest request) {
        DictionaryListRequest query = request == null ? new DictionaryListRequest() : request;
        return getTenantId().flatMap(tenantId -> dictionaryService.listDevicePointOptions(tenantId, query));
    }

    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Device Options", description = "Page through tenant-scoped device options.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_device")
    public Mono<OffsetPage<DictionaryOption>> listDeviceOptions(
            @RequestBody(required = false) DictionaryListRequest request) {
        DictionaryListRequest query = request == null ? new DictionaryListRequest() : request;
        return getTenantId().flatMap(tenantId -> dictionaryService.listDeviceOptions(tenantId, query));
    }

    @PreAuthorize("@perm.can('dictionary_for_manager', 'list')")
    @Operation(summary = "List Driver Device Options", description = "Page through device options managed by a tenant-owned driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_driver_device")
    public Mono<OffsetPage<DictionaryOption>> listDriverDeviceOptions(
            @RequestBody(required = false) DictionaryListRequest request) {
        DictionaryListRequest query = request == null ? new DictionaryListRequest() : request;
        return getTenantId().flatMap(tenantId -> dictionaryService.listDriverDeviceOptions(tenantId, query));
    }

}
